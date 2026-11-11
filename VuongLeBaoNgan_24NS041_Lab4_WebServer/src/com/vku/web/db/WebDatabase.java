package com.vku.web.db;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebDatabase {
    public static class PostEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        public long timestamp;
        public String clientIp;
        public String data;

        public PostEntry(long timestamp, String clientIp, String data) {
            this.timestamp = timestamp;
            this.clientIp = clientIp;
            this.data = data;
        }
    }

    private static final String DATA_DIR = "web_data";
    private static final String POSTS_FILE = DATA_DIR + "/posts.dat";

    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 3306;
    private static final String DB_NAME = "db_web";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private boolean useMySQL = false;
    private Connection conn;
    private final List<PostEntry> memoryPosts = new CopyOnWriteArrayList<>();

    public WebDatabase() {
        initDir();
        initMySQL();
        if (!useMySQL) {
            loadFromFile();
        }
    }

    private void initDir() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException ignored) {}
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
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS web_posts ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "timestamp BIGINT NOT NULL, "
                        + "client_ip VARCHAR(50) NOT NULL, "
                        + "raw_data TEXT NOT NULL, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS web_services ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "service_name VARCHAR(100) NOT NULL, "
                        + "mime_type VARCHAR(50) NOT NULL, "
                        + "file_path VARCHAR(255) NOT NULL, "
                        + "description TEXT)");
            }

            useMySQL = true;
            System.out.println("✓ [WEB DATABASE]: Kết nối MySQL THÀNH CÔNG! Đang sử dụng CSDL: " + DB_NAME + " (Port 3306)");
            loadFromMySQL();
        } catch (Exception e) {
            useMySQL = false;
            System.out.println("ℹ [WEB DATABASE]: Không thể kết nối MySQL (" + e.getMessage() + ").");
            System.out.println("  -> Kích hoạt chế độ lưu trữ File Database (.dat) an toàn!");
        }
    }

    private void loadFromMySQL() {
        if (!useMySQL || conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM web_posts ORDER BY timestamp DESC LIMIT 100");
            while (rs.next()) {
                memoryPosts.add(new PostEntry(rs.getLong("timestamp"), rs.getString("client_ip"), rs.getString("raw_data")));
            }
        } catch (SQLException ignored) {}
    }

    public synchronized void savePost(String clientIp, String rawData) {
        long ts = System.currentTimeMillis();
        PostEntry entry = new PostEntry(ts, clientIp, rawData);
        memoryPosts.add(0, entry);

        if (useMySQL && conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO web_posts (timestamp, client_ip, raw_data) VALUES (?, ?, ?)")) {
                ps.setLong(1, ts);
                ps.setString(2, clientIp);
                ps.setString(3, rawData);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Lỗi lưu POST vào MySQL: " + e.getMessage());
            }
        }
        saveToFile();
    }

    public List<PostEntry> getAllPosts() {
        return new ArrayList<>(memoryPosts);
    }

    public boolean isUsingMySQL() {
        return useMySQL;
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromFile() {
        File f = new File(POSTS_FILE);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                memoryPosts.addAll((List<PostEntry>) ois.readObject());
            } catch (Exception ignored) {}
        }
    }

    private synchronized void saveToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(POSTS_FILE))) {
            int limit = Math.min(memoryPosts.size(), 200);
            oos.writeObject(new ArrayList<>(memoryPosts.subList(0, limit)));
        } catch (Exception ignored) {}
    }
}

