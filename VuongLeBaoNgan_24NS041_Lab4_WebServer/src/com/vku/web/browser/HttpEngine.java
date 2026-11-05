package com.vku.web.browser;

import com.vku.web.common.HttpResponse;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

public class HttpEngine {

    public static HttpResponse execute(String method, String urlStr, String body) throws Exception {
        URI uri = new URI(urlStr);
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 80;
        String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/" : uri.getRawPath();
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }

        try (Socket socket = new Socket(host, port);
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Build request
            StringBuilder req = new StringBuilder();
            req.append(method.toUpperCase()).append(" ").append(path).append(" HTTP/1.1\r\n");
            req.append("Host: ").append(host).append("\r\n");
            req.append("User-Agent: VKU-MiniBrowser/1.0 (Chrome-like)\r\n");
            req.append("Accept: */*\r\n");
            req.append("Connection: close\r\n");

            byte[] bodyBytes = new byte[0];
            if ("POST".equalsIgnoreCase(method) && body != null) {
                bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                req.append("Content-Type: application/x-www-form-urlencoded\r\n");
                req.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            }
            req.append("\r\n");

            out.write(req.toString().getBytes(StandardCharsets.UTF_8));
            if (bodyBytes.length > 0) {
                out.write(bodyBytes);
            }
            out.flush();

            // Read response status and headers
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            if (statusLine == null) throw new IOException("Phản hồi rỗng từ máy chủ");

            String[] statusParts = statusLine.split(" ", 3);
            int code = statusParts.length > 1 ? Integer.parseInt(statusParts[1]) : 200;
            String msg = statusParts.length > 2 ? statusParts[2] : "OK";

            HttpResponse resp = new HttpResponse(code, msg);

            String headerLine;
            long contentLength = -1;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                int colon = headerLine.indexOf(':');
                if (colon > 0) {
                    String k = headerLine.substring(0, colon).trim();
                    String v = headerLine.substring(colon + 1).trim();
                    resp.setHeader(k, v);
                    if ("content-length".equalsIgnoreCase(k)) {
                        try { contentLength = Long.parseLong(v); } catch (Exception ignored) {}
                    }
                }
            }

            if (!"HEAD".equalsIgnoreCase(method)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (contentLength > 0) {
                    char[] buf = new char[8192];
                    long remaining = contentLength;
                    while (remaining > 0) {
                        int r = reader.read(buf, 0, (int) Math.min(buf.length, remaining));
                        if (r == -1) break;
                        baos.write(new String(buf, 0, r).getBytes(StandardCharsets.UTF_8));
                        remaining -= r;
                    }
                } else {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        baos.write(line.getBytes(StandardCharsets.UTF_8));
                        baos.write('\n');
                    }
                }
                resp.setBodyBytes(baos.toByteArray());
            }

            return resp;
        }
    }

    public static void download(String urlStr, File targetFile, BiConsumer<Long, Long> onProgress) throws Exception {
        URI uri = new URI(urlStr);
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 80;
        String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/" : uri.getRawPath();

        try (Socket socket = new Socket(host, port);
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            String req = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(req.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Read headers byte by byte until \r\n\r\n
            ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
            int b;
            int matchCount = 0;
            while ((b = in.read()) != -1) {
                headerBaos.write(b);
                if ((matchCount == 0 || matchCount == 2) && b == '\r') matchCount++;
                else if ((matchCount == 1 || matchCount == 3) && b == '\n') matchCount++;
                else matchCount = 0;
                if (matchCount == 4) break; // Found \r\n\r\n
            }

            String headerText = headerBaos.toString(StandardCharsets.UTF_8);
            long contentLength = -1;
            for (String line : headerText.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Long.parseLong(line.substring(15).trim());
                    } catch (Exception ignored) {}
                }
            }

            // Stream body directly to file
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buf = new byte[16384];
                int read;
                long totalDownloaded = 0;
                while ((read = in.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                    totalDownloaded += read;
                    if (onProgress != null) {
                        onProgress.accept(totalDownloaded, contentLength);
                    }
                }
                fos.flush();
            }
        }
    }
}
