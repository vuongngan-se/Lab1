package com.vku.ftp.db;

import com.vku.ftp.common.FtpLog;
import com.vku.ftp.common.FtpUser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FtpDatabase {
    private static final String DATA_DIR = "ftp_data";
    private static final String USERS_FILE = DATA_DIR + "/ftp_users.dat";
    private static final String LOGS_FILE = DATA_DIR + "/ftp_logs.dat";

    private final Map<String, FtpUser> users = new ConcurrentHashMap<>();
    private final List<FtpLog> logs = new CopyOnWriteArrayList<>();

    public FtpDatabase() {
        initDirectories();
        load();
        initDefaultUsers();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get("ftp_root"));
            Files.createDirectories(Paths.get("ftp_root/documents"));
            Files.createDirectories(Paths.get("ftp_root/public"));

            // Create a sample welcome file in ftp_root
            File sample = new File("ftp_root/welcome.txt");
            if (!sample.exists()) {
                Files.writeString(sample.toPath(), "Chào mừng bạn đến với VKU FTP Server (Lập Trình Mạng).\nMáy chủ FTP hỗ trợ RFC 959 đầy đủ chế độ PASV và PORT.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo thư mục FTP: " + e.getMessage());
        }
    }

    private void initDefaultUsers() {
        if (!users.containsKey("admin")) {
            users.put("admin", new FtpUser("admin", "admin123", "ftp_root", true, true));
        }
        if (!users.containsKey("vku")) {
            users.put("vku", new FtpUser("vku", "123456", "ftp_root", true, false));
        }
        if (!users.containsKey("anonymous")) {
            users.put("anonymous", new FtpUser("anonymous", "guest", "ftp_root/public", false, false));
        }
        saveUsers();
    }

    public synchronized boolean addUser(FtpUser user) {
        if (user == null || user.getUsername() == null) return false;
        users.put(user.getUsername().toLowerCase(), user);
        saveUsers();
        return true;
    }

    public synchronized boolean deleteUser(String username) {
        if (username == null) return false;
        boolean removed = users.remove(username.toLowerCase()) != null;
        if (removed) saveUsers();
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
        saveLogs();
    }

    public List<FtpLog> getAllLogs() {
        return new ArrayList<>(logs);
    }

    @SuppressWarnings("unchecked")
    private synchronized void load() {
        File uf = new File(USERS_FILE);
        if (uf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(uf))) {
                users.putAll((Map<String, FtpUser>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp người dùng FTP: " + e.getMessage());
            }
        }

        File lf = new File(LOGS_FILE);
        if (lf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(lf))) {
                logs.addAll((List<FtpLog>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp nhật ký FTP: " + e.getMessage());
            }
        }
    }

    private synchronized void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(new HashMap<>(users));
        } catch (Exception e) {
            System.err.println("Lỗi lưu người dùng FTP: " + e.getMessage());
        }
    }

    private synchronized void saveLogs() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOGS_FILE))) {
            int start = Math.max(0, logs.size() - 500);
            List<FtpLog> toSave = new ArrayList<>(logs.subList(start, logs.size()));
            oos.writeObject(toSave);
        } catch (Exception e) {
            System.err.println("Lỗi lưu nhật ký FTP: " + e.getMessage());
        }
    }
}
