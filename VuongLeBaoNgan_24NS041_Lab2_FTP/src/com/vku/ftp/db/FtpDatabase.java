package com.vku.ftp.db;

import com.vku.ftp.common.FtpLog;
import com.vku.ftp.common.FtpUser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FtpDatabase {
    private static final String DATA_DIR = "ftp_data";
    private static final String USERS_FILE = DATA_DIR + "/ftp_users.dat";
    private static final String LOGS_FILE = DATA_DIR + "/ftp_logs.dat";

    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "db_ftp";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private boolean useMySQL = false;
    private Connection conn;

    private final Map<String, FtpUser> users = new ConcurrentHashMap<>();
    private final List<FtpLog> logs = new CopyOnWriteArrayList<>();

    public FtpDatabase() {
        initDirectories();
        initMySQL();
        if (!useMySQL) {
            loadFromFile();
        }
        initDefaultUsers();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get("ftp_root"));
            Files.createDirectories(Paths.get("ftp_root/documents"));
            Files.createDirectories(Paths.get("ftp_root/public"));

            File sample = new File("ftp_root/welcome.txt");
            if (!sample.exists()) {
                Files.writeString(sample.toPath(), "Chào mừng bạn đến với VKU FTP Server (Lập Trình Mạng).\nMáy chủ FTP hỗ trợ RFC 959 đầy đủ chế độ PASV và PORT.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo thư mục FTP: " + e.getMessage());
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
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ftp_users ("
                        + "username VARCHAR(50) PRIMARY KEY, "
                        + "password VARCHAR(100) NOT NULL, "
                        + "home_directory VARCHAR(255) NOT NULL, "
                        + "can_write BOOLEAN NOT NULL DEFAULT TRUE, "
                        + "can_delete BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "enabled BOOLEAN NOT NULL DEFAULT TRUE)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ftp_logs ("
                        + "id VARCHAR(50) PRIMARY KEY, "
                        + "timestamp BIGINT NOT NULL, "
                        + "client_ip VARCHAR(50) NOT NULL, "
                        + "username VARCHAR(50) NOT NULL, "
                        + "action VARCHAR(50) NOT NULL, "
                        + "target_path VARCHAR(255), "
                        + "status VARCHAR(20) NOT NULL, "
                        + "bytes_transferred BIGINT NOT NULL DEFAULT 0)");
            }

            useMySQL = true;
            System.out.println("✓ [FTP DATABASE]: Kết nối MySQL THÀNH CÔNG! Đang sử dụng CSDL: " + DB_NAME + " (Port 3306)");
            loadFromMySQL();
        } catch (Exception e) {
            useMySQL = false;
            System.out.println("ℹ [FTP DATABASE]: Không thể kết nối MySQL (" + e.getMessage() + ").");
            System.out.println("  -> Kích hoạt chế độ lưu trữ File Database (.dat) an toàn!");
        }
    }

    private void loadFromMySQL() {
        if (!useMySQL || conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM ftp_users");
            while (rs.next()) {
                FtpUser u = new FtpUser(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("home_directory"),
                        rs.getBoolean("can_write"),
                        rs.getBoolean("can_delete")
                );
                u.setEnabled(rs.getBoolean("enabled"));
                users.put(u.getUsername().toLowerCase(), u);
            }

            ResultSet logRs = stmt.executeQuery("SELECT * FROM ftp_logs ORDER BY timestamp ASC");
            while (logRs.next()) {
                FtpLog l = new FtpLog(
                        logRs.getString("client_ip"),
                        logRs.getString("username"),
                        logRs.getString("action"),
                        logRs.getString("target_path"),
                        logRs.getString("status"),
                        logRs.getLong("bytes_transferred")
                );
                logs.add(l);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi nạp dữ liệu từ MySQL: " + e.getMessage());
        }
    }

    private void initDefaultUsers() {
        if (!users.containsKey("admin")) {
            addUser(new FtpUser("admin", "admin123", "ftp_root", true, true));
        }
        if (!users.containsKey("vku")) {
            addUser(new FtpUser("vku", "123456", "ftp_root", true, false));
        }
        if (!users.containsKey("anonymous")) {
            addUser(new FtpUser("anonymous", "guest", "ftp_root/public", false, false));
        }
    }

    public synchronized boolean addUser(FtpUser user) {
        if (user == null || user.getUsername() == null) return false;
        users.put(user.getUsername().toLowerCase(), user);

        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ftp_users (username, password, home_directory, can_write, can_delete, enabled) "
                            + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                            + "password=VALUES(password), home_directory=VALUES(home_directory), "
                            + "can_write=VALUES(can_write), can_delete=VALUES(can_delete), enabled=VALUES(enabled)")) {
                ps.setString(1, user.getUsername().toLowerCase());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getHomeDirectory());
                ps.setBoolean(4, user.isCanWrite());
                ps.setBoolean(5, user.isCanDelete());
                ps.setBoolean(6, user.isEnabled());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi thêm user vào MySQL: " + e.getMessage());
            }
        }
        saveUsersToFile();
        return true;
    }

    public synchronized boolean deleteUser(String username) {
        if (username == null) return false;
        boolean removed = users.remove(username.toLowerCase()) != null;
        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ftp_users WHERE username = ?")) {
                ps.setString(1, username.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi xóa user trong MySQL: " + e.getMessage());
            }
        }
        if (removed) saveUsersToFile();
        return removed;
    }

    public FtpUser authenticate(String username, String password) {
        if (username == null) return null;
        FtpUser user = users.get(username.toLowerCase());
        if (user != null && user.isEnabled()) {
            if ("anonymous".equalsIgnoreCase(username) || user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public List<FtpUser> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized void logAction(FtpLog log) {
        if (log == null) return;
        logs.add(log);

        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ftp_logs (id, timestamp, client_ip, username, action, target_path, status, bytes_transferred) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, log.getId());
                ps.setLong(2, log.getTimestamp());
                ps.setString(3, log.getClientIp());
                ps.setString(4, log.getUsername());
                ps.setString(5, log.getAction());
                ps.setString(6, log.getTargetPath());
                ps.setString(7, log.getStatus());
                ps.setLong(8, log.getBytesTransferred());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi ghi log vào MySQL: " + e.getMessage());
            }
        }
        saveLogsToFile();
    }

    public List<FtpLog> getAllLogs() {
        return new ArrayList<>(logs);
    }

    public boolean isUsingMySQL() {
        return useMySQL;
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromFile() {
        File uf = new File(USERS_FILE);
        if (uf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(uf))) {
                users.putAll((Map<String, FtpUser>) ois.readObject());
            } catch (Exception ignored) {}
        }

        File lf = new File(LOGS_FILE);
        if (lf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(lf))) {
                logs.addAll((List<FtpLog>) ois.readObject());
            } catch (Exception ignored) {}
        }
    }

    private synchronized void saveUsersToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(new HashMap<>(users));
        } catch (Exception ignored) {}
    }

    private synchronized void saveLogsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOGS_FILE))) {
            int start = Math.max(0, logs.size() - 500);
            List<FtpLog> toSave = new ArrayList<>(logs.subList(start, logs.size()));
            oos.writeObject(toSave);
        } catch (Exception ignored) {}
    }
}
