package com.vku.mail.db;

import com.vku.mail.model.MailAccount;
import com.vku.mail.model.MailLog;
import com.vku.mail.model.MailMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MailDatabase {
    private static final String DATA_DIR = "mail_data";
    private static final String ACCOUNTS_FILE = DATA_DIR + "/accounts.dat";
    private static final String MESSAGES_FILE = DATA_DIR + "/messages.dat";
    private static final String LOGS_FILE = DATA_DIR + "/logs.dat";

    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "db_mail";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private boolean useMySQL = false;
    private Connection conn;

    private final Map<String, MailAccount> accounts = new ConcurrentHashMap<>();
    private final List<MailMessage> messages = new CopyOnWriteArrayList<>();
    private final List<MailLog> logs = new CopyOnWriteArrayList<>();

    public MailDatabase() {
        initDir();
        initMySQL();
        if (!useMySQL) {
            loadFromFile();
        }
        initDefaultAccounts();
        initSampleEmails();
    }

    private void initDir() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Lỗi tạo thư mục mail_data: " + e.getMessage());
        }
    }

    private void initMySQL() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String rawUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            try (Connection rootConn = DriverManager.getConnection(rawUrl, DB_USER, DB_PASS);
                 Statement stmt = rootConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }

            String dbUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            conn = DriverManager.getConnection(dbUrl, DB_USER, DB_PASS);

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mail_accounts ("
                        + "username VARCHAR(50) PRIMARY KEY, "
                        + "password VARCHAR(100) NOT NULL, "
                        + "email VARCHAR(100) NOT NULL UNIQUE, "
                        + "full_name VARCHAR(100) NOT NULL)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mail_messages ("
                        + "id VARCHAR(50) PRIMARY KEY, "
                        + "sender VARCHAR(100) NOT NULL, "
                        + "recipient VARCHAR(100) NOT NULL, "
                        + "subject VARCHAR(255) NOT NULL, "
                        + "body TEXT, "
                        + "attachment_name VARCHAR(255), "
                        + "timestamp BIGINT NOT NULL, "
                        + "is_read BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "is_deleted BOOLEAN NOT NULL DEFAULT FALSE)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mail_logs ("
                        + "id VARCHAR(50) PRIMARY KEY, "
                        + "timestamp BIGINT NOT NULL, "
                        + "protocol VARCHAR(20) NOT NULL, "
                        + "client_ip VARCHAR(50) NOT NULL, "
                        + "details TEXT, "
                        + "status VARCHAR(20) NOT NULL)");
            }

            useMySQL = true;
            System.out.println("✓ [MAIL DATABASE]: Kết nối MySQL THÀNH CÔNG! Đang sử dụng CSDL: " + DB_NAME + " (Port 3306)");
            loadFromMySQL();
        } catch (Exception e) {
            useMySQL = false;
            System.out.println("ℹ [MAIL DATABASE]: Không thể kết nối MySQL (" + e.getMessage() + ").");
            System.out.println("  -> Kích hoạt chế độ lưu trữ File Database (.dat) an toàn!");
        }
    }

    private void loadFromMySQL() {
        if (!useMySQL || conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM mail_accounts");
            while (rs.next()) {
                MailAccount acc = new MailAccount(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("full_name")
                );
                accounts.put(acc.getUsername().toLowerCase(), acc);
                accounts.put(acc.getEmail().toLowerCase(), acc);
            }

            ResultSet msgRs = stmt.executeQuery("SELECT * FROM mail_messages ORDER BY timestamp ASC");
            while (msgRs.next()) {
                MailMessage m = new MailMessage(
                        msgRs.getString("sender"),
                        msgRs.getString("recipient"),
                        msgRs.getString("subject"),
                        msgRs.getString("body")
                );
                m.setId(msgRs.getString("id"));
                m.setAttachmentName(msgRs.getString("attachment_name"));
                m.setTimestamp(msgRs.getLong("timestamp"));
                m.setRead(msgRs.getBoolean("is_read"));
                m.setDeleted(msgRs.getBoolean("is_deleted"));
                messages.add(m);
            }

            ResultSet logRs = stmt.executeQuery("SELECT * FROM mail_logs ORDER BY timestamp ASC");
            while (logRs.next()) {
                MailLog l = new MailLog(
                        logRs.getString("protocol"),
                        logRs.getString("client_ip"),
                        logRs.getString("details"),
                        logRs.getString("status")
                );
                logs.add(l);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi nạp mail từ MySQL: " + e.getMessage());
        }
    }

    private void initDefaultAccounts() {
        addAccountIfAbsent("alice", "123456", "alice@vku.udn.vn", "Alice Nguyễn");
        addAccountIfAbsent("bob", "123456", "bob@vku.udn.vn", "Bob Trần");
        addAccountIfAbsent("charlie", "123456", "charlie@vku.udn.vn", "Charlie Lê");
        addAccountIfAbsent("admin", "admin123", "admin@localhost", "Quản Trị Viên");
    }

    public synchronized void addAccountIfAbsent(String user, String pass, String email, String name) {
        if (!accounts.containsKey(user.toLowerCase()) && !accounts.containsKey(email.toLowerCase())) {
            MailAccount acc = new MailAccount(user.toLowerCase(), pass, email.toLowerCase(), name);
            accounts.put(user.toLowerCase(), acc);
            accounts.put(email.toLowerCase(), acc);

            if (useMySQL && conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO mail_accounts (username, password, email, full_name) VALUES (?, ?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE password=VALUES(password), full_name=VALUES(full_name)")) {
                    ps.setString(1, user.toLowerCase());
                    ps.setString(2, pass);
                    ps.setString(3, email.toLowerCase());
                    ps.setString(4, name);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    System.err.println("Lỗi thêm account vào MySQL: " + e.getMessage());
                }
            }
            saveAccountsToFile();
        }
    }

    private void initSampleEmails() {
        if (messages.isEmpty()) {
            MailMessage welcome = new MailMessage("admin@localhost", "alice@vku.udn.vn",
                    "Chào mừng bạn đến với VKU Mail Server",
                    "Xin chào Alice,\n\nHệ thống Mail Server Lập Trình Mạng hỗ trợ đầy đủ giao thức SMTP (gửi thư cổng 2525) và POP3 (nhận thư cổng 1100).\nChúc bạn thực hiện tốt bài Lab!\n\nTrân trọng,\nAdmin VKU");
            saveMail(welcome);

            MailMessage assignment = new MailMessage("teacher@vku.udn.vn", "alice@vku.udn.vn",
                    "Thông báo kiểm tra Lab môn LTM",
                    "Gửi sinh viên Alice,\n\nBuổi chấm điểm Lab LTM sẽ diễn ra theo lịch. Nhớ chuẩn bị kiến thức về POP3 và SMTP.\n\nGiảng viên phụ trách");
            saveMail(assignment);
        }
    }

    public MailAccount authenticate(String userOrEmail, String password) {
        if (userOrEmail == null || password == null) return null;
        MailAccount acc = accounts.get(userOrEmail.trim().toLowerCase());
        if (acc != null && acc.getPassword().equals(password)) {
            return acc;
        }
        return null;
    }

    public MailAccount getAccount(String userOrEmail) {
        if (userOrEmail == null) return null;
        return accounts.get(userOrEmail.trim().toLowerCase());
    }

    public List<MailAccount> getAllAccounts() {
        Set<String> seen = new HashSet<>();
        List<MailAccount> result = new ArrayList<>();
        for (MailAccount acc : accounts.values()) {
            if (seen.add(acc.getUsername())) {
                result.add(acc);
            }
        }
        return result;
    }

    public synchronized void saveMail(MailMessage message) {
        if (message == null) return;
        messages.add(message);

        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mail_messages (id, sender, recipient, subject, body, attachment_name, timestamp, is_read, is_deleted) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE is_read=VALUES(is_read), is_deleted=VALUES(is_deleted)")) {
                ps.setString(1, message.getId());
                ps.setString(2, message.getSender());
                ps.setString(3, message.getRecipient());
                ps.setString(4, message.getSubject());
                ps.setString(5, message.getBody());
                ps.setString(6, message.getAttachmentName());
                ps.setLong(7, message.getTimestamp());
                ps.setBoolean(8, message.isRead());
                ps.setBoolean(9, message.isDeleted());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi lưu thư vào MySQL: " + e.getMessage());
            }
        }
        saveMessagesToFile();
    }

    public List<MailMessage> getInbox(String emailOrUser) {
        List<MailMessage> result = new ArrayList<>();
        String target = emailOrUser.toLowerCase();
        for (MailMessage m : messages) {
            if (!m.isDeleted() && (m.getRecipient().toLowerCase().contains(target) || target.contains(m.getRecipient().toLowerCase()))) {
                result.add(m);
            }
        }
        return result;
    }

    public List<MailMessage> getSent(String emailOrUser) {
        List<MailMessage> result = new ArrayList<>();
        String target = emailOrUser.toLowerCase();
        for (MailMessage m : messages) {
            if (!m.isDeleted() && (m.getSender().toLowerCase().contains(target) || target.contains(m.getSender().toLowerCase()))) {
                result.add(m);
            }
        }
        return result;
    }

    public List<MailMessage> getAllMessages() {
        return new ArrayList<>(messages);
    }

    public synchronized void deleteMail(String messageId) {
        for (MailMessage m : messages) {
            if (m.getId().equalsIgnoreCase(messageId)) {
                m.setDeleted(true);
                break;
            }
        }
        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE mail_messages SET is_deleted = TRUE WHERE id = ?")) {
                ps.setString(1, messageId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi cập nhật xóa thư trong MySQL: " + e.getMessage());
            }
        }
        saveMessagesToFile();
    }

    public synchronized void log(MailLog log) {
        if (log == null) return;
        logs.add(log);

        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mail_logs (id, timestamp, protocol, client_ip, details, status) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, log.getId());
                ps.setLong(2, log.getTimestamp());
                ps.setString(3, log.getProtocol());
                ps.setString(4, log.getClientIp());
                ps.setString(5, log.getDetails());
                ps.setString(6, log.getStatus());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi ghi log mail vào MySQL: " + e.getMessage());
            }
        }
        saveLogsToFile();
    }

    public List<MailLog> getAllLogs() {
        return new ArrayList<>(logs);
    }

    public boolean isUsingMySQL() {
        return useMySQL;
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromFile() {
        File af = new File(ACCOUNTS_FILE);
        if (af.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(af))) {
                accounts.putAll((Map<String, MailAccount>) ois.readObject());
            } catch (Exception ignored) {}
        }

        File mf = new File(MESSAGES_FILE);
        if (mf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mf))) {
                messages.addAll((List<MailMessage>) ois.readObject());
            } catch (Exception ignored) {}
        }

        File lf = new File(LOGS_FILE);
        if (lf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(lf))) {
                logs.addAll((List<MailLog>) ois.readObject());
            } catch (Exception ignored) {}
        }
    }

    private synchronized void saveAccountsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ACCOUNTS_FILE))) {
            oos.writeObject(new HashMap<>(accounts));
        } catch (Exception ignored) {}
    }

    private synchronized void saveMessagesToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MESSAGES_FILE))) {
            oos.writeObject(new ArrayList<>(messages));
        } catch (Exception ignored) {}
    }

    private synchronized void saveLogsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOGS_FILE))) {
            int start = Math.max(0, logs.size() - 500);
            oos.writeObject(new ArrayList<>(logs.subList(start, logs.size())));
        } catch (Exception ignored) {}
    }
}
