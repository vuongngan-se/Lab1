package com.vku.chat.client;

import com.vku.chat.model.ChatMessage;
import com.vku.chat.server.UdpMediaStreamer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ChatClientGUI extends JFrame {
    private ChatClient client;
    private String currentTarget = "general"; // "general" or username or room id
    private boolean isPrivate = false;

    // UI Components
    private JLabel headerTitleLabel;
    private JLabel headerSubLabel;
    private JTextPane chatPane;
    private HTMLEditorKit htmlKit;
    private HTMLDocument htmlDoc;
    private JTextField inputField;
    private DefaultListModel<String> onlineUsersModel;
    private JList<String> onlineUsersList;
    private DefaultListModel<String> channelModel;
    private JList<String> channelList;

    // In-memory cache for received files
    private final Map<String, byte[]> receivedFiles = new HashMap<>();
    private String serverHost = "localhost";
    private int serverPort = 8888;
    private VideoCallDialog activeCallDialog = null;

    public ChatClientGUI(String initialUser, String initialPass) {
        this(initialUser, initialPass, "localhost");
    }

    public ChatClientGUI(String initialUser, String initialPass, String host) {
        super("VKU Chat - Messenger (LTM)");
        if (host != null && !host.trim().isEmpty()) {
            this.serverHost = host.trim();
        }
        initUI();
        if (initialUser != null && !initialUser.isEmpty()) {
            SwingUtilities.invokeLater(() -> performLogin(this.serverHost, initialUser, initialPass));
        } else {
            SwingUtilities.invokeLater(this::showLoginDialog);
        }
    }

    private void initUI() {
        setSize(960, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(new Color(245, 247, 250));

        // Left Sidebar: Users and Rooms
        JPanel leftSidebar = createSidebar();
        mainPanel.add(leftSidebar, BorderLayout.WEST);

        // Center: Chat view & Input
        JPanel chatPanel = createChatArea();
        mainPanel.add(chatPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(new Color(240, 242, 245));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(218, 224, 233)));

        // User Profile Header
        JPanel profilePanel = new JPanel(new BorderLayout(8, 0));
        profilePanel.setBackground(new Color(24, 119, 242));
        profilePanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel avatarLabel = new JLabel("👤", JLabel.CENTER);
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        avatarLabel.setForeground(Color.WHITE);

        JPanel profileText = new JPanel(new GridLayout(2, 1));
        profileText.setOpaque(false);
        JLabel nameLabel = new JLabel("Chưa đăng nhập");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel statusBadge = new JLabel("Offline");
        statusBadge.setForeground(new Color(220, 230, 255));
        statusBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        profileText.add(nameLabel);
        profileText.add(statusBadge);

        profilePanel.add(avatarLabel, BorderLayout.WEST);
        profilePanel.add(profileText, BorderLayout.CENTER);
        sidebar.add(profilePanel, BorderLayout.NORTH);

        // Channels & Online Users
        JPanel listsPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        listsPanel.setOpaque(false);
        listsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Channels (Broadcast & Multicast)
        channelModel = new DefaultListModel<>();
        channelModel.addElement("🌐 Phòng Chung (Broadcast)");
        channelModel.addElement("📡 Nhóm Học Tập (Multicast UDP)");
        channelModel.addElement("💡 Công Nghệ & Dự Án");
        channelList = new JList<>(channelModel);
        channelList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        channelList.setSelectedIndex(0);
        channelList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && channelList.getSelectedValue() != null) {
                onlineUsersList.clearSelection();
                isPrivate = false;
                int idx = channelList.getSelectedIndex();
                if (idx == 0) {
                    currentTarget = "general";
                    headerTitleLabel.setText("🌐 Phòng Chung (Broadcast toàn hệ thống)");
                    headerSubLabel.setText("Mọi người trong máy chủ đều nhận được tin nhắn");
                } else if (idx == 1) {
                    currentTarget = "study";
                    headerTitleLabel.setText("📡 Nhóm Học Tập (Multicast UDP 224.2.2.3)");
                    headerSubLabel.setText("Truyền phát đa hướng Multicast Socket tới các thành viên");
                } else {
                    currentTarget = "tech";
                    headerTitleLabel.setText("💡 Công Nghệ & Dự Án (Multicast UDP 224.2.2.4)");
                    headerSubLabel.setText("Nhóm trao đổi kỹ thuật phân tán");
                }
            }
        });
        JScrollPane channelScroll = new JScrollPane(channelList);
        channelScroll.setBorder(BorderFactory.createTitledBorder("Kênh Chat (Broadcast / Multicast)"));
        listsPanel.add(channelScroll);

        // Online Users
        onlineUsersModel = new DefaultListModel<>();
        onlineUsersList = new JList<>(onlineUsersModel);
        onlineUsersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        onlineUsersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineUsersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onlineUsersList.getSelectedValue() != null) {
                String selected = onlineUsersList.getSelectedValue();
                if (client != null && selected.equalsIgnoreCase(client.getCurrentUser())) {
                    return; // Don't chat with self
                }
                channelList.clearSelection();
                isPrivate = true;
                currentTarget = selected;
                headerTitleLabel.setText("💬 Chat Riêng với: " + selected);
                headerSubLabel.setText("Tin nhắn TCP bảo mật 1-đối-1");
            }
        });
        JScrollPane usersScroll = new JScrollPane(onlineUsersList);
        usersScroll.setBorder(BorderFactory.createTitledBorder("Bạn Bè Đang Online (TCP)"));
        listsPanel.add(usersScroll);

        sidebar.add(listsPanel, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel createChatArea() {
        JPanel chatPanel = new JPanel(new BorderLayout(0, 0));
        chatPanel.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 240)),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel headerText = new JPanel(new GridLayout(2, 1));
        headerText.setOpaque(false);
        headerTitleLabel = new JLabel("🌐 Phòng Chung (Broadcast toàn hệ thống)");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerSubLabel = new JLabel("Mọi người trong máy chủ đều nhận được tin nhắn");
        headerSubLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        headerSubLabel.setForeground(Color.GRAY);
        headerText.add(headerTitleLabel);
        headerText.add(headerSubLabel);
        header.add(headerText, BorderLayout.WEST);

        chatPanel.add(header, BorderLayout.NORTH);

        // Message Pane
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        htmlKit = new HTMLEditorKit();
        chatPane.setEditorKit(htmlKit);
        initChatPaneContent();

        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // Bottom Controls & Input
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBorder(new EmptyBorder(8, 12, 12, 12));
        bottomContainer.setBackground(Color.WHITE);

        // Toolbar: Emoji, File, Voice, Video, Save File
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);

        JButton btnEmoji = new JButton("😀 Emoji");
        btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        btnEmoji.addActionListener(e -> {
            EmojiDialog dialog = new EmojiDialog(this, emoji -> inputField.setText(inputField.getText() + emoji));
            dialog.setVisible(true);
        });

        JButton btnFile = new JButton("📎 Đính kèm tệp");
        btnFile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnFile.addActionListener(e -> selectAndSendFile());

        JButton btnAudio = new JButton("🎤 Voice Audio (UDP)");
        btnAudio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAudio.addActionListener(e -> sendAudioMessage());

        JButton btnCall = new JButton("📞 Gọi Video (UDP)");
        btnCall.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCall.setForeground(new Color(2, 132, 199));
        btnCall.addActionListener(e -> startVideoCall());

        JButton btnVideo = new JButton("📷 Snapshot (UDP)");
        btnVideo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnVideo.addActionListener(e -> sendVideoMessage());

        JButton btnDownloadLast = new JButton("💾 Lưu tệp nhận");
        btnDownloadLast.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDownloadLast.addActionListener(e -> saveReceivedFile());

        toolbar.add(btnEmoji);
        toolbar.add(btnFile);
        toolbar.add(btnAudio);
        toolbar.add(btnCall);
        toolbar.add(btnVideo);
        toolbar.add(btnDownloadLast);

        bottomContainer.add(toolbar, BorderLayout.NORTH);

        // Input field and send button
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.setBorder(new EmptyBorder(8, 0, 0, 0));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendTextMessage();
                }
            }
        });

        JButton btnSend = new JButton(" Gửi (Enter) ");
        btnSend.setBackground(new Color(24, 119, 242));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setFocusPainted(false);
        btnSend.addActionListener(e -> sendTextMessage());

        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(btnSend, BorderLayout.EAST);

        bottomContainer.add(inputRow, BorderLayout.SOUTH);
        chatPanel.add(bottomContainer, BorderLayout.SOUTH);

        return chatPanel;
    }

    private void showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField hostField = new JTextField(this.serverHost);
        JTextField userField = new JTextField("alice");
        JPasswordField passField = new JPasswordField("123456");

        panel.add(new JLabel("IP Máy Chủ (Server IP):"));
        panel.add(hostField);
        panel.add(new JLabel("Tên đăng nhập (alice, bob...):"));
        panel.add(userField);
        panel.add(new JLabel("Mật khẩu:"));
        panel.add(passField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Đăng Nhập VKU Chat (LTM)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText().trim();
            if (host.isEmpty()) host = "localhost";
            this.serverHost = host;
            performLogin(this.serverHost, userField.getText().trim(), new String(passField.getPassword()));
        } else {
            System.exit(0);
        }
    }

    private void performLogin(String host, String username, String password) {
        new Thread(() -> {
            client = new ChatClient(host, serverPort);
            client.setMessageListener(this::onMessageReceived);
            client.setUserListListener(this::onUserListUpdated);
            client.setStatusListener(status -> appendHtml("<table width='100%' border='0' cellpadding='2' cellspacing='0'><tr><td align='center' style='color:#ef4444;font-size:11px;'>[Hệ Thống]: " + escapeHtml(status) + "</td></tr></table>"));

            boolean ok = client.connect(username, password, username);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    setTitle("VKU Chat - [" + client.getCurrentFullName() + " (@" + username + ")] - Server: " + host);
                    appendHtml("<table width='100%' border='0' cellpadding='4' cellspacing='0'><tr><td align='center' style='color:#059669;font-weight:bold;font-size:11px;'>✓ Đã kết nối máy chủ (" + host + ") thành công với tài khoản: " + escapeHtml(client.getCurrentFullName()) + "</td></tr></table>");
                } else {
                    JOptionPane.showMessageDialog(this, "Đăng nhập thất bại! Không thể kết nối tới Server: " + host + ":" + serverPort + "\nVui lòng kiểm tra lại địa chỉ IP máy chủ và Tường lửa (Firewall).", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
                    showLoginDialog();
                }
            });
        }, "Login-Thread").start();
    }

    private void sendTextMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null || !client.isConnected()) return;

        ChatMessage msg;
        if (isPrivate) {
            msg = ChatMessage.text(client.getCurrentUser(), currentTarget, text, true);
        } else if ("study".equalsIgnoreCase(currentTarget) || "tech".equalsIgnoreCase(currentTarget)) {
            msg = ChatMessage.roomMessage(client.getCurrentUser(), currentTarget, text);
        } else {
            msg = new ChatMessage(ChatMessage.Type.BROADCAST, client.getCurrentUser(), text);
        }

        client.send(msg);
        inputField.setText("");
    }

    private void selectAndSendFile() {
        if (client == null || !client.isConnected()) return;
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                ChatMessage msg = ChatMessage.file(
                        client.getCurrentUser(),
                        isPrivate ? currentTarget : "ALL",
                        isPrivate ? null : currentTarget,
                        file.getName(),
                        data
                );
                client.send(msg);
                appendHtml("<table width='100%' border='0' cellpadding='0' cellspacing='0' style='margin:4px 0;'><tr><td align='right'><div style='display:inline-block;background-color:#dbeafe;padding:6px 10px;color:#1e40af;font-size:12px;'>📤 Bạn đã gửi tệp: <b>" + escapeHtml(file.getName()) + "</b> (" + (data.length / 1024) + " KB)</div></td></tr></table>");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Không thể đọc tệp: " + ex.getMessage());
            }
        }
    }

    private void sendAudioMessage() {
        if (client == null || !client.isConnected()) return;
        byte[] audioData = AudioRecorderSimulator.generateSimulatedAudio(3); // 3-second audio note
        ChatMessage msg = ChatMessage.audio(
                client.getCurrentUser(),
                isPrivate ? currentTarget : "ALL",
                isPrivate ? null : currentTarget,
                audioData,
                3
        );
        client.send(msg);
        // Also transmit UDP packet to demonstrate UDP stream
        client.sendUdpMedia(audioData, UdpMediaStreamer.AUDIO_STREAM_PORT);
        appendHtml("<table width='100%' border='0' cellpadding='0' cellspacing='0' style='margin:4px 0;'><tr><td align='right'><div style='display:inline-block;background-color:#dbeafe;padding:6px 10px;color:#6b21a8;font-size:12px;'>🎤 Bạn đã gửi tin nhắn thoại 3s (UDP)</div></td></tr></table>");
    }

    private void startVideoCall() {
        if (client == null || !client.isConnected()) return;
        if (!isPrivate || currentTarget == null || currentTarget.trim().isEmpty()
                || "general".equalsIgnoreCase(currentTarget)
                || "study".equalsIgnoreCase(currentTarget)
                || "tech".equalsIgnoreCase(currentTarget)) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một người bạn trong danh sách 'Bạn Bè Đang Online' để thực hiện cuộc gọi Video 1-1!",
                    "Gọi Video Call (UDP)",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (activeCallDialog != null && activeCallDialog.isDisplayable()) {
            JOptionPane.showMessageDialog(this, "Bạn đang trong một cuộc gọi khác!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Send signaling CALL_REQUEST via TCP
        client.send(ChatMessage.callRequest(client.getCurrentUser(), currentTarget, "VIDEO"));

        // Open VideoCallDialog as caller
        activeCallDialog = new VideoCallDialog(this, client, currentTarget, currentTarget, serverHost, true);
        activeCallDialog.setVisible(true);

        appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr><td align='center' style='color:#0284c7;font-weight:bold;font-size:11px;'>📞 Đang gọi Video tới @" + escapeHtml(currentTarget) + " (Đang chờ đối phương trả lời...)</td></tr></table>");
    }

    private void handleIncomingCall(ChatMessage msg) {
        if (activeCallDialog != null && activeCallDialog.isDisplayable()) {
            client.send(ChatMessage.callReject(client.getCurrentUser(), msg.getSender(), "Người nhận đang bận"));
            return;
        }

        int choice = JOptionPane.showOptionDialog(
                this,
                "📞 Người dùng @" + msg.getSender() + " đang gọi Video cho bạn!\nBạn có muốn nhận cuộc gọi không?",
                "Cuộc Gọi Video Đến (VKU UDP Video)",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"🟢 Trả lời (Accept)", "🔴 Từ chối (Decline)"},
                "🟢 Trả lời (Accept)"
        );

        if (choice == 0) {
            // Accept
            client.send(ChatMessage.callAccept(client.getCurrentUser(), msg.getSender()));
            activeCallDialog = new VideoCallDialog(this, client, msg.getSender(), msg.getSender(), serverHost, false);
            activeCallDialog.onCallAccepted();
            activeCallDialog.setVisible(true);
            appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr><td align='center' style='color:#059669;font-weight:bold;font-size:11px;'>✓ Bạn đã nhận cuộc gọi Video từ @" + escapeHtml(msg.getSender()) + "</td></tr></table>");
        } else {
            // Reject
            client.send(ChatMessage.callReject(client.getCurrentUser(), msg.getSender(), "Người nhận đã từ chối cuộc gọi"));
            appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr><td align='center' style='color:#dc2626;font-size:11px;'>✗ Bạn đã từ chối cuộc gọi Video từ @" + escapeHtml(msg.getSender()) + "</td></tr></table>");
        }
    }

    private void sendVideoMessage() {
        if (client == null || !client.isConnected()) return;
        byte[] videoFrame = VideoStreamSimulator.generateVideoSnapshot(
                "Video Call - " + client.getCurrentFullName(),
                "VKU Network Programming Lab 1"
        );
        ChatMessage msg = ChatMessage.video(
                client.getCurrentUser(),
                isPrivate ? currentTarget : "ALL",
                isPrivate ? null : currentTarget,
                videoFrame,
                "640x480 30fps"
        );
        client.send(msg);
        // Transmit frame via UDP socket
        client.sendUdpMedia(videoFrame, UdpMediaStreamer.VIDEO_STREAM_PORT);
        appendHtml("<table width='100%' border='0' cellpadding='0' cellspacing='0' style='margin:4px 0;'><tr><td align='right'><div style='display:inline-block;background-color:#dbeafe;padding:6px 10px;color:#b45309;font-size:12px;'>🎥 Bạn đã gửi snapshot video (UDP)</div></td></tr></table>");
    }

    private void saveReceivedFile() {
        if (receivedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa nhận được tệp đính kèm nào!");
            return;
        }
        String[] fileNames = receivedFiles.keySet().toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
                this, "Chọn tệp cần lưu xuống máy tính:", "Lưu Tệp Đính Kèm",
                JOptionPane.QUESTION_MESSAGE, null, fileNames, fileNames[0]
        );
        if (selected != null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(selected));
            int res = chooser.showSaveDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                    fos.write(receivedFiles.get(selected));
                    JOptionPane.showMessageDialog(this, "Đã lưu tệp thành công vào: " + chooser.getSelectedFile().getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi lưu tệp: " + ex.getMessage());
                }
            }
        }
    }

    private void onMessageReceived(ChatMessage msg) {
        SwingUtilities.invokeLater(() -> {
            boolean isMe = client != null && msg.getSender() != null && msg.getSender().equalsIgnoreCase(client.getCurrentUser());

            String bodyHtml = "";
            switch (msg.getType()) {
                case TEXT:
                case EMOJI:
                case BROADCAST:
                case MULTICAST:
                case PRIVATE:
                    bodyHtml = "<span style='font-size:13px;'>" + escapeHtml(msg.getContent()) + "</span>";
                    break;
                case FILE:
                    if (msg.getFileData() != null && msg.getFileName() != null) {
                        receivedFiles.put(msg.getFileName(), msg.getFileData());
                        bodyHtml = "📁 <b>Tệp đính kèm:</b> " + escapeHtml(msg.getFileName())
                                + " (" + (msg.getFileData().length / 1024) + " KB)<br/>"
                                + "<small style='color:#2563eb;'>[Bấm nút 'Lưu tệp nhận' ở thanh công cụ để tải]</small>";
                    } else {
                        bodyHtml = "📁 " + escapeHtml(msg.getContent());
                    }
                    break;
                case AUDIO:
                    bodyHtml = "🎤 <b>Tin nhắn thoại</b> (" + msg.getAudioDurationSec() + "s)<br/>"
                            + "<small style='color:#059669;'>[Đang phát âm thanh qua loa...]</small>";
                    if (msg.getFileData() != null) {
                        AudioRecorderSimulator.playAudio(msg.getFileData());
                    }
                    break;
                case VIDEO:
                    bodyHtml = "🎥 <b>Video Call Snapshot:</b> " + escapeHtml(msg.getMediaMeta()) + "<br/>"
                            + "<small style='color:#d97706;'>[Bấm vào để mở trình phát video]</small>";
                    if (msg.getFileData() != null) {
                        VideoStreamSimulator.showVideoPlayer(this, msg.getSender(), msg.getFileData());
                    }
                    break;
                case CALL_REQUEST:
                    handleIncomingCall(msg);
                    return;
                case CALL_ACCEPT:
                    if (activeCallDialog != null) {
                        activeCallDialog.onCallAccepted();
                    }
                    appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr><td align='center' style='color:#059669;font-weight:bold;font-size:11px;'>✓ @" + escapeHtml(msg.getSender())
                            + " đã nhận cuộc gọi. Đang truyền phát luồng Video UDP 2 chiều...</td></tr></table>");
                    return;
                case CALL_REJECT:
                    if (activeCallDialog != null) {
                        activeCallDialog.onCallRejected(msg.getContent());
                    }
                    appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr><td align='center' style='color:#dc2626;font-size:11px;'>✗ Cuộc gọi tới @" + escapeHtml(msg.getSender())
                            + " bị từ chối: " + escapeHtml(msg.getContent()) + "</td></tr></table>");
                    return;
                case CALL_END:
                    if (activeCallDialog != null) {
                        activeCallDialog.onCallEnded();
                        activeCallDialog = null;
                    }
                    appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr><td align='center' style='color:#64748b;font-size:11px;'>📞 Cuộc gọi Video với @" + escapeHtml(msg.getSender()) + " đã kết thúc.</td></tr></table>");
                    return;
                case SYSTEM:
                    appendHtml("<table width='100%' border='0' cellpadding='3' cellspacing='0' style='margin:3px 0;'>"
                            + "<tr><td align='center' style='color:#64748b;font-size:11px;'>"
                            + "— " + escapeHtml(msg.getContent()) + " —"
                            + "</td></tr></table>");
                    return;
                default:
                    bodyHtml = escapeHtml(msg.getContent());
                    break;
            }

            String align = isMe ? "right" : "left";
            String bubbleBg = isMe ? "#dbeafe" : "#f1f5f9"; // Soft blue for me, clean light gray for other
            String nameColor = isMe ? "#1d4ed8" : "#334155";
            String senderName = isMe ? "Bạn" : msg.getSender();

            String html = String.format(
                    "<table width='100%%' border='0' cellpadding='0' cellspacing='0' style='margin:4px 0;'>"
                    + "<tr><td align='%s'>"
                    + "<table border='0' cellpadding='6' cellspacing='0' bgcolor='%s' style='background-color:%s;'>"
                    + "<tr><td>"
                    + "<div style='font-size:10px;font-weight:bold;color:%s;margin-bottom:2px;'>%s "
                    + "<span style='font-weight:normal;color:#64748b;font-size:9px;'>%s</span></div>"
                    + "<div style='font-size:13px;color:#0f172a;'>%s</div>"
                    + "</td></tr>"
                    + "</table>"
                    + "</td></tr>"
                    + "</table>",
                    align, bubbleBg, bubbleBg, nameColor, senderName, msg.getFormattedTime(), bodyHtml
            );
            appendHtml(html);
        });
    }

    private void onUserListUpdated(String[] users) {
        SwingUtilities.invokeLater(() -> {
            onlineUsersModel.clear();
            for (String u : users) {
                if (u != null && !u.trim().isEmpty()) {
                    onlineUsersModel.addElement(u.trim());
                }
            }
        });
    }

    private void initChatPaneContent() {
        chatPane.setText("<html><head><style>"
                + "body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; margin: 8px; padding: 0; background-color: #ffffff; }"
                + "</style></head><body><div id='chat-box'>"
                + "<table width='100%' border='0' cellpadding='4' cellspacing='0'>"
                + "<tr><td align='center' style='color:#94a3b8;font-size:11px;'>"
                + "— Chào mừng bạn đến với VKU Chat. Kết nối TCP &amp; UDP sẵn sàng. —"
                + "</td></tr></table>"
                + "</div></body></html>");
        htmlDoc = (HTMLDocument) chatPane.getDocument();
    }

    private void appendHtml(String html) {
        SwingUtilities.invokeLater(() -> {
            try {
                Element chatBox = htmlDoc.getElement("chat-box");
                if (chatBox != null) {
                    htmlDoc.insertBeforeEnd(chatBox, html);
                } else {
                    htmlKit.insertHTML(htmlDoc, htmlDoc.getLength(), html, 0, 0, javax.swing.text.html.HTML.Tag.DIV);
                }
                chatPane.setCaretPosition(chatPane.getDocument().getLength());
            } catch (Exception ignored) {}
        });
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            String user = args.length > 0 ? args[0] : null;
            String pass = args.length > 1 ? args[1] : "123456";
            String host = args.length > 2 ? args[2] : "localhost";
            ChatClientGUI gui = new ChatClientGUI(user, pass, host);
            gui.setVisible(true);
        });
    }
}
