package com.vku.mail.db;

import com.vku.mail.model.MailAccount;
import com.vku.mail.model.MailLog;
import com.vku.mail.model.MailMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MailDatabase {
    private static final String DATA_DIR = "mail_data";
    private static final String ACCOUNTS_FILE = DATA_DIR + "/accounts.dat";
    private static final String MESSAGES_FILE = DATA_DIR + "/messages.dat";
    private static final String LOGS_FILE = DATA_DIR + "/logs.dat";

    private final Map<String, MailAccount> accounts = new ConcurrentHashMap<>();
    private final List<MailMessage> messages = new CopyOnWriteArrayList<>();
    private final List<MailLog> logs = new CopyOnWriteArrayList<>();

    public MailDatabase() {
        initDir();
        load();
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

    private void initDefaultAccounts() {
        addAccountIfAbsent("alice", "123456", "alice@vku.udn.vn", "Alice Nguyễn");
        addAccountIfAbsent("bob", "123456", "bob@vku.udn.vn", "Bob Trần");
        addAccountIfAbsent("charlie", "123456", "charlie@vku.udn.vn", "Charlie Lê");
        addAccountIfAbsent("admin", "admin123", "admin@localhost", "Quản Trị Viên");
    }

    private void addAccountIfAbsent(String user, String pass, String email, String name) {
        if (!accounts.containsKey(user.toLowerCase()) && !accounts.containsKey(email.toLowerCase())) {
            MailAccount acc = new MailAccount(user.toLowerCase(), pass, email.toLowerCase(), name);
            accounts.put(user.toLowerCase(), acc);
            accounts.put(email.toLowerCase(), acc);
            saveAccounts();
        }
    }

    private void initSampleEmails() {
        if (messages.isEmpty()) {
            MailMessage welcome = new MailMessage("admin@localhost", "alice@vku.udn.vn",
                    "Chào mừng bạn đến với VKU Mail Server",
                    "Xin chào Alice,\n\nHệ thống Mail Server Lập Trình Mạng hỗ trợ đầy đủ giao thức SMTP (gửi thư cổng 2525) và POP3 (nhận thư cổng 1100).\nChúc bạn thực hiện tốt bài Lab!\n\nTrân trọng,\nAdmin VKU");
            messages.add(welcome);

            MailMessage assignment = new MailMessage("teacher@vku.udn.vn", "alice@vku.udn.vn",
                    "Thông báo kiểm tra Lab môn LTM",
                    "Gửi sinh viên Alice,\n\nBuổi chấm điểm Lab LTM sẽ diễn ra theo lịch. Nhớ chuẩn bị kiến thức về POP3 và SMTP.\n\nGiảng viên phụ trách");
            messages.add(assignment);
            saveMessages();
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
        saveMessages();
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
        saveMessages();
    }

    public synchronized void log(MailLog log) {
        if (log == null) return;
        logs.add(log);
        saveLogs();
    }

    public List<MailLog> getAllLogs() {
        return new ArrayList<>(logs);
    }

    @SuppressWarnings("unchecked")
    private synchronized void load() {
        File af = new File(ACCOUNTS_FILE);
        if (af.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(af))) {
                accounts.putAll((Map<String, MailAccount>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp accounts: " + e.getMessage());
            }
        }

        File mf = new File(MESSAGES_FILE);
        if (mf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mf))) {
                messages.addAll((List<MailMessage>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp messages: " + e.getMessage());
            }
        }

        File lf = new File(LOGS_FILE);
        if (lf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(lf))) {
                logs.addAll((List<MailLog>) ois.readObject());
            } catch (Exception e) {
                System.err.println("Lỗi nạp logs: " + e.getMessage());
            }
        }
    }

    private synchronized void saveAccounts() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ACCOUNTS_FILE))) {
            oos.writeObject(new HashMap<>(accounts));
        } catch (Exception ignored) {}
    }

    private synchronized void saveMessages() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MESSAGES_FILE))) {
            oos.writeObject(new ArrayList<>(messages));
        } catch (Exception ignored) {}
    }

    private synchronized void saveLogs() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOGS_FILE))) {
            int start = Math.max(0, logs.size() - 500);
            oos.writeObject(new ArrayList<>(logs.subList(start, logs.size())));
        } catch (Exception ignored) {}
    }
}
