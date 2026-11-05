package com.vku.web.server;

import com.vku.web.common.HttpRequest;
import com.vku.web.common.MimeTypes;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpConnectionHandler implements Runnable {
    private final Socket socket;
    private final HttpWebServer server;
    private final File webRoot;

    public HttpConnectionHandler(Socket socket, HttpWebServer server, File webRoot) {
        this.socket = socket;
        this.server = server;
        this.webRoot = webRoot;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            HttpRequest req = parseRequest(in);
            if (req == null) return;

            server.log("[" + socket.getInetAddress().getHostAddress() + "] " + req.getMethod() + " " + req.getPath());
            server.incrementRequestCount();

            handleRequest(req, out);
        } catch (Exception e) {
            // Socket closed
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }

    private HttpRequest parseRequest(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.trim().isEmpty()) return null;

        String[] parts = requestLine.split(" ");
        if (parts.length < 3) return null;

        HttpRequest req = new HttpRequest();
        req.setMethod(parts[0].toUpperCase());
        req.setPath(parts[1]);
        req.setVersion(parts[2]);

        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colon = headerLine.indexOf(':');
            if (colon > 0) {
                String key = headerLine.substring(0, colon).trim();
                String val = headerLine.substring(colon + 1).trim();
                req.addHeader(key, val);
            }
        }

        // Read POST body if Content-Length present
        String contentLengthStr = req.getHeader("content-length");
        if (contentLengthStr != null) {
            try {
                int len = Integer.parseInt(contentLengthStr);
                char[] bodyBuf = new char[len];
                int readTotal = 0;
                while (readTotal < len) {
                    int r = reader.read(bodyBuf, readTotal, len - readTotal);
                    if (r == -1) break;
                    readTotal += r;
                }
                req.setBody(new String(bodyBuf, 0, readTotal));
            } catch (Exception ignored) {}
        }

        return req;
    }

    private void handleRequest(HttpRequest req, OutputStream out) throws IOException {
        String method = req.getMethod();
        String path = req.getPath();

        // Strip query string if any
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }

        // REST API routes
        if (path.startsWith("/api/")) {
            handleApi(req, out);
            return;
        }

        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(webRoot, path.substring(1)).getCanonicalFile();
        if (!file.getAbsolutePath().startsWith(webRoot.getAbsolutePath()) || !file.exists() || file.isDirectory()) {
            send404(req, out);
            return;
        }

        String mime = MimeTypes.getContentType(file.getName());
        long fileLength = file.length();

        // Check for Byte-Range request (for streaming video/audio seeking)
        String rangeHeader = req.getHeader("range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=") && "GET".equals(method)) {
            handleByteRange(req, file, mime, fileLength, rangeHeader, out);
            return;
        }

        if ("HEAD".equals(method)) {
            sendHeaders(out, 200, "OK", mime, fileLength, true);
        } else if ("GET".equals(method)) {
            sendHeaders(out, 200, "OK", mime, fileLength, true);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[16384];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
                out.flush();
            }
        } else if ("POST".equals(method)) {
            // POST to file or static asset is usually echoed
            String respText = "Đã nhận yêu cầu POST tới tệp " + path + ". Dữ liệu gửi: " + req.getBody();
            byte[] bytes = respText.getBytes(StandardCharsets.UTF_8);
            sendHeaders(out, 200, "OK", "text/plain; charset=UTF-8", bytes.length, false);
            out.write(bytes);
            out.flush();
        } else {
            send405(out);
        }
    }

    private void handleByteRange(HttpRequest req, File file, String mime, long fileLength, String rangeHeader, OutputStream out) throws IOException {
        String rangeVal = rangeHeader.substring(6).trim(); // Remove "bytes="
        long start = 0;
        long end = fileLength - 1;

        String[] parts = rangeVal.split("-");
        try {
            if (!parts[0].isEmpty()) {
                start = Long.parseLong(parts[0]);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            start = 0;
            end = fileLength - 1;
        }

        if (start > end || start >= fileLength) {
            String resp = "HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */" + fileLength + "\r\n\r\n";
            out.write(resp.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        long contentLength = end - start + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 206 Partial Content\r\n");
        sb.append("Content-Type: ").append(mime).append("\r\n");
        sb.append("Content-Range: bytes ").append(start).append("-").append(end).append("/").append(fileLength).append("\r\n");
        sb.append("Content-Length: ").append(contentLength).append("\r\n");
        sb.append("Accept-Ranges: bytes\r\n");
        sb.append("Server: VKU-HttpServer/1.1\r\n");
        sb.append("Date: ").append(getServerDate()).append("\r\n\r\n");

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            byte[] buf = new byte[16384];
            long bytesRemaining = contentLength;
            while (bytesRemaining > 0) {
                int toRead = (int) Math.min(buf.length, bytesRemaining);
                int read = raf.read(buf, 0, toRead);
                if (read == -1) break;
                out.write(buf, 0, read);
                bytesRemaining -= read;
            }
            out.flush();
        }
    }

    private void handleApi(HttpRequest req, OutputStream out) throws IOException {
        String path = req.getPath();
        if ("/api/stats".equalsIgnoreCase(path)) {
            String json = String.format("{\"server\":\"VKU-HttpServer\",\"total_requests\":%d,\"port\":%d,\"time\":\"%s\"}",
                    server.getTotalRequests(), server.getPort(), getServerDate());
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            sendHeaders(out, 200, "OK", "application/json; charset=UTF-8", bytes.length, false);
            if (!"HEAD".equals(req.getMethod())) {
                out.write(bytes);
            }
            out.flush();
        } else if ("/api/echo".equalsIgnoreCase(path) || "/api/post".equalsIgnoreCase(path)) {
            String json = String.format("{\"status\":\"success\",\"received_method\":\"%s\",\"body\":\"%s\",\"time\":\"%s\"}",
                    req.getMethod(), req.getBody().replace("\"", "\\\"").replace("\n", "\\n"), getServerDate());
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            sendHeaders(out, 200, "OK", "application/json; charset=UTF-8", bytes.length, false);
            if (!"HEAD".equals(req.getMethod())) {
                out.write(bytes);
            }
            out.flush();
        } else {
            send404(req, out);
        }
    }

    private void sendHeaders(OutputStream out, int status, String msg, String mime, long length, boolean acceptRanges) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(status).append(" ").append(msg).append("\r\n");
        sb.append("Content-Type: ").append(mime).append("\r\n");
        sb.append("Content-Length: ").append(length).append("\r\n");
        if (acceptRanges) sb.append("Accept-Ranges: bytes\r\n");
        sb.append("Server: VKU-HttpServer/1.1\r\n");
        sb.append("Date: ").append(getServerDate()).append("\r\n");
        sb.append("Connection: close\r\n\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void send404(HttpRequest req, OutputStream out) throws IOException {
        String body = "<html><head><title>404 Not Found</title></head><body style='font-family:sans-serif;text-align:center;padding:50px;'>"
                + "<h1>404 - Không Tìm Thấy Tài Nguyên</h1>"
                + "<p>Tệp hoặc dịch vụ bạn yêu cầu không tồn tại trên VKU Web Server: <code>" + req.getPath() + "</code></p>"
                + "<a href='/'>Quay về Trang Chủ</a>"
                + "</body></html>";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        sendHeaders(out, 404, "Not Found", "text/html; charset=UTF-8", bytes.length, false);
        if (!"HEAD".equals(req.getMethod())) {
            out.write(bytes);
        }
        out.flush();
    }

    private void send405(OutputStream out) throws IOException {
        String body = "HTTP/1.1 405 Method Not Allowed\r\nContent-Type: text/plain\r\n\r\nPhương thức không được hỗ trợ.";
        out.write(body.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String getServerDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }
}
