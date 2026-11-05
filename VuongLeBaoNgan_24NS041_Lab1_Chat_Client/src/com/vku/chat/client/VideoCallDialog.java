package com.vku.chat.client;

import com.vku.chat.model.ChatMessage;
import com.vku.chat.server.MediaPacket;
import com.vku.chat.server.UdpMediaStreamer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoCallDialog extends JDialog {
    private final ChatClient client;
    private final String targetUsername;
    private final String targetFullName;
    private final String serverHost;
    private final boolean isCaller;

    // Call state
    private volatile boolean inCall = true;
    private volatile boolean callConnected = false;
    private volatile boolean micEnabled = true;
    private volatile boolean cameraEnabled = true;
    private volatile boolean screenShareMode = false;

    // Network & Media
    private DatagramSocket udpSocket;
    private Thread senderThread;
    private Thread receiverThread;
    private Robot robot;

    // Statistics
    private long callStartTime = 0;
    private javax.swing.Timer durationTimer;
    private int framesSent = 0;
    private int framesReceived = 0;
    private long bytesSent = 0;
    private long bytesReceived = 0;
    private int currentFps = 0;
    private int fpsCounter = 0;
    private long lastFpsTime = System.currentTimeMillis();

    // Video frames
    private BufferedImage currentRemoteFrame;
    private BufferedImage currentLocalFrame;

    // UI Components
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JLabel statsLabel;
    private JPanel videoCanvas;
    private JButton btnToggleMic;
    private JButton btnToggleCam;
    private JButton btnScreenShare;
    private JButton btnHangUp;

    public VideoCallDialog(Frame parent, ChatClient client, String targetUsername,
                           String targetFullName, String serverHost, boolean isCaller) {
        super(parent, "VKU Video Call - [" + client.getCurrentUser() + " 📞 " + targetUsername + "]", false);
        this.client = client;
        this.targetUsername = targetUsername;
        this.targetFullName = targetFullName != null ? targetFullName : targetUsername;
        this.serverHost = serverHost;
        this.isCaller = isCaller;

        try {
            this.robot = new Robot();
        } catch (Exception ignored) {}

        initUDP();
        initUI();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                endCall();
            }
        });

        startMediaThreads();
    }

    private void initUDP() {
        try {
            // Bind to an ephemeral port (0 = system assigns free port)
            this.udpSocket = new DatagramSocket(0);
            sendRegisterPacket();
        } catch (Exception e) {
            System.err.println("Lỗi mở UDP Socket cho Video Call: " + e.getMessage());
        }
    }

    private void sendRegisterPacket() {
        if (udpSocket == null || udpSocket.isClosed()) return;
        try {
            MediaPacket reg = new MediaPacket(
                    MediaPacket.TYPE_REGISTER,
                    client.getCurrentUser(),
                    targetUsername,
                    0,
                    new byte[0]
            );
            byte[] data = reg.encode();
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getByName(serverHost), UdpMediaStreamer.VIDEO_STREAM_PORT
            );
            udpSocket.send(packet);
        } catch (Exception ignored) {}
    }

    private void initUI() {
        setSize(860, 640);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(new Color(15, 23, 42)); // Slate 900
        setLayout(new BorderLayout());

        // --- TOP OVERLAY BAR ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(15, 23, 42, 220));
        topBar.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userInfoPanel.setOpaque(false);
        JLabel avatarIcon = new JLabel("👤");
        avatarIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        avatarIcon.setForeground(Color.WHITE);

        JPanel userText = new JPanel(new GridLayout(2, 1));
        userText.setOpaque(false);
        JLabel nameLbl = new JLabel(targetFullName + " (@" + targetUsername + ")");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(Color.WHITE);

        statusLabel = new JLabel(isCaller ? "📞 Đang gọi... Đang chờ đối phương trả lời" : "🟢 Cuộc gọi đã kết nối");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(isCaller ? new Color(251, 191, 36) : new Color(52, 211, 153));

        userText.add(nameLbl);
        userText.add(statusLabel);
        userInfoPanel.add(avatarIcon);
        userInfoPanel.add(userText);
        topBar.add(userInfoPanel, BorderLayout.WEST);

        // Center: Call duration timer
        timerLabel = new JLabel("⏱ 00:00", JLabel.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(new Color(52, 211, 153)); // Emerald green
        topBar.add(timerLabel, BorderLayout.CENTER);

        // Right: UDP Network Stats
        statsLabel = new JLabel("📶 UDP: 8892 | 0 FPS | 0 KB/s", JLabel.RIGHT);
        statsLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        statsLabel.setForeground(new Color(148, 163, 184));
        topBar.add(statsLabel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // --- CENTER: VIDEO CANVAS (Main remote + Mini PiP local) ---
        videoCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                // 1. Draw Remote Video (Main background)
                if (currentRemoteFrame != null) {
                    drawScaledImage(g2, currentRemoteFrame, 0, 0, w, h);
                } else {
                    // Placeholder when waiting for remote video
                    drawVideoPlaceholder(g2, w, h);
                }

                // 2. Draw Local Video in Picture-in-Picture (PiP) mini window (bottom right)
                int pipW = 200;
                int pipH = 150;
                int pipX = w - pipW - 16;
                int pipY = h - pipH - 16;

                // PiP background & border
                g2.setColor(new Color(30, 41, 59, 230));
                g2.fillRoundRect(pipX, pipY, pipW, pipH, 12, 12);

                if (cameraEnabled && currentLocalFrame != null) {
                    Shape oldClip = g2.getClip();
                    g2.clip(new java.awt.geom.RoundRectangle2D.Float(pipX, pipY, pipW, pipH, 12, 12));
                    g2.drawImage(currentLocalFrame, pipX, pipY, pipW, pipH, null);
                    g2.setClip(oldClip);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    String camOffText = cameraEnabled ? "Đang mở cam..." : "📷 Camera Tắt";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(camOffText, pipX + (pipW - fm.stringWidth(camOffText)) / 2, pipY + pipH / 2);
                }

                // PiP Border
                g2.setColor(new Color(2, 132, 199)); // Sky blue border
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(pipX, pipY, pipW, pipH, 12, 12);

                // PiP Badge label
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(pipX + 8, pipY + 8, 85, 20, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(screenShareMode ? "🖥 Màn hình" : "👤 Bạn (Local)", pipX + 12, pipY + 22);

                g2.dispose();
            }
        };
        videoCanvas.setBackground(new Color(15, 23, 42));
        add(videoCanvas, BorderLayout.CENTER);

        // --- BOTTOM CONTROL BAR ---
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 14));
        bottomBar.setBackground(new Color(15, 23, 42, 240));
        bottomBar.setBorder(new EmptyBorder(8, 0, 8, 0));

        btnToggleMic = createControlButton("🎤 Tắt Mic", new Color(51, 65, 85), Color.WHITE);
        btnToggleMic.addActionListener(e -> toggleMic());

        btnToggleCam = createControlButton("📷 Tắt Cam", new Color(51, 65, 85), Color.WHITE);
        btnToggleCam.addActionListener(e -> toggleCam());

        btnScreenShare = createControlButton("🖥️ Chia sẻ Màn hình", new Color(2, 132, 199), Color.WHITE);
        btnScreenShare.addActionListener(e -> toggleScreenShare());

        btnHangUp = createControlButton("🔴 Gác máy (Kết thúc)", new Color(225, 29, 72), Color.WHITE);
        btnHangUp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnHangUp.addActionListener(e -> endCall());

        bottomBar.add(btnToggleMic);
        bottomBar.add(btnToggleCam);
        bottomBar.add(btnScreenShare);
        bottomBar.add(btnHangUp);

        add(bottomBar, BorderLayout.SOUTH);

        // Timer for duration & statistics
        durationTimer = new javax.swing.Timer(1000, e -> updateDurationAndStats());
    }

    private JButton createControlButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void drawScaledImage(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        double imgAspect = (double) img.getWidth() / img.getHeight();
        double canvasAspect = (double) w / h;

        int drawW, drawH;
        if (canvasAspect > imgAspect) {
            drawH = h;
            drawW = (int) (h * imgAspect);
        } else {
            drawW = w;
            drawH = (int) (w / imgAspect);
        }
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;

        g2.drawImage(img, drawX, drawY, drawW, drawH, null);
    }

    private void drawVideoPlaceholder(Graphics2D g2, int w, int h) {
        // Gradient background
        GradientPaint gp = new GradientPaint(0, 0, new Color(15, 23, 42), w, h, new Color(30, 41, 59));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        int centerX = w / 2;
        int centerY = h / 2 - 20;

        // Pulse wave effect
        long now = System.currentTimeMillis();
        int pulseRadius = 70 + (int) ((Math.sin(now / 300.0) + 1.0) * 15);
        g2.setColor(new Color(2, 132, 199, 40));
        g2.fillOval(centerX - pulseRadius, centerY - pulseRadius, pulseRadius * 2, pulseRadius * 2);

        // Main Avatar Circle
        g2.setColor(new Color(37, 99, 235));
        g2.fillOval(centerX - 50, centerY - 50, 100, 100);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(centerX - 50, centerY - 50, 100, 100);

        // Initials inside avatar
        String initials = targetUsername.length() >= 2 ? targetUsername.substring(0, 2).toUpperCase() : "U";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initials, centerX - fm.stringWidth(initials) / 2, centerY + 12);

        // Name and status text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        fm = g2.getFontMetrics();
        g2.drawString(targetFullName, centerX - fm.stringWidth(targetFullName) / 2, centerY + 80);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.setColor(new Color(148, 163, 184));
        fm = g2.getFontMetrics();
        String subText = !callConnected ? "Đang chờ đối phương nhận cuộc gọi..." : "Đang chờ luồng video từ @" + targetUsername + "...";
        g2.drawString(subText, centerX - fm.stringWidth(subText) / 2, centerY + 105);
    }

    private void startMediaThreads() {
        // Video Sender Thread (~20 FPS -> 50ms interval)
        senderThread = new Thread(() -> {
            int seq = 0;
            while (inCall) {
                try {
                    long loopStart = System.currentTimeMillis();

                    if (cameraEnabled) {
                        BufferedImage frame;
                        if (screenShareMode && robot != null) {
                            frame = captureScreenFrame();
                        } else {
                            frame = generateVirtualCameraFrame(seq);
                        }
                        currentLocalFrame = frame;

                        // Compress frame to JPEG
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(frame, "jpg", baos);
                        byte[] jpegData = baos.toByteArray();

                        // Send via UDP MediaPacket to Server
                        MediaPacket mp = new MediaPacket(
                                MediaPacket.TYPE_VIDEO_FRAME,
                                client.getCurrentUser(),
                                targetUsername,
                                seq++,
                                jpegData
                        );
                        byte[] packetData = mp.encode();
                        DatagramPacket packet = new DatagramPacket(
                                packetData, packetData.length, InetAddress.getByName(serverHost), UdpMediaStreamer.VIDEO_STREAM_PORT
                        );
                        udpSocket.send(packet);

                        framesSent++;
                        bytesSent += packetData.length;
                    }

                    fpsCounter++;
                    long now = System.currentTimeMillis();
                    if (now - lastFpsTime >= 1000) {
                        currentFps = fpsCounter;
                        fpsCounter = 0;
                        lastFpsTime = now;
                    }

                    videoCanvas.repaint();

                    long elapsed = System.currentTimeMillis() - loopStart;
                    long sleepTime = Math.max(10, 50 - elapsed);
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    if (!inCall) break;
                }
            }
        }, "VideoCall-Sender");
        senderThread.setDaemon(true);
        senderThread.start();

        // Video Receiver Thread
        receiverThread = new Thread(() -> {
            byte[] buf = new byte[65507];
            while (inCall && udpSocket != null && !udpSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(packet);

                    MediaPacket mp = MediaPacket.decode(packet.getData(), packet.getLength());
                    if (mp != null && mp.getType() == MediaPacket.TYPE_VIDEO_FRAME) {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(mp.getPayload()));
                        if (img != null) {
                            currentRemoteFrame = img;
                            framesReceived++;
                            bytesReceived += packet.getLength();
                            videoCanvas.repaint();
                        }
                    } else if (mp != null && mp.getType() == MediaPacket.TYPE_END_STREAM) {
                        onCallEnded();
                    }
                } catch (Exception e) {
                    if (!inCall) break;
                }
            }
        }, "VideoCall-Receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private BufferedImage captureScreenFrame() {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(0, 0, screenSize.width, screenSize.height);
            BufferedImage screen = robot.createScreenCapture(screenRect);

            // Scale to 480x360 for fast transmission
            int w = 480;
            int h = 360;
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(screen, 0, 0, w, h, null);

            // Draw cursor position
            try {
                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                int cursorX = (int) (mousePos.x * ((double) w / screenSize.width));
                int cursorY = (int) (mousePos.y * ((double) h / screenSize.height));
                g.setColor(new Color(239, 68, 68)); // Red dot
                g.fillOval(cursorX - 5, cursorY - 5, 10, 10);
                g.setColor(Color.WHITE);
                g.drawOval(cursorX - 6, cursorY - 6, 12, 12);
            } catch (Exception ignored) {}

            // Header watermark
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, w, 24);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.drawString("🖥️ LIVE SCREEN: " + client.getCurrentFullName() + " | " + new SimpleDateFormat("HH:mm:ss").format(new Date()), 10, 16);

            g.dispose();
            return scaled;
        } catch (Exception e) {
            return generateVirtualCameraFrame(0);
        }
    }

    private BufferedImage generateVirtualCameraFrame(int seq) {
        int w = 480;
        int h = 360;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Modern gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(17, 24, 39), w, h, new Color(30, 58, 138));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Live grid pattern
        g2.setColor(new Color(255, 255, 255, 8));
        for (int i = 0; i < w; i += 30) g2.drawLine(i, 0, i, h);
        for (int i = 0; i < h; i += 30) g2.drawLine(0, i, w, i);

        // Center Avatar
        int cx = w / 2;
        int cy = h / 2 - 15;

        // Dynamic audio equalizer bars around avatar if mic is on
        if (micEnabled) {
            g2.setColor(new Color(34, 197, 94, 180)); // Green waves
            int barCount = 18;
            for (int i = 0; i < barCount; i++) {
                double angle = 2 * Math.PI * i / barCount;
                double wave = Math.sin((seq * 0.3) + i) * 15 + 18;
                int x1 = (int) (cx + Math.cos(angle) * 52);
                int y1 = (int) (cy + Math.sin(angle) * 52);
                int x2 = (int) (cx + Math.cos(angle) * (52 + wave));
                int y2 = (int) (cy + Math.sin(angle) * (52 + wave));
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        // Avatar Circle
        g2.setColor(new Color(24, 119, 242));
        g2.fillOval(cx - 45, cy - 45, 90, 90);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(cx - 45, cy - 45, 90, 90);

        String initials = client.getCurrentUser().length() >= 2 ? client.getCurrentUser().substring(0, 2).toUpperCase() : "ME";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initials, cx - fm.stringWidth(initials) / 2, cy + 10);

        // Subtitle
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        fm = g2.getFontMetrics();
        g2.drawString(client.getCurrentFullName(), cx - fm.stringWidth(client.getCurrentFullName()) / 2, cy + 68);

        // Bottom Banner with live stats
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, h - 28, w, 28);

        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(new Color(52, 211, 153));
        g2.drawString("VKU UDP-STREAM [Port 8892]", 10, h - 10);

        g2.setColor(Color.WHITE);
        String liveClock = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        int clockW = g2.getFontMetrics().stringWidth(liveClock);
        g2.drawString(liveClock, w - clockW - 10, h - 10);

        g2.dispose();
        return img;
    }

    private void toggleMic() {
        micEnabled = !micEnabled;
        if (micEnabled) {
            btnToggleMic.setText("🎤 Tắt Mic");
            btnToggleMic.setBackground(new Color(51, 65, 85));
        } else {
            btnToggleMic.setText("🔇 Bật Mic");
            btnToggleMic.setBackground(new Color(220, 38, 38)); // Red
        }
    }

    private void toggleCam() {
        cameraEnabled = !cameraEnabled;
        if (cameraEnabled) {
            btnToggleCam.setText("📷 Tắt Cam");
            btnToggleCam.setBackground(new Color(51, 65, 85));
        } else {
            btnToggleCam.setText("🚫 Bật Cam");
            btnToggleCam.setBackground(new Color(220, 38, 38));
            currentLocalFrame = null;
        }
        videoCanvas.repaint();
    }

    private void toggleScreenShare() {
        screenShareMode = !screenShareMode;
        if (screenShareMode) {
            btnScreenShare.setText("🎭 Dùng Camera Ảo");
            btnScreenShare.setBackground(new Color(168, 85, 247)); // Purple
        } else {
            btnScreenShare.setText("🖥️ Chia sẻ Màn hình");
            btnScreenShare.setBackground(new Color(2, 132, 199)); // Blue
        }
    }

    private void updateDurationAndStats() {
        if (!callConnected) return;

        long elapsedSec = (System.currentTimeMillis() - callStartTime) / 1000;
        long mins = elapsedSec / 60;
        long secs = elapsedSec % 60;
        timerLabel.setText(String.format("⏱ %02d:%02d", mins, secs));

        long kbps = (bytesSent / 1024) / Math.max(1, elapsedSec);
        statsLabel.setText(String.format(
                "📶 UDP: 8892 | %d FPS | %d KB/s | Gửi: %d | Nhận: %d",
                currentFps, kbps, framesSent, framesReceived
        ));
    }

    // Call state handlers
    public void onCallAccepted() {
        callConnected = true;
        callStartTime = System.currentTimeMillis();
        statusLabel.setText("🟢 Đang phát luồng UDP Media");
        statusLabel.setForeground(new Color(52, 211, 153));
        durationTimer.start();
        sendRegisterPacket(); // Re-announce endpoint
    }

    public void onCallRejected(String reason) {
        statusLabel.setText("🔴 Cuộc gọi bị từ chối: " + reason);
        statusLabel.setForeground(new Color(239, 68, 68));
        new javax.swing.Timer(2500, e -> endCall()).start();
    }

    public void onCallEnded() {
        if (!inCall) return;
        inCall = false;
        statusLabel.setText("🔴 Cuộc gọi đã kết thúc.");
        statusLabel.setForeground(new Color(239, 68, 68));
        new javax.swing.Timer(1500, e -> cleanupAndClose()).start();
    }

    public void endCall() {
        if (!inCall) return;
        inCall = false;

        // Notify other user via TCP
        if (client != null && client.isConnected()) {
            client.send(ChatMessage.callEnd(client.getCurrentUser(), targetUsername));
        }

        // Send UDP END packet
        if (udpSocket != null && !udpSocket.isClosed()) {
            try {
                MediaPacket endPkt = new MediaPacket(
                        MediaPacket.TYPE_END_STREAM,
                        client.getCurrentUser(),
                        targetUsername,
                        0,
                        new byte[0]
                );
                byte[] d = endPkt.encode();
                udpSocket.send(new DatagramPacket(d, d.length, InetAddress.getByName(serverHost), UdpMediaStreamer.VIDEO_STREAM_PORT));
            } catch (Exception ignored) {}
        }

        cleanupAndClose();
    }

    private void cleanupAndClose() {
        inCall = false;
        if (durationTimer != null && durationTimer.isRunning()) {
            durationTimer.stop();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        dispose();
    }
}

