package com.vku.mail.client;

import com.vku.mail.model.MailMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MailClientGUI extends JFrame {
    private final MailClient client;
    private final List<MailMessage> currentInboxList = new ArrayList<>();
    private MailMessage selectedMessage;

    // UI Components
    private JLabel currentAccountLabel;
    private JLabel folderTitleLabel;
    private DefaultTableModel mailTableModel;
    private JTable mailTable;

    // Reading Pane
    private JLabel msgSubjectLabel;
    private JLabel msgFromLabel;
    private JLabel msgDateLabel;
    private JTextArea msgBodyArea;
    private JButton btnSaveAttachment;

    // Console Log
    private JTextArea logArea;

    public MailClientGUI() {
        super("VKU Mail Client (Giao Diện Tương Tự Gmail) - LTM");
        this.client = new MailClient();
        this.client.configureLocal("alice", "123456", "alice@vku.udn.vn");
        initUI();
        updateAccountDisplay();
        refreshInbox();
    }

    private void initUI() {
        setSize(1080, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(new Color(246, 248, 252));

        // Left Sidebar
        JPanel sidebar = createSidebar();
        mainPanel.add(sidebar, BorderLayout.WEST);

        // Center Area (Mail List + Reading Pane)
        JPanel contentArea = createContentArea();
        mainPanel.add(contentArea, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(246, 248, 252));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(229, 231, 235)));

        // Top Logo + Compose button
        JPanel topSide = new JPanel(new BorderLayout(0, 12));
        topSide.setOpaque(false);
        topSide.setBorder(new EmptyBorder(16, 16, 0, 16));

        JLabel logo = new JLabel("  ✉ VKU Gmail", JLabel.LEFT);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(new Color(220, 38, 38));
        topSide.add(logo, BorderLayout.NORTH);

        JButton btnCompose = new JButton("  +  Soạn Thư Mới  ");
        btnCompose.setBackground(new Color(194, 231, 255));
        btnCompose.setForeground(new Color(0, 29, 53));
        btnCompose.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCompose.setFocusPainted(false);
        btnCompose.setPreferredSize(new Dimension(0, 48));
        btnCompose.addActionListener(e -> openComposeDialog());
        topSide.add(btnCompose, BorderLayout.CENTER);

        sidebar.add(topSide, BorderLayout.NORTH);

        // Navigation Folders
        JPanel navPanel = new JPanel(new GridLayout(4, 1, 0, 4));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JButton btnInbox = new JButton("📥  Hộp Thư Đến (Inbox)");
        btnInbox.setHorizontalAlignment(SwingConstants.LEFT);
        btnInbox.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnInbox.addActionListener(e -> refreshInbox());

        JButton btnConfig = new JButton("⚙  Đổi Tài Khoản / Gmail");
        btnConfig.setHorizontalAlignment(SwingConstants.LEFT);
        btnConfig.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnConfig.addActionListener(e -> showConfigDialog());

        JButton btnLogs = new JButton("📋  Nhật Ký Giao Thức");
        btnLogs.setHorizontalAlignment(SwingConstants.LEFT);
        btnLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnLogs.addActionListener(e -> showLogDialog());

        navPanel.add(btnInbox);
        navPanel.add(btnConfig);
        navPanel.add(btnLogs);

        sidebar.add(navPanel, BorderLayout.CENTER);

        // Current Account Status Badge at bottom
        JPanel bottomSide = new JPanel(new BorderLayout());
        bottomSide.setOpaque(false);
        bottomSide.setBorder(new EmptyBorder(12, 12, 16, 12));
        currentAccountLabel = new JLabel("<html><small>Đang đăng nhập:</small><br/><b>" + client.getEmailAddress() + "</b></html>");
        currentAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottomSide.add(currentAccountLabel, BorderLayout.CENTER);
        sidebar.add(bottomSide, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createContentArea() {
        JPanel area = new JPanel(new BorderLayout(0, 0));
        area.setBackground(Color.WHITE);

        // Top Toolbar
        JPanel topToolbar = new JPanel(new BorderLayout());
        topToolbar.setBackground(Color.WHITE);
        topToolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
                new EmptyBorder(12, 16, 12, 16)
        ));

        folderTitleLabel = new JLabel("Hộp Thư Đến (Inbox) - " + (client.isGmailMode() ? "Chế độ Gmail" : "Chế độ Localhost"));
        folderTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topToolbar.add(folderTitleLabel, BorderLayout.WEST);

        JButton btnRefresh = new JButton("🔄 Làm Mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> refreshInbox());
        topToolbar.add(btnRefresh, BorderLayout.EAST);

        area.add(topToolbar, BorderLayout.NORTH);

        // Center SplitPane: Mail Table (Top) vs Reading Pane (Bottom)
        mailTableModel = new DefaultTableModel(new String[]{"Người Gửi (From)", "Tiêu Đề (Subject)", "Đính Kèm", "Thời Gian"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        mailTable = new JTable(mailTableModel);
        mailTable.setRowHeight(28);
        mailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mailTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mailTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = mailTable.getSelectedRow();
                if (row >= 0 && row < currentInboxList.size()) {
                    displayMessage(currentInboxList.get(row));
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(mailTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        // Reading Pane
        JPanel readingPane = createReadingPane();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, readingPane);
        splitPane.setResizeWeight(0.45);
        splitPane.setDividerLocation(260);

        area.add(splitPane, BorderLayout.CENTER);
        return area;
    }

    private JPanel createReadingPane() {
        JPanel pane = new JPanel(new BorderLayout(8, 8));
        pane.setBackground(Color.WHITE);
        pane.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Message Headers
        JPanel headerPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        headerPanel.setOpaque(false);

        msgSubjectLabel = new JLabel("Chọn một email từ danh sách phía trên để xem nội dung");
        msgSubjectLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        msgFromLabel = new JLabel("");
        msgFromLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgFromLabel.setForeground(new Color(75, 85, 99));

        JPanel subRow = new JPanel(new BorderLayout());
        subRow.setOpaque(false);
        msgDateLabel = new JLabel("");
        msgDateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msgDateLabel.setForeground(Color.GRAY);

        btnSaveAttachment = new JButton("💾 Tải Tệp Đính Kèm");
        btnSaveAttachment.setVisible(false);
        btnSaveAttachment.addActionListener(e -> saveAttachment());

        subRow.add(msgDateLabel, BorderLayout.WEST);
        subRow.add(btnSaveAttachment, BorderLayout.EAST);

        headerPanel.add(msgSubjectLabel);
        headerPanel.add(msgFromLabel);
        headerPanel.add(subRow);

        pane.add(headerPanel, BorderLayout.NORTH);

        // Body area
        msgBodyArea = new JTextArea();
        msgBodyArea.setEditable(false);
        msgBodyArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgBodyArea.setLineWrap(true);
        msgBodyArea.setWrapStyleWord(true);
        msgBodyArea.setBackground(new Color(250, 250, 250));
        msgBodyArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235)),
                new EmptyBorder(8, 8, 8, 8)
        ));

        pane.add(new JScrollPane(msgBodyArea), BorderLayout.CENTER);
        return pane;
    }

    private void displayMessage(MailMessage msg) {
        this.selectedMessage = msg;
        msgSubjectLabel.setText(msg.getSubject() != null ? msg.getSubject() : "(Không có tiêu đề)");
        msgFromLabel.setText("Từ: " + msg.getSender() + "  ->  Đến: " + msg.getRecipient());
        msgDateLabel.setText(msg.getFormattedDate());
        msgBodyArea.setText(msg.getBody() != null ? msg.getBody() : "");

        if (msg.getAttachmentName() != null && !msg.getAttachmentName().isEmpty()) {
            btnSaveAttachment.setText("💾 Tải Tệp: " + msg.getAttachmentName());
            btnSaveAttachment.setVisible(true);
        } else {
            btnSaveAttachment.setVisible(false);
        }
    }

    private void saveAttachment() {
        if (selectedMessage == null || selectedMessage.getAttachmentName() == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(selectedMessage.getAttachmentName()));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                if (selectedMessage.getAttachmentData() != null) {
                    fos.write(selectedMessage.getAttachmentData());
                } else {
                    fos.write(("Nội dung tệp đính kèm: " + selectedMessage.getAttachmentName()).getBytes());
                }
                JOptionPane.showMessageDialog(this, "Đã lưu tệp đính kèm thành công!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu tệp: " + ex.getMessage());
            }
        }
    }

    private void refreshInbox() {
        new Thread(() -> {
            List<MailMessage> list = client.fetchInbox();
            SwingUtilities.invokeLater(() -> {
                currentInboxList.clear();
                currentInboxList.addAll(list);
                mailTableModel.setRowCount(0);
                for (MailMessage m : currentInboxList) {
                    mailTableModel.addRow(new Object[]{
                            m.getSender(),
                            m.getSubject(),
                            m.getAttachmentName() != null ? "📎 Có" : "—",
                            m.getFormattedDate()
                    });
                }
                if (currentInboxList.isEmpty()) {
                    msgSubjectLabel.setText("Hộp thư rỗng");
                    msgFromLabel.setText("");
                    msgBodyArea.setText("Không có thư nào trong hộp thư.");
                } else {
                    mailTable.setRowSelectionInterval(0, 0);
                }
            });
        }).start();
    }

    private void openComposeDialog() {
        ComposeDialog dialog = new ComposeDialog(this, client.getEmailAddress(), data -> {
            new Thread(() -> {
                boolean ok = client.sendMail(data.to, data.subject, data.body, data.attachName, data.attachData);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Đã gửi email thành công!");
                        refreshInbox();
                    } else {
                        JOptionPane.showMessageDialog(this, "Gửi email thất bại! Kiểm tra cài đặt máy chủ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        });
        dialog.setVisible(true);
    }

    private void updateAccountDisplay() {
        String serverDesc = client.isGmailMode() ? "Gmail Trực Tuyến" : ("Máy Chủ: " + client.getSmtpHost());
        currentAccountLabel.setText("<html><small>Đang đăng nhập:</small><br/><b>" + client.getEmailAddress() + "</b><br/><small style='color:#64748b'>(" + serverDesc + ")</small></html>");
        folderTitleLabel.setText("Hộp Thư Đến (Inbox) - " + (client.isGmailMode() ? "Chế độ Gmail" : ("Server: " + client.getSmtpHost())));
    }

    private void showConfigDialog() {
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        JComboBox<String> modeBox = new JComboBox<>(new String[]{
            "1. Máy Chủ Cục Bộ / LAN (SMTP:2525 & POP3:1100)",
            "2. Google Gmail Thật (smtp.gmail.com:465 & pop.gmail.com:995)"
        });
        JTextField txtHost = new JTextField(client.isGmailMode() ? "127.0.0.1" : client.getSmtpHost());
        JTextField txtEmail = new JTextField(client.getEmailAddress());
        JPasswordField txtPass = new JPasswordField("123456");

        form.add(new JLabel("Môi trường gửi/nhận:"));
        form.add(modeBox);
        form.add(new JLabel("Địa chỉ IP Máy Chủ (Server IP):"));
        form.add(txtHost);
        form.add(new JLabel("Email / Tên tài khoản:"));
        form.add(txtEmail);
        form.add(new JLabel("Mật khẩu / App Password:"));
        form.add(txtPass);

        if (client.isGmailMode()) {
            modeBox.setSelectedIndex(1);
            txtHost.setEnabled(false);
            txtHost.setText("smtp.gmail.com / pop.gmail.com");
        } else {
            modeBox.setSelectedIndex(0);
            txtHost.setEnabled(true);
        }

        modeBox.addActionListener(e -> {
            if (modeBox.getSelectedIndex() == 0) {
                txtHost.setEnabled(true);
                txtHost.setText(client.getSmtpHost().contains("gmail") ? "127.0.0.1" : client.getSmtpHost());
                txtEmail.setText("alice@vku.udn.vn");
                txtPass.setText("123456");
            } else {
                txtHost.setEnabled(false);
                txtHost.setText("smtp.gmail.com / pop.gmail.com");
                txtEmail.setText("your_email@gmail.com");
                txtPass.setText("");
            }
        });

        int res = JOptionPane.showConfirmDialog(this, form, "Cấu Hình Tài Khoản & Địa Chỉ Máy Chủ", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String email = txtEmail.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            if (modeBox.getSelectedIndex() == 0) {
                String host = txtHost.getText().trim();
                if (host.isEmpty()) host = "127.0.0.1";
                String u = email.contains("@") ? email.split("@")[0] : email;
                client.configureLocal(host, 2525, 1100, u, pass, email);
            } else {
                client.configureGmail(email, pass);
            }
            updateAccountDisplay();
            refreshInbox();
        }
    }

    private void showLogDialog() {
        JDialog dialog = new JDialog(this, "Nhật Ký Giao Thức (Protocol Logs)", false);
        dialog.setSize(680, 420);
        dialog.setLocationRelativeTo(this);
        if (logArea == null) {
            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
            client.setLogListener(msg -> SwingUtilities.invokeLater(() -> {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }));
        }
        dialog.add(new JScrollPane(logArea));
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MailClientGUI gui = new MailClientGUI();
            gui.setVisible(true);
        });
    }
}
