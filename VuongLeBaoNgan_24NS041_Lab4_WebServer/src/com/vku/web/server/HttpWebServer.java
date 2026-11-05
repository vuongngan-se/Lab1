package com.vku.web.server;

import com.vku.web.db.WebDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class HttpWebServer {
    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private final File webRoot;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final WebDatabase database = new WebDatabase();
    private ServerSocket serverSocket;
    private boolean running = false;
    private WebServerGUI gui;

    public HttpWebServer() {
        this(DEFAULT_PORT, new File("webroot"));
    }

    public HttpWebServer(int port, File webRoot) {
        this.port = port;
        this.webRoot = webRoot;
        initWebRoot();
    }

    private void initWebRoot() {
        try {
            if (!webRoot.exists()) webRoot.mkdirs();

            // 1. Generate index.html
            File indexFile = new File(webRoot, "index.html");
            if (!indexFile.exists()) {
                String html = "<!DOCTYPE html>\n"
                        + "<html lang='vi'>\n"
                        + "<head>\n"
                        + "    <meta charset='UTF-8'>\n"
                        + "    <title>VKU Web Server - Đa Dịch Vụ LTM</title>\n"
                        + "    <link rel='stylesheet' href='style.css'>\n"
                        + "</head>\n"
                        + "<body>\n"
                        + "    <div class='container'>\n"
                        + "        <header>\n"
                        + "            <h1>🚀 VKU HTTP Web Server (Lab 4 - LTM)</h1>\n"
                        + "            <p class='subtitle'>Máy chủ Web đa phương tiện hỗ trợ GET, HEAD, POST, Audio Streaming, Video Streaming, PDF</p>\n"
                        + "        </header>\n"
                        + "        <main class='grid'>\n"
                        + "            <div class='card'>\n"
                        + "                <div class='icon'>📄</div>\n"
                        + "                <h3>Dịch Vụ 1: Tài Liệu PDF (.pdf)</h3>\n"
                        + "                <p>Phục vụ tài liệu học phần Lập Trình Mạng với MIME type <code>application/pdf</code>.</p>\n"
                        + "                <a href='sample.pdf' class='btn' target='_blank'>Xem / Tải File PDF</a>\n"
                        + "            </div>\n"
                        + "            <div class='card'>\n"
                        + "                <div class='icon'>🎵</div>\n"
                        + "                <h3>Dịch Vụ 2: Âm Thanh MP3 (.mp3)</h3>\n"
                        + "                <p>Truyền phát âm thanh với MIME <code>audio/mpeg</code>.</p>\n"
                        + "                <audio controls src='sample.mp3' style='width:100%;margin-top:10px;'></audio><br/><br/>\n"
                        + "                <a href='sample.mp3' class='btn secondary' download>Tải Tệp MP3</a>\n"
                        + "            </div>\n"
                        + "            <div class='card'>\n"
                        + "                <div class='icon'>🎬</div>\n"
                        + "                <h3>Dịch Vụ 3: Video Streaming MP4 (.mp4)</h3>\n"
                        + "                <p>Hỗ trợ Byte-Range request (HTTP 206 Partial Content) để tua video mượt mà.</p>\n"
                        + "                <video controls src='sample.mp4' style='width:100%;max-height:180px;background:#000;margin-top:8px;'></video><br/><br/>\n"
                        + "                <a href='sample.mp4' class='btn secondary' download>Tải Tệp MP4</a>\n"
                        + "            </div>\n"
                        + "            <div class='card'>\n"
                        + "                <div class='icon'>⚡</div>\n"
                        + "                <h3>Dịch Vụ 4: REST API (GET / POST)</h3>\n"
                        + "                <p>API kiểm tra trạng thái máy chủ và gửi dữ liệu form.</p>\n"
                        + "                <a href='/api/stats' class='btn' target='_blank'>Gọi API /api/stats (JSON)</a>\n"
                        + "            </div>\n"
                        + "        </main>\n"
                        + "        <footer>\n"
                        + "            <p>Trường Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU) | Khoa Khoa Học Máy Tính</p>\n"
                        + "        </footer>\n"
                        + "    </div>\n"
                        + "</body>\n"
                        + "</html>";
                Files.writeString(indexFile.toPath(), html, StandardCharsets.UTF_8);
            }

            // 2. Generate style.css
            File cssFile = new File(webRoot, "style.css");
            if (!cssFile.exists()) {
                String css = "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }\n"
                        + ".container { max-width: 1000px; margin: 0 auto; }\n"
                        + "header { text-align: center; margin-bottom: 30px; padding: 20px; background: #1e293b; border-radius: 12px; border: 1px solid #334155; }\n"
                        + "h1 { color: #38bdf8; margin: 0 0 10px 0; }\n"
                        + ".subtitle { color: #94a3b8; font-size: 15px; margin: 0; }\n"
                        + ".grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }\n"
                        + ".card { background: #1e293b; padding: 24px; border-radius: 12px; border: 1px solid #334155; text-align: center; }\n"
                        + ".icon { font-size: 36px; margin-bottom: 12px; }\n"
                        + "h3 { color: #f1f5f9; margin-top: 0; }\n"
                        + "p { color: #cbd5e1; font-size: 14px; line-height: 1.5; }\n"
                        + ".btn { display: inline-block; background: #0284c7; color: white; padding: 10px 18px; border-radius: 6px; text-decoration: none; font-weight: bold; font-size: 13px; }\n"
                        + ".btn.secondary { background: #475569; }\n"
                        + ".btn:hover { opacity: 0.9; }\n"
                        + "footer { text-align: center; margin-top: 40px; color: #64748b; font-size: 13px; }";
                Files.writeString(cssFile.toPath(), css, StandardCharsets.UTF_8);
            }

            // 3. Generate sample.pdf (Standard PDF 1.4 stream)
            File pdfFile = new File(webRoot, "sample.pdf");
            if (!pdfFile.exists()) {
                String pdf = "%PDF-1.4\n"
                        + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                        + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                        + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                        + "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                        + "5 0 obj << /Length 135 >> stream\n"
                        + "BT\n"
                        + "/F1 20 Tf\n"
                        + "50 720 Td\n"
                        + "(TRUONG DH CNTT & TT VIET - HAN (VKU)) Tj\n"
                        + "/F1 14 Tf\n"
                        + "0 -40 Td\n"
                        + "(HOC PHAN: LAP TRINH MANG - LAB 4 WEB SERVER) Tj\n"
                        + "ET\n"
                        + "endstream endobj\n"
                        + "xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000244 00000 n \n0000000318 00000 n \n"
                        + "trailer << /Size 6 /Root 1 0 R >>\nstartxref\n505\n%%EOF\n";
                Files.writeString(pdfFile.toPath(), pdf, StandardCharsets.US_ASCII);
            }

            // 4. Generate sample.mp3
            File mp3File = new File(webRoot, "sample.mp3");
            if (!mp3File.exists()) {
                byte[] mp3Bytes = new byte[32768];
                // MP3 sync frame headers (0xFF, 0xFB) repeating
                for (int i = 0; i < mp3Bytes.length; i += 4) {
                    mp3Bytes[i] = (byte) 0xFF;
                    mp3Bytes[i + 1] = (byte) 0xFB;
                    mp3Bytes[i + 2] = (byte) 0x90;
                    mp3Bytes[i + 3] = (byte) 0x00;
                }
                Files.write(mp3File.toPath(), mp3Bytes);
            }

            // 5. Generate sample.mp4
            File mp4File = new File(webRoot, "sample.mp4");
            if (!mp4File.exists()) {
                byte[] mp4Bytes = new byte[65536];
                // Standard ftyp box: size 24, "ftyp", "isom", 512, "isom", "iso2", "mp41"
                byte[] ftyp = new byte[]{
                        0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
                        'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00,
                        'i', 's', 'o', 'm', 'i', 's', 'o', '2'
                };
                System.arraycopy(ftyp, 0, mp4Bytes, 0, ftyp.length);
                Files.write(mp4File.toPath(), mp4Bytes);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo webroot: " + e.getMessage());
        }
    }

    public void setGui(WebServerGUI gui) {
        this.gui = gui;
    }

    public void start() {
        if (running) return;
        running = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("VKU HTTP Web Server đã khởi động trên cổng: " + port);
                log("Thư mục gốc: " + webRoot.getAbsolutePath());
                log("Dịch vụ phục vụ: HTML, CSS, PDF (.pdf), Audio (.mp3), Video (.mp4), JSON API");

                while (running && !serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    threadPool.submit(new HttpConnectionHandler(client, this, webRoot));
                }
            } catch (IOException e) {
                if (running) log("Lỗi Web ServerSocket: " + e.getMessage());
            }
        }, "HTTP-Server-Acceptor").start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }

    public void log(String msg) {
        System.out.println(msg);
        if (gui != null) {
            gui.appendLog(msg);
        }
    }

    public void incrementRequestCount() {
        totalRequests.incrementAndGet();
        if (gui != null) {
            gui.updateRequestCount(totalRequests.get());
        }
    }

    public long getTotalRequests() { return totalRequests.get(); }
    public WebDatabase getDatabase() { return database; }
    public int getPort() { return port; }
    public File getWebRoot() { return webRoot; }
    public boolean isRunning() { return running; }
}
