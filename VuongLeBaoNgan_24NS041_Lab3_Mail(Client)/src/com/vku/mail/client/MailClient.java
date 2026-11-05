package com.vku.mail.client;

import com.vku.mail.model.MailMessage;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class MailClient {
    private String smtpHost = "localhost";
    private int smtpPort = 2525;
    private String pop3Host = "localhost";
    private int pop3Port = 1100;

    private String username = "alice";
    private String password = "123456";
    private String emailAddress = "alice@vku.udn.vn";

    private boolean isGmailMode = false;
    private Consumer<String> logListener;

    public MailClient() {}

    public void configureLocal(String user, String pass, String email) {
        configureLocal("127.0.0.1", 2525, 1100, user, pass, email);
    }

    public void configureLocal(String host, int smtpPort, int pop3Port, String user, String pass, String email) {
        this.smtpHost = (host != null && !host.trim().isEmpty()) ? host.trim() : "127.0.0.1";
        this.smtpPort = smtpPort > 0 ? smtpPort : 2525;
        this.pop3Host = (host != null && !host.trim().isEmpty()) ? host.trim() : "127.0.0.1";
        this.pop3Port = pop3Port > 0 ? pop3Port : 1100;
        this.username = user;
        this.password = pass;
        this.emailAddress = email;
        this.isGmailMode = false;
    }

    public void configureGmail(String gmailAddress, String appPassword) {
        this.smtpHost = "smtp.gmail.com";
        this.smtpPort = 465; // SSL
        this.pop3Host = "pop.gmail.com";
        this.pop3Port = 995; // SSL
        this.username = gmailAddress;
        this.password = appPassword;
        this.emailAddress = gmailAddress;
        this.isGmailMode = true;
    }

    public void setLogListener(Consumer<String> listener) {
        this.logListener = listener;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (logListener != null) logListener.accept(msg);
    }

    public boolean sendMail(String to, String subject, String body, String attachName, byte[] attachData) {
        if (isGmailMode) {
            return sendMailGmail(to, subject, body, attachName, attachData);
        } else {
            return sendMailLocal(to, subject, body, attachName, attachData);
        }
    }

    private boolean sendMailLocal(String to, String subject, String body, String attachName, byte[] attachData) {
        try (Socket socket = new Socket(smtpHost, smtpPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            readSmtpResponse(reader, "SMTP");

            sendAndLog(writer, "HELO " + (smtpHost.equals("localhost") || smtpHost.equals("127.0.0.1") ? "localhost" : "client"), "SMTP");
            readSmtpResponse(reader, "SMTP");

            sendAndLog(writer, "MAIL FROM:<" + emailAddress + ">", "SMTP");
            readSmtpResponse(reader, "SMTP");

            sendAndLog(writer, "RCPT TO:<" + to + ">", "SMTP");
            readSmtpResponse(reader, "SMTP");

            sendAndLog(writer, "DATA", "SMTP");
            readSmtpResponse(reader, "SMTP");

            // Write headers
            writer.write("From: " + emailAddress + "\r\n");
            writer.write("To: " + to + "\r\n");
            writer.write("Subject: " + subject + "\r\n");
            if (attachName != null) {
                writer.write("X-Attachment-Name: " + attachName + "\r\n");
            }
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");

            // Write body
            writer.write(body != null ? body : "");
            writer.write("\r\n.\r\n");
            writer.flush();
            log("[SMTP SEND]: [Message Data End .]");

            String resp = readSmtpResponse(reader, "SMTP");

            sendAndLog(writer, "QUIT", "SMTP");
            readSmtpResponse(reader, "SMTP");

            return resp.startsWith("250");
        } catch (Exception e) {
            log("[SMTP LỖI]: " + e.getMessage());
            return false;
        }
    }

    private boolean sendMailGmail(String to, String subject, String body, String attachName, byte[] attachData) {
        try {
            log("[GMAIL SMTP]: Đang kết nối SSL tới smtp.gmail.com:465...");
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket("smtp.gmail.com", 465);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                readAndLog(reader, "GMAIL-SMTP");

                sendAndLog(writer, "EHLO localhost", "GMAIL-SMTP");
                readMultipleResponses(reader, "GMAIL-SMTP");

                // Authenticate with AUTH LOGIN
                sendAndLog(writer, "AUTH LOGIN", "GMAIL-SMTP");
                readAndLog(reader, "GMAIL-SMTP");

                String userB64 = Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8));
                sendAndLog(writer, userB64, "GMAIL-SMTP (Username B64)");
                readAndLog(reader, "GMAIL-SMTP");

                String passB64 = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
                sendAndLog(writer, passB64, "GMAIL-SMTP (Pass B64)");
                String authResp = readAndLog(reader, "GMAIL-SMTP");
                if (!authResp.startsWith("235")) {
                    log("[GMAIL SMTP LỖI]: Xác thực thất bại! Kiểm tra Email và Mật khẩu ứng dụng (App Password).");
                    return false;
                }

                sendAndLog(writer, "MAIL FROM:<" + emailAddress + ">", "GMAIL-SMTP");
                readAndLog(reader, "GMAIL-SMTP");

                sendAndLog(writer, "RCPT TO:<" + to + ">", "GMAIL-SMTP");
                readAndLog(reader, "GMAIL-SMTP");

                sendAndLog(writer, "DATA", "GMAIL-SMTP");
                readAndLog(reader, "GMAIL-SMTP");

                writer.write("From: " + emailAddress + "\r\n");
                writer.write("To: " + to + "\r\n");
                writer.write("Subject: " + subject + "\r\n");
                writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                writer.write(body != null ? body : "");
                writer.write("\r\n.\r\n");
                writer.flush();

                String complete = readAndLog(reader, "GMAIL-SMTP");
                sendAndLog(writer, "QUIT", "GMAIL-SMTP");
                readAndLog(reader, "GMAIL-SMTP");

                return complete.startsWith("250");
            }
        } catch (Exception e) {
            log("[GMAIL SMTP LỖI]: " + e.getMessage());
            return false;
        }
    }

    public List<MailMessage> fetchInbox() {
        if (isGmailMode) {
            return fetchInboxGmail();
        } else {
            return fetchInboxLocal();
        }
    }

    private List<MailMessage> fetchInboxLocal() {
        List<MailMessage> list = new ArrayList<>();
        try (Socket socket = new Socket(pop3Host, pop3Port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            readAndLog(reader, "POP3");

            sendAndLog(writer, "USER " + username, "POP3");
            readAndLog(reader, "POP3");

            sendAndLog(writer, "PASS " + password, "POP3");
            String passResp = readAndLog(reader, "POP3");
            if (!passResp.startsWith("+OK")) return list;

            sendAndLog(writer, "STAT", "POP3");
            String statResp = readAndLog(reader, "POP3");
            String[] statTokens = statResp.split(" ");
            int count = (statTokens.length >= 2) ? Integer.parseInt(statTokens[1]) : 0;

            for (int i = 1; i <= count; i++) {
                sendAndLog(writer, "RETR " + i, "POP3");
                String retrResp = readAndLog(reader, "POP3");
                if (retrResp.startsWith("+OK")) {
                    MailMessage msg = parseRfc822(reader);
                    if (msg != null) list.add(msg);
                }
            }

            sendAndLog(writer, "QUIT", "POP3");
            readAndLog(reader, "POP3");
        } catch (Exception e) {
            log("[POP3 LỖI]: " + e.getMessage());
        }
        return list;
    }

    private List<MailMessage> fetchInboxGmail() {
        List<MailMessage> list = new ArrayList<>();
        try {
            log("[GMAIL POP3]: Đang kết nối SSL tới pop.gmail.com:995...");
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket("pop.gmail.com", 995);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                readAndLog(reader, "GMAIL-POP3");

                sendAndLog(writer, "USER " + username, "GMAIL-POP3");
                readAndLog(reader, "GMAIL-POP3");

                sendAndLog(writer, "PASS " + password, "GMAIL-POP3");
                String passResp = readAndLog(reader, "GMAIL-POP3");
                if (!passResp.startsWith("+OK")) return list;

                sendAndLog(writer, "STAT", "GMAIL-POP3");
                String statResp = readAndLog(reader, "GMAIL-POP3");
                String[] tokens = statResp.split(" ");
                int count = (tokens.length >= 2) ? Math.min(10, Integer.parseInt(tokens[1])) : 0;

                for (int i = 1; i <= count; i++) {
                    sendAndLog(writer, "RETR " + i, "GMAIL-POP3");
                    String retr = readAndLog(reader, "GMAIL-POP3");
                    if (retr.startsWith("+OK")) {
                        MailMessage msg = parseRfc822(reader);
                        if (msg != null) list.add(msg);
                    }
                }

                sendAndLog(writer, "QUIT", "GMAIL-POP3");
                readAndLog(reader, "GMAIL-POP3");
            }
        } catch (Exception e) {
            log("[GMAIL POP3 LỖI]: " + e.getMessage());
        }
        return list;
    }

    private MailMessage parseRfc822(BufferedReader reader) throws IOException {
        String line;
        String from = "Unknown";
        String to = emailAddress;
        String subject = "(Không có tiêu đề)";
        String attachName = null;
        StringBuilder body = new StringBuilder();
        boolean inHeader = true;

        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;

            if (inHeader) {
                if (line.isEmpty()) {
                    inHeader = false;
                } else {
                    String lower = line.toLowerCase();
                    if (lower.startsWith("from:")) from = decodeRfc2047(line.substring(5).trim());
                    else if (lower.startsWith("to:")) to = decodeRfc2047(line.substring(3).trim());
                    else if (lower.startsWith("subject:")) subject = decodeRfc2047(line.substring(8).trim());
                    else if (lower.startsWith("x-attachment-name:")) attachName = line.substring(18).trim();
                }
            } else {
                body.append(line).append("\n");
            }
        }

        String cleanBody = cleanEmailBody(body.toString());
        MailMessage msg = new MailMessage(from, to, subject, cleanBody);
        msg.setAttachmentName(attachName);
        return msg;
    }

    private String cleanEmailBody(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) return "";

        String[] lines = rawBody.split("\r?\n");
        StringBuilder clean = new StringBuilder();
        boolean inTextPlain = false;
        boolean inPartHeader = false;
        boolean isQuotedPrintable = false;
        boolean foundAnyPlainPart = false;

        boolean hasMimeParts = false;
        for (String l : lines) {
            if (l.trim().startsWith("--") && l.trim().length() > 5) {
                hasMimeParts = true;
                break;
            }
        }

        if (hasMimeParts) {
            for (String l : lines) {
                String trimmed = l.trim();
                if (trimmed.startsWith("--")) {
                    if (trimmed.endsWith("--")) {
                        inTextPlain = false;
                        inPartHeader = false;
                        continue;
                    }
                    if (foundAnyPlainPart) {
                        break; // Already extracted text/plain section
                    }
                    inPartHeader = true;
                    inTextPlain = false;
                    isQuotedPrintable = false;
                    continue;
                }

                if (inPartHeader) {
                    if (trimmed.isEmpty()) {
                        inPartHeader = false;
                    } else {
                        String lower = trimmed.toLowerCase();
                        if (lower.contains("content-type:") && lower.contains("text/plain")) {
                            inTextPlain = true;
                            foundAnyPlainPart = true;
                        } else if (lower.contains("content-type:") && (lower.contains("text/html") || lower.contains("image/") || lower.contains("application/"))) {
                            inTextPlain = false;
                        }
                        if (lower.contains("content-transfer-encoding:") && lower.contains("quoted-printable")) {
                            isQuotedPrintable = true;
                        }
                    }
                    continue;
                }

                if (inTextPlain) {
                    clean.append(l).append("\n");
                }
            }

            if (foundAnyPlainPart && clean.length() > 0) {
                String result = clean.toString();
                if (isQuotedPrintable || result.contains("=0D") || result.contains("=\n") || result.contains("=\r\n")) {
                    result = decodeQuotedPrintable(result);
                }
                return result.trim();
            }
        }

        String result = rawBody;
        if (result.contains("=0D") || result.contains("=3D") || result.contains("quoted-printable")) {
            result = decodeQuotedPrintable(result);
        }
        return result.trim();
    }

    private static String decodeQuotedPrintable(String input) {
        if (input == null) return "";
        try {
            // Remove soft line breaks (=\r\n or =\n)
            String normalized = input.replaceAll("=\r?\n", "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = normalized.getBytes(StandardCharsets.ISO_8859_1);
            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                if (b == '=' && i + 2 < bytes.length) {
                    char h1 = (char) bytes[i + 1];
                    char h2 = (char) bytes[i + 2];
                    int d1 = Character.digit(h1, 16);
                    int d2 = Character.digit(h2, 16);
                    if (d1 != -1 && d2 != -1) {
                        baos.write((d1 << 4) + d2);
                        i += 2;
                        continue;
                    }
                }
                baos.write(b);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8).replace("\r", "");
        } catch (Exception e) {
            return input;
        }
    }

    private static String decodeRfc2047(String text) {
        if (text == null) return "";
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("=\\?([^?]+)\\?([BQbq])\\?([^?]+)\\?=");
            java.util.regex.Matcher m = p.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String charset = m.group(1);
                String encoding = m.group(2).toUpperCase();
                String data = m.group(3);
                String decoded = data;
                if ("B".equals(encoding)) {
                    byte[] bytes = Base64.getDecoder().decode(data);
                    decoded = new String(bytes, charset);
                } else if ("Q".equals(encoding)) {
                    decoded = decodeQuotedPrintable(data.replace('_', ' '));
                }
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(decoded));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return text;
        }
    }

    private void sendAndLog(BufferedWriter writer, String cmd, String tag) throws IOException {
        log("[" + tag + " SEND]: " + cmd);
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private String readAndLog(BufferedReader reader, String tag) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            log("[" + tag + " RECV]: " + line);
        }
        return line != null ? line : "";
    }

    private String readSmtpResponse(BufferedReader reader, String tag) throws IOException {
        String line;
        String lastLine = "";
        while ((line = reader.readLine()) != null) {
            log("[" + tag + " RECV]: " + line);
            lastLine = line;
            if (line.length() >= 4 && line.charAt(3) != '-') {
                break;
            }
            if (line.length() < 4) {
                break;
            }
        }
        return lastLine;
    }

    private void readMultipleResponses(BufferedReader reader, String tag) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            log("[" + tag + " RECV]: " + line);
            if (line.length() >= 4 && line.charAt(3) != '-') {
                break; // Last response line of multiline response (e.g. "250 8BITMIME")
            }
        }
    }

    public String getEmailAddress() { return emailAddress; }
    public String getUsername() { return username; }
    public boolean isGmailMode() { return isGmailMode; }
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public String getPop3Host() { return pop3Host; }
    public int getPop3Port() { return pop3Port; }
}
