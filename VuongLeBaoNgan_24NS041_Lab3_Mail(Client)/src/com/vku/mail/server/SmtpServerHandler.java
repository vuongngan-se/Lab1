package com.vku.mail.server;

import com.vku.mail.db.MailDatabase;
import com.vku.mail.model.MailLog;
import com.vku.mail.model.MailMessage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SmtpServerHandler implements Runnable {
    private final Socket socket;
    private final MailServer server;
    private final MailDatabase db;

    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientIp;
    private String mailFrom = "";
    private String rcptTo = "";
    private boolean active = true;

    public SmtpServerHandler(Socket socket, MailServer server, MailDatabase db) {
        this.socket = socket;
        this.server = server;
        this.db = db;
        this.clientIp = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            sendReply("220 vku.udn.vn ESMTP VKU Mail Server ready");

            String line;
            while (active && (line = reader.readLine()) != null) {
                line = line.trim();
                server.log("[SMTP " + clientIp + "] RECV: " + line);
                handleCommand(line);
            }
        } catch (Exception e) {
            // Client closed
        } finally {
            close();
        }
    }

    private void handleCommand(String line) throws IOException {
        String upper = line.toUpperCase();
        if (upper.startsWith("EHLO")) {
            sendReply("250-vku.udn.vn Hello [" + clientIp + "]\r\n250-SIZE 20971520\r\n250 8BITMIME");
        } else if (upper.startsWith("HELO")) {
            sendReply("250 vku.udn.vn Hello [" + clientIp + "]");
        } else if (upper.startsWith("MAIL FROM:")) {
            mailFrom = extractAddress(line.substring(10).trim());
            sendReply("250 2.1.0 Sender <" + mailFrom + "> OK");
        } else if (upper.startsWith("RCPT TO:")) {
            rcptTo = extractAddress(line.substring(8).trim());
            sendReply("250 2.1.5 Recipient <" + rcptTo + "> OK");
        } else if (upper.equals("DATA")) {
            handleData();
        } else if (upper.equals("RSET")) {
            mailFrom = "";
            rcptTo = "";
            sendReply("250 2.0.0 OK Reset state");
        } else if (upper.equals("NOOP")) {
            sendReply("250 2.0.0 OK");
        } else if (upper.equals("QUIT")) {
            sendReply("221 2.0.0 vku.udn.vn closing transmission channel");
            active = false;
        } else {
            sendReply("500 5.5.1 Command unrecognized");
        }
    }

    private void handleData() throws IOException {
        sendReply("354 Start mail input; end with <CRLF>.<CRLF>");
        StringBuilder headers = new StringBuilder();
        StringBuilder body = new StringBuilder();
        boolean readingHeaders = true;
        String subject = "(Không có tiêu đề)";

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break; // End of DATA

            if (readingHeaders) {
                if (line.isEmpty()) {
                    readingHeaders = false;
                } else {
                    headers.append(line).append("\n");
                    if (line.toLowerCase().startsWith("subject:")) {
                        subject = line.substring(8).trim();
                    } else if (line.toLowerCase().startsWith("from:") && mailFrom.isEmpty()) {
                        mailFrom = extractAddress(line.substring(5).trim());
                    } else if (line.toLowerCase().startsWith("to:") && rcptTo.isEmpty()) {
                        rcptTo = extractAddress(line.substring(3).trim());
                    }
                }
            } else {
                body.append(line).append("\n");
            }
        }

        MailMessage msg = new MailMessage(
                mailFrom.isEmpty() ? "unknown@localhost" : mailFrom,
                rcptTo.isEmpty() ? "unknown@localhost" : rcptTo,
                subject,
                body.toString().trim()
        );

        db.saveMail(msg);
        db.log(new MailLog("SMTP", clientIp, "Nhận thư từ " + mailFrom + " gửi tới " + rcptTo + " [Tiêu đề: " + subject + "]", "SUCCESS"));
        server.notifyNewMail();

        sendReply("250 2.0.0 OK Message accepted for delivery id=" + msg.getId());
    }

    private String extractAddress(String raw) {
        int start = raw.indexOf("<");
        int end = raw.indexOf(">");
        if (start != -1 && end > start) {
            return raw.substring(start + 1, end).trim();
        }
        return raw.trim();
    }

    private void sendReply(String reply) throws IOException {
        server.log("[SMTP " + clientIp + "] SEND: " + reply.replace("\r\n", " "));
        writer.write(reply + "\r\n");
        writer.flush();
    }

    public void close() {
        active = false;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}
