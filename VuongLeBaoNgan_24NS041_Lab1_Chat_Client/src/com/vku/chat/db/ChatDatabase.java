package com.vku.chat.db;

import com.vku.chat.model.ChatMessage;
import com.vku.chat.model.Room;
import com.vku.chat.model.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatDatabase {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.dat";
    private static final String MESSAGES_FILE = DATA_DIR + "/messages.dat";

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final List<ChatMessage> messageHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Room> defaultRooms = new ConcurrentHashMap<>();

    public ChatDatabase() {
        initDataDir();
        initDefaultRooms();
        loadFromDisk();
        initDefaultUsers();
    }

    private void initDataDir() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Không thể tạo thư mục dữ liệu: " + e.getMessage());
        }
    }

    private void initDefaultRooms() {
        Room general = new Room("general", "Phòng Chung (Broadcast)", "Kênh chat chung cho tất cả thành viên", false, null, 0);
        Room studyGroup = new Room("study", "Nhóm Học Tập (Multicast UDP)", "Kênh thảo luận môn LTM", true, "224.2.2.3", 8889);
        Room techGroup = new Room("tech", "Công Nghệ & Dự Án", "Trao đổi kỹ thuật socket & RMI", true, "224.2.2.4", 8890);

        defaultRooms.put(general.getId(), general);
        defaultRooms.put(studyGroup.getId(), studyGroup);
        defaultRooms.put(techGroup.getId(), techGroup);
    }

    private void initDefaultUsers() {
        registerIfAbsent("alice", "Alice Nguyễn", "123456");
        registerIfAbsent("bob", "Bob Trần", "123456");
        registerIfAbsent("charlie", "Charlie Lê", "123456");
        registerIfAbsent("david", "David Phạm", "123456");
        registerIfAbsent("eve", "Eve Hoàng", "123456");
        registerIfAbsent("frank", "Frank Đỗ", "123456");
    }

    private void registerIfAbsent(String username, String fullName, String password) {
        if (!users.containsKey(username.toLowerCase())) {
            User user = new User(username.toLowerCase(), fullName, password);
            users.put(username.toLowerCase(), user);
            saveUsers();
        }
    }

    public synchronized boolean register(String username, String fullName, String password) {
        if (username == null || password == null || username.trim().isEmpty()) return false;
        String key = username.trim().toLowerCase();
        if (users.containsKey(key)) return false;

        User user = new User(key, fullName != null ? fullName.trim() : key, password);
        users.put(key, user);
        saveUsers();
        return true;
    }

    public synchronized User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User user = users.get(username.trim().toLowerCase());
        if (user != null && user.getPassword().equals(password)) {
            user.setOnline(true);
            user.setLastSeen(System.currentTimeMillis());
            return user;
        }
        return null;
    }

    public synchronized void setUserOffline(String username) {
        if (username == null) return;
        User user = users.get(username.trim().toLowerCase());
        if (user != null) {
            user.setOnline(false);
            user.setLastSeen(System.currentTimeMillis());
        }
    }

    public User getUser(String username) {
        if (username == null) return null;
        return users.get(username.trim().toLowerCase());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(defaultRooms.values());
    }

    public Room getRoom(String roomId) {
        return defaultRooms.get(roomId);
    }

    public synchronized void saveMessage(ChatMessage message) {
        if (message == null) return;
        // Don't keep heavy byte data indefinitely in memory history if too big
        if (message.getFileData() != null && message.getFileData().length > 1024 * 1024 * 5) {
            // Persist large files to disk separately
            String fileDir = DATA_DIR + "/uploads";
            try {
                Files.createDirectories(Paths.get(fileDir));
                String filePath = fileDir + "/" + System.currentTimeMillis() + "_" + message.getFileName();
                Files.write(Paths.get(filePath), message.getFileData());
                message.setMediaMeta(filePath);
            } catch (Exception e) {
                System.err.println("Lỗi lưu file đính kèm: " + e.getMessage());
            }
        }
        messageHistory.add(message);
        saveMessages();
    }

    public List<ChatMessage> getHistoryForRoom(String room, int limit) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage msg : messageHistory) {
            if (room != null && room.equalsIgnoreCase(msg.getRoom())) {
                result.add(msg);
            }
        }
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    public List<ChatMessage> getPrivateHistory(String user1, String user2, int limit) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage msg : messageHistory) {
            if (msg.getType() == ChatMessage.Type.PRIVATE) {
                if ((user1.equalsIgnoreCase(msg.getSender()) && user2.equalsIgnoreCase(msg.getRecipient())) ||
                    (user2.equalsIgnoreCase(msg.getSender()) && user1.equalsIgnoreCase(msg.getRecipient()))) {
                    result.add(msg);
                }
            }
        }
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        File uf = new File(USERS_FILE);
        if (uf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(uf))) {
                Map<String, User> loaded = (Map<String, User>) ois.readObject();
                users.putAll(loaded);
            } catch (Exception e) {
                System.err.println("Lỗi đọc dữ liệu người dùng: " + e.getMessage());
            }
        }

        File mf = new File(MESSAGES_FILE);
        if (mf.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mf))) {
                List<ChatMessage> loaded = (List<ChatMessage>) ois.readObject();
                messageHistory.addAll(loaded);
            } catch (Exception e) {
                System.err.println("Lỗi đọc lịch sử tin nhắn: " + e.getMessage());
            }
        }
    }

    private synchronized void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(new HashMap<>(users));
        } catch (Exception e) {
            System.err.println("Lỗi lưu người dùng: " + e.getMessage());
        }
    }

    private synchronized void saveMessages() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MESSAGES_FILE))) {
            // Keep last 500 messages to prevent uncontrolled file size growth
            int start = Math.max(0, messageHistory.size() - 500);
            List<ChatMessage> toSave = new ArrayList<>(messageHistory.subList(start, messageHistory.size()));
            oos.writeObject(toSave);
        } catch (Exception e) {
            System.err.println("Lỗi lưu lịch sử tin nhắn: " + e.getMessage());
        }
    }
}
