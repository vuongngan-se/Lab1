package com.vku.web.server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class WebServerGUI extends JFrame {
    private final HttpWebServer server;
    private JLabel statusLabel;
    private JLabel requestsCountLabel;
    private JTextArea logArea;

    public WebServerGUI(HttpWebServer server) {
        super("VKU Web Server Manager - Lập Trình Mạng (Port 8080)");
        this.server = server;
        this.server.setGui(this);
        initUI();
    }

    private void initUI() {
        setSize(880, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(2, 132, 199));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        String dbStatus = server.getDatabase().isUsingMySQL() ? "MySQL: db_web" : "File Storage (.dat)";
        statusLabel = new JLabel("  MÁY CHỦ WEB (Port " + server.getPort() + ") | DB: [" + dbStatus + "]");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        requestsCountLabel = new JLabel("Tổng số yêu cầu: 0  ");
        requestsCountLabel.setForeground(Color.WHITE);
        requestsCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        header.add(statusLabel, BorderLayout.WEST);
        header.add(requestsCountLabel, BorderLayout.EAST);
        mainPanel.add(header, BorderLayout.NORTH);

        // Center: Log + Services Panel
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(15, 23, 42));
        logArea.setForeground(new Color(241, 245, 249));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Nhật Ký Yêu Cầu Web (HTTP Request Logs)"));
        centerPanel.add(scrollLog, BorderLayout.CENTER);

        // Right side: Services list
        JPanel servicesPanel = new JPanel(new GridLayout(6, 1, 6, 6));
        servicesPanel.setPreferredSize(new Dimension(240, 0));
        servicesPanel.setBorder(BorderFactory.createTitledBorder("Dịch Vụ Phục Vụ"));

        servicesPanel.add(new JLabel("🌐 HTML Portal (/index.html)"));
        servicesPanel.add(new JLabel("📄 Tài liệu PDF (/sample.pdf)"));
        servicesPanel.add(new JLabel("🎵 Nhạc MP3 (/sample.mp3)"));
        servicesPanel.add(new JLabel("🎬 Video MP4 (/sample.mp4)"));
        servicesPanel.add(new JLabel("⚡ REST API (/api/stats, /api/posts)"));
        servicesPanel.add(new JLabel("🗄️ CSDL: " + (server.getDatabase().isUsingMySQL() ? "MySQL (db_web)" : "File Storage (.dat)")));

        centerPanel.add(servicesPanel, BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOpenBrowser = new JButton("Mở Trình Duyệt Ngoài (Chrome/Edge)");
        btnOpenBrowser.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + server.getPort() + "/"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi mở trình duyệt: " + ex.getMessage());
            }
        });
        JButton btnOpenFolder = new JButton("Mở Thư Mục webroot");
        btnOpenFolder.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(server.getWebRoot());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi mở thư mục: " + ex.getMessage());
            }
        });
        JButton btnClear = new JButton("Xóa Nhật Ký");
        btnClear.addActionListener(e -> logArea.setText(""));

        bottom.add(btnOpenBrowser);
        bottom.add(btnOpenFolder);
        bottom.add(btnClear);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        add(mainPanel);
    }

    public void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    public void updateRequestCount(long count) {
        SwingUtilities.invokeLater(() -> {
            if (requestsCountLabel != null) {
                requestsCountLabel.setText("Tổng số yêu cầu: " + count + "  ");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            HttpWebServer server = new HttpWebServer();
            WebServerGUI gui = new WebServerGUI(server);
            gui.setVisible(true);
            server.start();
        });
    }
}
