package com.vku.chat.server;

import com.vku.chat.db.ChatDatabase;
import com.vku.chat.model.ChatMessage;
import com.vku.chat.model.Room;
import com.vku.chat.model.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static final int TCP_PORT = 8888;
    private final ChatDatabase database;
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private boolean running = false;

    private UdpMulticastService udpMulticastService;
    private UdpMediaStreamer udpAudioStreamer;
    private UdpMediaStreamer udpVideoStreamer;

    // Optional GUI components
    private JFrame frame;
    private JTextArea logArea;
    private JLabel clientCountLabel;
    private DefaultListModel<String> clientListModel;

    public ChatServer() {
        this.database = new ChatDatabase();
        initUdpServices();
    }

    private void initUdpServices() {
        // UDP Multicast receiver for announcements
        udpMulticastService = new UdpMulticastService(
                UdpMulticastService.DEFAULT_MULTICAST_GROUP,
                UdpMulticastService.DEFAULT_MULTICAST_PORT,
                (msg) -> log("[UDP Multicast]: " + msg)
        );
        udpMulticastService.start();

        // UDP Audio Streamer
        udpAudioStreamer = new UdpMediaStreamer(UdpMediaStreamer.AUDIO_STREAM_PORT, (from, data) -> {
            log("[UDP Audio Stream]: Nhận gói âm thanh từ " + from + " (" + data.length + " bytes)");
        });
        udpAudioStreamer.startListening();

        // UDP Video Streamer & Relay
        udpVideoStreamer = new UdpMediaStreamer(UdpMediaStreamer.VIDEO_STREAM_PORT, (from, data) -> {
            MediaPacket mp = MediaPacket.decode(data, data.length);
            if (mp != null) {
                if (mp.getType() == MediaPacket.TYPE_VIDEO_FRAME && mp.getSeq() % 30 == 0) {
                    log("[UDP Video Relay]: " + mp.getSender() + " -> " + mp.getTarget()
                            + " (Khung #" + mp.getSeq() + ", " + (data.length / 1024) + " KB)");
                } else if (mp.getType() == MediaPacket.TYPE_REGISTER) {
                    log("[UDP Media Client Registered]: " + mp.getSender() + " tại " + from);
                }
            } else {
                log("[UDP Video Stream]: Nhận khung hình video từ " + from + " (" + data.length + " bytes)");
            }
        });
        udpVideoStreamer.startListening();
    }

    public void startServer() {
        if (running) return;
        running = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(TCP_PORT);
                log("Chat Server đã khởi động trên cổng TCP: " + TCP_PORT);
                log("UDP Multicast: " + UdpMulticastService.DEFAULT_MULTICAST_GROUP + ":" + UdpMulticastService.DEFAULT_MULTICAST_PORT);
                log("UDP Audio Port: " + UdpMediaStreamer.AUDIO_STREAM_PORT + " | UDP Video Port: " + UdpMediaStreamer.VIDEO_STREAM_PORT);

                while (running && !serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket, this);
                    threadPool.submit(handler);
                }
            } catch (IOException e) {
                if (running) log("Lỗi ServerSocket: " + e.getMessage());
            }
        }, "TCP-Acceptor").start();
    }

    public synchronized void onClientConnected(ClientHandler handler) {
        String username = handler.getUsername();
        activeClients.put(username, handler);
        log("Client kết nối thành công: " + username + " (" + handler.getUser().getFullName() + ")");

        updateGuiClientList();
        broadcastUserList();

        // Send welcome and rooms
        handler.sendMessage(ChatMessage.system("Chào mừng " + handler.getUser().getFullName() + " đến với VKU Chat Server!"));
        broadcastMessage(ChatMessage.system("Người dùng " + handler.getUser().getFullName() + " đã tham gia phòng chat."));
    }

    public synchronized void onClientDisconnected(ClientHandler handler) {
        String username = handler.getUsername();
        if (username != null && activeClients.remove(username) != null) {
            database.setUserOffline(username);
            log("Client ngắt kết nối: " + username);
            updateGuiClientList();
            broadcastUserList();
            broadcastMessage(ChatMessage.system("Người dùng " + username + " đã rời khỏi phòng chat."));
        }
    }

    public void broadcastMessage(ChatMessage message) {
        database.saveMessage(message);
        log("[BROADCAST] " + message.getSender() + ": " + message.getContent());
        for (ClientHandler client : activeClients.values()) {
            client.sendMessage(message);
        }
    }

    public void sendToRoom(ChatMessage message) {
        database.saveMessage(message);
        String room = message.getRoom();
        log("[MULTICAST/ROOM: " + room + "] " + message.getSender() + ": " + message.getContent());

        for (ClientHandler client : activeClients.values()) {
            if (client.isInRoom(room)) {
                client.sendMessage(message);
            }
        }

        // Also broadcast over UDP Multicast if it's the multicast group
        if ("study".equalsIgnoreCase(room)) {
            udpMulticastService.sendMulticast("[" + message.getSender() + "]: " + message.getContent());
        }
    }

    public void sendPrivateMessage(ChatMessage message) {
        database.saveMessage(message);
        String recipient = message.getRecipient();
        log("[PRIVATE] " + message.getSender() + " -> " + recipient + ": " + message.getContent());

        ClientHandler target = activeClients.get(recipient);
        if (target != null) {
            target.sendMessage(message);
        }

        // Echo back to sender so they see their sent message
        ClientHandler senderHandler = activeClients.get(message.getSender());
        if (senderHandler != null && senderHandler != target) {
            senderHandler.sendMessage(message);
        }
    }

    public void handleCallSignaling(ChatMessage message) {
        String recipient = message.getRecipient();
        log("[CALL SIGNAL] " + message.getType() + ": " + message.getSender() + " -> " + recipient);
        ClientHandler target = activeClients.get(recipient);
        if (target != null) {
            target.sendMessage(message);
        } else {
            ClientHandler sender = activeClients.get(message.getSender());
            if (sender != null) {
                sender.sendMessage(ChatMessage.callReject("HỆ THỐNG", message.getSender(), "Người dùng @" + recipient + " hiện không trực tuyến!"));
            }
        }
    }

    public void broadcastUserList() {
        StringBuilder sb = new StringBuilder();
        for (String u : activeClients.keySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(u);
        }
        ChatMessage listMsg = new ChatMessage(ChatMessage.Type.USER_LIST, "SERVER", sb.toString());
        for (ClientHandler client : activeClients.values()) {
            client.sendMessage(listMsg);
        }
    }

    public boolean isRoom(String name) {
        for (Room r : database.getAllRooms()) {
            if (r.getId().equalsIgnoreCase(name) || r.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public ChatDatabase getDatabase() {
        return database;
    }

    public void log(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String line = "[" + timestamp + "] " + message;
        System.out.println(line);
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(line + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    private void updateGuiClientList() {
        if (clientListModel == null || clientCountLabel == null) return;
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String user : activeClients.keySet()) {
                clientListModel.addElement(user);
            }
            clientCountLabel.setText("Đang online: " + activeClients.size() + " client");
        });
    }

    public void initGUI() {
        frame = new JFrame("VKU Chat Server - Quản Trị Trung Tâm (LTM)");
        frame.setSize(800, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 119, 242));
        JLabel titleLabel = new JLabel("  HỆ THỐNG MÁY CHỦ CHAT LTM (TCP + UDP MULTICAST)");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setPreferredSize(new Dimension(500, 40));
        clientCountLabel = new JLabel("Đang online: 0 client  ");
        clientCountLabel.setForeground(Color.WHITE);
        clientCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(clientCountLabel, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(245, 247, 250));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Nhật Ký Hoạt Động Hệ Thống (TCP/UDP Logs)"));

        // Right side: online clients list
        clientListModel = new DefaultListModel<>();
        JList<String> clientJList = new JList<>(clientListModel);
        clientJList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane clientScroll = new JScrollPane(clientJList);
        clientScroll.setPreferredSize(new Dimension(180, 0));
        clientScroll.setBorder(BorderFactory.createTitledBorder("Clients Online"));

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(scrollLog, BorderLayout.CENTER);
        centerPanel.add(clientScroll, BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClearLog = new JButton("Xóa Nhật Ký");
        btnClearLog.addActionListener(e -> logArea.setText(""));
        JButton btnSendAnnounce = new JButton("Gửi Thông Báo Hệ Thống");
        btnSendAnnounce.addActionListener(e -> {
            String text = JOptionPane.showInputDialog(frame, "Nhập nội dung thông báo broadcast toàn bộ client:");
            if (text != null && !text.trim().isEmpty()) {
                broadcastMessage(ChatMessage.system("[THÔNG BÁO TỪ MÁY CHỦ]: " + text.trim()));
            }
        });
        bottomPanel.add(btnClearLog);
        bottomPanel.add(btnSendAnnounce);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            ChatServer server = new ChatServer();
            server.initGUI();
            server.startServer();
        });
    }
}
