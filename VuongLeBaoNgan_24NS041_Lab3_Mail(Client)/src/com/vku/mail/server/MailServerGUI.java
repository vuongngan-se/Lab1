package com.vku.mail.server;

import com.vku.mail.model.MailAccount;
import com.vku.mail.model.MailLog;
import com.vku.mail.model.MailMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MailServerGUI extends JFrame {
    private final MailServer server;
    private JTextArea logArea;
    private DefaultTableModel accountsModel;
    private DefaultTableModel mailsModel;
    private DefaultTableModel logsModel;

    public MailServerGUI(MailServer server) {
        super("VKU Mail Server - Quản Trị SMTP (2525) & POP3 (1100)");
        this.server = server;
        this.server.setGui(this);
        initUI();
        refreshData();
    }

    private void initUI() {
        setSize(960, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(220, 38, 38));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("  HỆ THỐNG MÁY CHỦ THƯ ĐIỆN TỬ (SMTP: 2525 | POP3: 1100)");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        // Detect local IP for multi-machine testing
        String serverIp = "127.0.0.1";
        try {
            serverIp = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {}

        JLabel badge = new JLabel("IP Máy Chủ: " + serverIp + " | Trạng thái: Đang hoạt động  ");
        badge.setForeground(new Color(254, 226, 226));
        badge.setFont(new Font("Segoe UI", Font.BOLD, 13));

        header.add(title, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);
        mainPanel.add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Tab 1: Live Protocol Logs
        JPanel logPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(15, 23, 42));
        logArea.setForeground(new Color(241, 245, 249));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel logBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClear = new JButton("Xóa Nhật Ký");
        btnClear.addActionListener(e -> logArea.setText(""));
        logBtns.add(btnClear);
        logPanel.add(logBtns, BorderLayout.SOUTH);
        tabbedPane.addTab("  Nhật Ký Giao Thức (SMTP / POP3)  ", logPanel);

        // Tab 2: Mail Messages in DB
        JPanel mailsPanel = new JPanel(new BorderLayout(6, 6));
        mailsModel = new DefaultTableModel(new String[]{"ID Thư", "Thời Gian", "Người Gửi", "Người Nhận", "Tiêu Đề", "Nội Dung Tóm Tắt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable mailsTable = new JTable(mailsModel);
        mailsTable.setRowHeight(24);
        mailsPanel.add(new JScrollPane(mailsTable), BorderLayout.CENTER);

        JPanel mailBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefreshMails = new JButton("Làm Mới Hòm Thư");
        btnRefreshMails.addActionListener(e -> refreshData());
        mailBtns.add(btnRefreshMails);
        mailsPanel.add(mailBtns, BorderLayout.SOUTH);
        tabbedPane.addTab("  Dữ Liệu Thư Trong Database  ", mailsPanel);

        // Tab 3: Accounts
        JPanel accPanel = new JPanel(new BorderLayout(6, 6));
        accountsModel = new DefaultTableModel(new String[]{"Tài Khoản", "Mật Khẩu", "Địa Chỉ Email", "Họ Tên"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable accTable = new JTable(accountsModel);
        accTable.setRowHeight(24);
        accPanel.add(new JScrollPane(accTable), BorderLayout.CENTER);

        JPanel accBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefreshAcc = new JButton("Làm Mới Danh Sách");
        btnRefreshAcc.addActionListener(e -> refreshData());
        accBtns.add(btnRefreshAcc);
        accPanel.add(accBtns, BorderLayout.SOUTH);
        tabbedPane.addTab("  Tài Khoản Hộp Thư  ", accPanel);

        // Tab 4: Database Logs
        JPanel dbLogsPanel = new JPanel(new BorderLayout(6, 6));
        logsModel = new DefaultTableModel(new String[]{"Thời Gian", "Giao Thức", "Client IP", "Chi Tiết Thao Tác", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable logsTable = new JTable(logsModel);
        logsTable.setRowHeight(22);
        dbLogsPanel.add(new JScrollPane(logsTable), BorderLayout.CENTER);

        JPanel dbLogBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefreshLogs = new JButton("Làm Mới Logs");
        btnRefreshLogs.addActionListener(e -> refreshData());
        dbLogBtns.add(btnRefreshLogs);
        dbLogsPanel.add(dbLogBtns, BorderLayout.SOUTH);
        tabbedPane.addTab("  Kiểm Toán Database Logs  ", dbLogsPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
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

    public void refreshData() {
        SwingUtilities.invokeLater(() -> {
            // Refresh mails
            mailsModel.setRowCount(0);
            List<MailMessage> msgs = server.getDatabase().getAllMessages();
            for (int i = msgs.size() - 1; i >= 0; i--) {
                MailMessage m = msgs.get(i);
                String preview = m.getBody() != null ? m.getBody().replace("\n", " ") : "";
                if (preview.length() > 60) preview = preview.substring(0, 60) + "...";
                mailsModel.addRow(new Object[]{
                        m.getId(), m.getFormattedDate(), m.getSender(), m.getRecipient(), m.getSubject(), preview
                });
            }

            // Refresh accounts
            accountsModel.setRowCount(0);
            List<MailAccount> accs = server.getDatabase().getAllAccounts();
            for (MailAccount a : accs) {
                accountsModel.addRow(new Object[]{
                        a.getUsername(), a.getPassword(), a.getEmail(), a.getFullName()
                });
            }

            // Refresh logs
            logsModel.setRowCount(0);
            List<MailLog> logs = server.getDatabase().getAllLogs();
            for (int i = logs.size() - 1; i >= 0; i--) {
                MailLog l = logs.get(i);
                logsModel.addRow(new Object[]{
                        l.getFormattedDate(), l.getProtocol(), l.getClientIp(), l.getDetails(), l.getStatus()
                });
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MailServer server = new MailServer();
            MailServerGUI gui = new MailServerGUI(server);
            gui.setVisible(true);
            server.start();
        });
    }
}
