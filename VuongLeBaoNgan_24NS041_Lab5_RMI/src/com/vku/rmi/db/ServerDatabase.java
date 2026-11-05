package com.vku.rmi.db;

import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.SyncEvent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerDatabase {
    private final String serverId;
    private final String dbName;
    private final String dbHost;
    private final int dbPort;
    private final String dbUser;
    private final String dbPass;

    private boolean useMysql = false;
    private Connection connection;

    // File fallback storage
    private final String dataDir;
    private final String recordsFile;
    private final String eventsFile;

    // Memory cache
    private final Map<String, DataRecord> records = new ConcurrentHashMap<>();
    private final List<SyncEvent> events = new CopyOnWriteArrayList<>();

    public ServerDatabase(String serverId, String dataDir) {
        this.serverId = serverId;
        this.dataDir = dataDir;
        this.recordsFile = dataDir + "/records.dat";
        this.eventsFile = dataDir + "/sync_events.dat";

        // Generate database name based on serverId (e.g. SERVER_1 -> db_server1)
        this.dbName = "db_" + serverId.toLowerCase().replace("-", "").replace("_", "");
        this.dbHost = System.getProperty("db.host", "127.0.0.1");
        this.dbPort = Integer.parseInt(System.getProperty("db.port", "3306"));
        this.dbUser = System.getProperty("db.user", "root");
        this.dbPass = System.getProperty("db.password", "");

        initStorage();
    }

    private void initStorage() {
        initDirectory();
        boolean mysqlSuccess = initMysql();

        if (mysqlSuccess) {
            this.useMysql = true;
            System.out.println("  [DATABASE - " + serverId + "] ĐÃ KẾT NỐI THÀNH CÔNG MYSQL: database '" + dbName + "' trên " + dbHost + ":" + dbPort);
            loadFromMysql();
            if (records.isEmpty()) {
                initSampleData();
            }
        } else {
            this.useMysql = false;
            System.out.println("  [DATABASE - " + serverId + "] ⚠ Không kết nối được MySQL. Sử dụng lưu trữ File dự phòng (" + dataDir + ")");
            loadFromFile();
            if (records.isEmpty()) {
                initSampleData();
            }
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
            connection = DriverManager.getConnection(url, dbUser, dbPass);
        }
        return connection;
    }

    private boolean initMysql() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. Kết nối tới server MySQL để tự động tạo database nếu chưa có
            String serverUrl = "jdbc:mysql://" + dbHost + ":" + dbPort
                    + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
            try (Connection conn = DriverManager.getConnection(serverUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            }

            // 2. Kết nối tới database vừa tạo và tạo bảng nếu chưa có
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                String createRecordsTable = "CREATE TABLE IF NOT EXISTS records ("
                        + "id VARCHAR(50) PRIMARY KEY, "
                        + "name VARCHAR(100) NOT NULL, "
                        + "department VARCHAR(100) NOT NULL, "
                        + "score DOUBLE NOT NULL, "
                        + "version BIGINT NOT NULL DEFAULT 1, "
                        + "last_modified BIGINT NOT NULL, "
                        + "updated_by_server VARCHAR(50) NOT NULL"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
                stmt.executeUpdate(createRecordsTable);

                String createEventsTable = "CREATE TABLE IF NOT EXISTS sync_events ("
                        + "id VARCHAR(50) PRIMARY KEY, "
                        + "timestamp BIGINT NOT NULL, "
                        + "type VARCHAR(20) NOT NULL, "
                        + "record_id VARCHAR(50) NOT NULL, "
                        + "origin_server VARCHAR(50) NOT NULL, "
                        + "target_server VARCHAR(50) NOT NULL, "
                        + "description TEXT"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
                stmt.executeUpdate(createEventsTable);
            }
            return true;
        } catch (Exception e) {
            System.err.println("  [MySQL Init Error] " + e.getMessage());
            return false;
        }
    }

    private void loadFromMysql() {
        records.clear();
        events.clear();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, name, department, score, version, last_modified, updated_by_server FROM records");
            while (rs.next()) {
                DataRecord r = new DataRecord();
                r.setId(rs.getString("id"));
                r.setName(rs.getString("name"));
                r.setDepartment(rs.getString("department"));
                r.setScore(rs.getDouble("score"));
                r.setVersion(rs.getLong("version"));
                r.setLastModified(rs.getLong("last_modified"));
                r.setUpdatedByServer(rs.getString("updated_by_server"));
                records.put(r.getId(), r);
            }

            ResultSet rsEvents = stmt.executeQuery("SELECT id, timestamp, type, record_id, origin_server, target_server, description FROM sync_events ORDER BY timestamp ASC");
            while (rsEvents.next()) {
                try {
                    SyncEvent.Type type = SyncEvent.Type.valueOf(rsEvents.getString("type"));
                    SyncEvent ev = new SyncEvent(
                            type,
                            rsEvents.getString("record_id"),
                            rsEvents.getString("origin_server"),
                            rsEvents.getString("target_server"),
                            rsEvents.getString("description")
                    );
                    events.add(ev);
                } catch (Exception ignored) {}
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đọc dữ liệu từ MySQL: " + e.getMessage());
        }
    }

    private void initDirectory() {
        try {
            Files.createDirectories(Paths.get(dataDir));
        } catch (IOException e) {
            System.err.println("Lỗi tạo thư mục database cho " + serverId + ": " + e.getMessage());
        }
    }

    private void initSampleData() {
        saveOrUpdateRecordLocal(new DataRecord("24NS001", "Nguyễn Văn An", "Khoa Học Máy Tính", 3.85, serverId));
        saveOrUpdateRecordLocal(new DataRecord("24NS002", "Trần Thị Bình", "Kỹ Thuật Phần Mềm", 3.70, serverId));
        saveOrUpdateRecordLocal(new DataRecord("24NS003", "Lê Hoàng Cường", "An Toàn Thông Tin", 3.92, serverId));
    }

    public synchronized boolean saveOrUpdateRecordLocal(DataRecord record) {
        if (record == null || record.getId() == null) return false;
        record.setUpdatedByServer(serverId);
        record.setLastModified(System.currentTimeMillis());
        records.put(record.getId(), record);

        if (useMysql) {
            saveRecordToMysql(record);
        } else {
            persistFile();
        }
        return true;
    }

    public synchronized boolean applyReplication(DataRecord record, String originServerId) {
        if (record == null || record.getId() == null) return false;
        DataRecord existing = records.get(record.getId());
        if (existing != null) {
            if (record.getVersion() >= existing.getVersion()) {
                records.put(record.getId(), record);
                if (useMysql) saveRecordToMysql(record);
                else persistFile();
            }
        } else {
            records.put(record.getId(), record);
            if (useMysql) saveRecordToMysql(record);
            else persistFile();
        }
        return true;
    }

    private void saveRecordToMysql(DataRecord r) {
        String sql = "INSERT INTO records (id, name, department, score, version, last_modified, updated_by_server) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "name = VALUES(name), department = VALUES(department), score = VALUES(score), "
                + "version = VALUES(version), last_modified = VALUES(last_modified), updated_by_server = VALUES(updated_by_server);";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getName());
            ps.setString(3, r.getDepartment());
            ps.setDouble(4, r.getScore());
            ps.setLong(5, r.getVersion());
            ps.setLong(6, r.getLastModified());
            ps.setString(7, r.getUpdatedByServer());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi ghi MySQL: " + e.getMessage());
        }
    }

    public synchronized boolean deleteRecordLocal(String id) {
        if (id == null) return false;
        boolean removed = records.remove(id) != null;
        if (removed) {
            if (useMysql) deleteRecordFromMysql(id);
            else persistFile();
        }
        return removed;
    }

    public synchronized boolean applyReplicationDelete(String id) {
        if (id == null) return false;
        boolean removed = records.remove(id) != null;
        if (removed) {
            if (useMysql) deleteRecordFromMysql(id);
            else persistFile();
        }
        return removed;
    }

    private void deleteRecordFromMysql(String id) {
        String sql = "DELETE FROM records WHERE id = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi xóa record trên MySQL: " + e.getMessage());
        }
    }

    public DataRecord getRecord(String id) {
        return records.get(id);
    }

    public List<DataRecord> getAllRecords() {
        List<DataRecord> list = new ArrayList<>(records.values());
        list.sort(Comparator.comparing(DataRecord::getId));
        return list;
    }

    public int getRecordCount() {
        return records.size();
    }

    public synchronized void recordEvent(SyncEvent event) {
        if (event == null) return;
        events.add(event);
        if (useMysql) {
            saveEventToMysql(event);
        } else {
            saveEventsFile();
        }
    }

    private void saveEventToMysql(SyncEvent ev) {
        String sql = "INSERT INTO sync_events (id, timestamp, type, record_id, origin_server, target_server, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE description = VALUES(description);";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ev.getId());
            ps.setLong(2, ev.getTimestamp());
            ps.setString(3, ev.getType().name());
            ps.setString(4, ev.getRecordId());
            ps.setString(5, ev.getOriginServer());
            ps.setString(6, ev.getTargetServer());
            ps.setString(7, ev.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi ghi sync_event vào MySQL: " + e.getMessage());
        }
    }

    public List<SyncEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    public boolean isUsingMysql() {
        return useMysql;
    }

    public String getDbName() {
        return dbName;
    }

    // --- File Persistence Fallback ---
    @SuppressWarnings("unchecked")
    private synchronized void loadFromFile() {
        File rf = new File(recordsFile);
        if (rf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(rf))) {
                records.putAll((Map<String, DataRecord>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp database file " + serverId + ": " + e.getMessage());
            }
        }

        File ef = new File(eventsFile);
        if (ef.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ef))) {
                events.addAll((List<SyncEvent>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp sync events file " + serverId + ": " + e.getMessage());
            }
        }
    }

    private synchronized void persistFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(recordsFile))) {
            oos.writeObject(new HashMap<>(records));
        } catch (Exception ignored) {}
    }

    private synchronized void saveEventsFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(eventsFile))) {
            int start = Math.max(0, events.size() - 500);
            oos.writeObject(new ArrayList<>(events.subList(start, events.size())));
        } catch (Exception ignored) {}
    }
}
