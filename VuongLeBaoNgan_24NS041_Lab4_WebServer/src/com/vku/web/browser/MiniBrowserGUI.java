package com.vku.web.browser;

import com.vku.web.common.HistoryItem;
import com.vku.web.common.HttpResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MiniBrowserGUI extends JFrame {
    private final List<HistoryItem> historyList = new ArrayList<>();
    private final Stack<String> backStack = new Stack<>();
    private final Stack<String> forwardStack = new Stack<>();
    private String currentUrl = "http://localhost:8080/";

    // Top Navigation UI
    private JButton btnBack;
    private JButton btnForward;
    private JButton btnReload;
    private JButton btnHome;
    private JTextField urlField;
    private JComboBox<String> methodBox;
    private JButton btnGo;

    // View Components
    private JTabbedPane tabbedPane;
    private JEditorPane renderPane;
    private JLabel statusBadge;
    private DefaultTableModel headersTableModel;
    private JTextArea rawResponseArea;

    // POST Panel components
    private JTextArea postBodyArea;
    private JTextArea postResponseArea;

    public MiniBrowserGUI() {
        super("VKU Mini Browser (Tương Tự Chrome / Cốc Cốc) - LTM");
        initUI();
        navigate("http://localhost:8080/", "GET", null, false);
    }

    private void initUI() {
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));

        // Top Navigation Bar (Chrome style)
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(createNavBar(), BorderLayout.NORTH);
        topContainer.add(createBookmarksBar(), BorderLayout.SOUTH);
        mainPanel.add(topContainer, BorderLayout.NORTH);

        // Center Views (Render, Headers/Raw, POST Tester)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Tab 1: Render View
        renderPane = new JEditorPane();
        renderPane.setEditable(false);
        renderPane.setContentType("text/html");
        renderPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String link = e.getDescription();
                if (!link.startsWith("http://") && !link.startsWith("https://")) {
                    if (link.startsWith("/")) {
                        link = "http://localhost:8080" + link;
                    } else {
                        link = "http://localhost:8080/" + link;
                    }
                }
                navigate(link, "GET", null, true);
            }
        });
        tabbedPane.addTab("  🌐 Trang Web (Render View)  ", new JScrollPane(renderPane));

        // Tab 2: Headers & Raw
        tabbedPane.addTab("  📋 Headers & Mã Phản Hồi (Inspector)  ", createHeadersPane());

        // Tab 3: POST Sender
        tabbedPane.addTab("  📮 Gửi Dữ Liệu HTTP POST  ", createPostPane());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createNavBar() {
        JPanel nav = new JPanel(new BorderLayout(6, 0));
        nav.setBackground(new Color(241, 243, 244));
        nav.setBorder(new EmptyBorder(8, 10, 6, 10));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftBtns.setOpaque(false);

        btnBack = new JButton("←");
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnBack.setEnabled(false);
        btnBack.addActionListener(e -> goBack());

        btnForward = new JButton("→");
        btnForward.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnForward.setEnabled(false);
        btnForward.addActionListener(e -> goForward());

        btnReload = new JButton("🔄");
        btnReload.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnReload.addActionListener(e -> navigate(currentUrl, (String) methodBox.getSelectedItem(), null, false));

        btnHome = new JButton("🏠");
        btnHome.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnHome.addActionListener(e -> navigate("http://localhost:8080/", "GET", null, true));

        leftBtns.add(btnBack);
        leftBtns.add(btnForward);
        leftBtns.add(btnReload);
        leftBtns.add(btnHome);

        // Center URL Bar
        JPanel centerBar = new JPanel(new BorderLayout(4, 0));
        centerBar.setOpaque(false);

        methodBox = new JComboBox<>(new String[]{"GET", "HEAD", "POST"});
        methodBox.setFont(new Font("Segoe UI", Font.BOLD, 12));

        urlField = new JTextField("http://localhost:8080/");
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        urlField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigate(urlField.getText().trim(), (String) methodBox.getSelectedItem(), null, true);
                }
            }
        });

        btnGo = new JButton(" Đi Đến (Go) ");
        btnGo.setBackground(new Color(26, 115, 232));
        btnGo.setForeground(Color.WHITE);
        btnGo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGo.addActionListener(e -> navigate(urlField.getText().trim(), (String) methodBox.getSelectedItem(), null, true));

        centerBar.add(methodBox, BorderLayout.WEST);
        centerBar.add(urlField, BorderLayout.CENTER);
        centerBar.add(btnGo, BorderLayout.EAST);

        // Right Tools (Download, History)
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightBtns.setOpaque(false);

        JButton btnDownload = new JButton("📥 Tải Tệp Này (Download)");
        btnDownload.addActionListener(e -> openDownloadManager(urlField.getText().trim()));

        JButton btnHistory = new JButton("🕒 Lịch Sử (History)");
        btnHistory.addActionListener(e -> showHistoryDialog());

        rightBtns.add(btnDownload);
        rightBtns.add(btnHistory);

        nav.add(leftBtns, BorderLayout.WEST);
        nav.add(centerBar, BorderLayout.CENTER);
        nav.add(rightBtns, BorderLayout.EAST);
        return nav;
    }

    private JPanel createBookmarksBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setBackground(new Color(248, 249, 250));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 220, 224)));

        JLabel bmLabel = new JLabel("  Dịch Vụ Máy Chủ: ");
        bmLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        bmLabel.setForeground(Color.GRAY);
        bar.add(bmLabel);

        bar.add(createBookmarkButton("🏠 Trang Chủ", "http://localhost:8080/", "GET"));
        bar.add(createBookmarkButton("📄 Tài Liệu PDF", "http://localhost:8080/sample.pdf", "GET"));
        bar.add(createBookmarkButton("🎵 Nhạc MP3", "http://localhost:8080/sample.mp3", "GET"));
        bar.add(createBookmarkButton("🎬 Video MP4", "http://localhost:8080/sample.mp4", "GET"));
        bar.add(createBookmarkButton("⚡ REST API", "http://localhost:8080/api/stats", "GET"));
        bar.add(createBookmarkButton("🔍 Kiểm Tra HEAD", "http://localhost:8080/sample.mp4", "HEAD"));

        return bar;
    }

    private JButton createBookmarkButton(String title, String url, String method) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.addActionListener(e -> {
            methodBox.setSelectedItem(method);
            navigate(url, method, null, true);
        });
        return btn;
    }

    private JPanel createHeadersPane() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        statusBadge = new JLabel("Trạng thái: Chưa có yêu cầu nào");
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(statusBadge, BorderLayout.NORTH);

        headersTableModel = new DefaultTableModel(new String[]{"Tên Header", "Giá Trị Header"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(headersTableModel);
        table.setRowHeight(22);
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(0, 200));
        scrollTable.setBorder(BorderFactory.createTitledBorder("HTTP Response Headers"));

        rawResponseArea = new JTextArea();
        rawResponseArea.setEditable(false);
        rawResponseArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollRaw = new JScrollPane(rawResponseArea);
        scrollRaw.setBorder(BorderFactory.createTitledBorder("Nội Dung Dữ Liệu Thô (Raw Body)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTable, scrollRaw);
        split.setDividerLocation(200);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPostPane() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("Nhập dữ liệu POST (Form Data hoặc JSON):");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        top.add(lbl, BorderLayout.WEST);

        JButton btnSubmitPost = new JButton(" 🚀 Gửi Yêu Cầu POST ");
        btnSubmitPost.setBackground(new Color(16, 185, 129));
        btnSubmitPost.setForeground(Color.WHITE);
        btnSubmitPost.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSubmitPost.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.endsWith("/")) url += "api/echo";
            String body = postBodyArea.getText();
            methodBox.setSelectedItem("POST");
            navigate(url, "POST", body, true);
        });
        top.add(btnSubmitPost, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        postBodyArea = new JTextArea("name=NguyenVanA&mssv=24NS001&monhoc=LTM");
        postBodyArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane postScroll = new JScrollPane(postBodyArea);
        postScroll.setBorder(BorderFactory.createTitledBorder("Request Body"));

        postResponseArea = new JTextArea();
        postResponseArea.setEditable(false);
        postResponseArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane respScroll = new JScrollPane(postResponseArea);
        respScroll.setBorder(BorderFactory.createTitledBorder("Phản Hồi Từ Máy Chủ (POST Response)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, postScroll, respScroll);
        split.setDividerLocation(180);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    public void navigate(String urlStr, String method, String body, boolean addToBack) {
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            urlStr = "http://" + urlStr;
        }
        urlField.setText(urlStr);

        if (addToBack && currentUrl != null && !currentUrl.equalsIgnoreCase(urlStr)) {
            backStack.push(currentUrl);
            forwardStack.clear();
            updateNavButtons();
        }
        this.currentUrl = urlStr;

        String finalUrl = urlStr;
        new Thread(() -> {
            try {
                HttpResponse resp = HttpEngine.execute(method, finalUrl, body);

                // Add to history
                historyList.add(new HistoryItem(finalUrl, method, resp.getStatusCode()));

                SwingUtilities.invokeLater(() -> {
                    // Update Status Badge
                    statusBadge.setText(String.format("Mã Trạng Thái: %d %s (Phương thức: %s)",
                            resp.getStatusCode(), resp.getStatusMessage(), method));
                    if (resp.getStatusCode() == 200 || resp.getStatusCode() == 206) {
                        statusBadge.setForeground(new Color(5, 150, 105));
                    } else {
                        statusBadge.setForeground(new Color(220, 38, 38));
                    }

                    // Update Headers Table
                    headersTableModel.setRowCount(0);
                    for (var entry : resp.getHeaders().entrySet()) {
                        headersTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                    }

                    // Update Raw Response
                    String bodyStr = resp.getBodyAsString();
                    rawResponseArea.setText(bodyStr);
                    if ("POST".equalsIgnoreCase(method)) {
                        postResponseArea.setText(bodyStr);
                        tabbedPane.setSelectedIndex(2); // Switch to POST tab
                    }

                    // Update Render View
                    String mime = resp.getHeader("content-type");
                    if (mime != null && mime.toLowerCase().contains("text/html")) {
                        renderPane.setContentType("text/html");
                        renderPane.setText(bodyStr);
                        renderPane.setCaretPosition(0);
                        if (!"POST".equalsIgnoreCase(method)) tabbedPane.setSelectedIndex(0);
                    } else if (mime != null && (mime.contains("audio") || mime.contains("video") || mime.contains("pdf"))) {
                        String notice = "<html><body style='font-family:Segoe UI,sans-serif;text-align:center;padding:40px;'>"
                                + "<h2>📁 Tệp Đa Phương Tiện Nhị Phân: " + mime + "</h2>"
                                + "<p>Dung lượng: " + (resp.getHeader("content-length") != null ? (Long.parseLong(resp.getHeader("content-length")) / 1024) + " KB" : "Không xác định") + "</p>"
                                + "<p>Đây là tệp nhị phân (.pdf / .mp3 / .mp4), vui lòng bấm nút <b>'Tải Tệp Này (Download)'</b> ở góc trên bên phải để lưu về máy!</p>"
                                + "</body></html>";
                        renderPane.setContentType("text/html");
                        renderPane.setText(notice);
                        if (!"POST".equalsIgnoreCase(method)) tabbedPane.setSelectedIndex(0);
                    } else {
                        renderPane.setContentType("text/plain");
                        renderPane.setText(bodyStr);
                        if (!"POST".equalsIgnoreCase(method)) tabbedPane.setSelectedIndex(0);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusBadge.setText("Lỗi kết nối: " + e.getMessage());
                    statusBadge.setForeground(Color.RED);
                    renderPane.setContentType("text/html");
                    renderPane.setText("<html><body style='font-family:Segoe UI,sans-serif;color:red;padding:20px;'>"
                            + "<h3>Không thể kết nối tới máy chủ HTTP</h3>"
                            + "<p>" + e.getMessage() + "</p>"
                            + "</body></html>");
                });
            }
        }).start();
    }

    private void goBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(currentUrl);
            String prev = backStack.pop();
            updateNavButtons();
            navigate(prev, "GET", null, false);
        }
    }

    private void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(currentUrl);
            String next = forwardStack.pop();
            updateNavButtons();
            navigate(next, "GET", null, false);
        }
    }

    private void updateNavButtons() {
        btnBack.setEnabled(!backStack.isEmpty());
        btnForward.setEnabled(!forwardStack.isEmpty());
    }

    private void openDownloadManager(String url) {
        String filename = "downloaded_file";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            filename = url.substring(lastSlash + 1);
            if (filename.contains("?")) filename = filename.substring(0, filename.indexOf("?"));
        }
        if (filename.isEmpty()) filename = "download.html";

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(filename));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            DownloadManagerDialog dialog = new DownloadManagerDialog(this, url, target);
            dialog.setVisible(true);
        }
    }

    private void showHistoryDialog() {
        HistoryDialog dialog = new HistoryDialog(this, historyList, url -> navigate(url, "GET", null, true));
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MiniBrowserGUI browser = new MiniBrowserGUI();
            browser.setVisible(true);
        });
    }
}
