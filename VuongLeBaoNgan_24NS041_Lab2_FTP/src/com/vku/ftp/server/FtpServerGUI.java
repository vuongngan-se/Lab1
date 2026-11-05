package com.vku.ftp.server;

import com.vku.ftp.common.FtpLog;
import com.vku.ftp.common.FtpUser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

public class FtpServerGUI extends JFrame {
    private final FtpServer server;
    private JLabel statusLabel;
    private JLabel clientsLabel;
    private JTextArea logArea;
    private DefaultTableModel usersTableModel;
    private DefaultTableModel logsTableModel;

    public FtpServerGUI(FtpServer server) {
        super("VKU FTP Server Manager (RFC 959 + Database)");
        this.server = server;
        this.server.setGui(this);
        initUI();
        refreshData();
    }

    private void initUI() {
        setSize(920, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        String dbMode = server.getDatabase().isUsingMySQL() ? "MySQL: db_ftp (Port 3306)" : "File Storage (.dat)";
        statusLabel = new JLabel("  MÁY CHỦ FTP: Cổng " + server.getPort() + " | CSDL: " + dbMode);
        statusLabel.setForeground(new Color(52, 211, 153));
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        clientsLabel = new JLabel("Đang kết nối: 0 client  ");
        clientsLabel.setForeground(Color.WHITE);
        clientsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        header.add(statusLabel, BorderLayout.WEST);
        header.add(clientsLabel, BorderLayout.EAST);
        mainPanel.add(header, BorderLayout.NORTH);

        // Center Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Tab 1: Live Logs
        JPanel logPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(15, 23, 42));
        logArea.setForeground(new Color(226, 232, 240));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel logBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClearLog = new JButton("Xóa Nhật Ký");
        btnClearLog.addActionListener(e -> logArea.setText(""));
        JButton btnOpenFolder = new JButton("Mở Thư Mục ftp_root");
        btnOpenFolder.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File("ftp_root"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + ex.getMessage());
            }
        });
        logBtnRow.add(btnOpenFolder);
        logBtnRow.add(btnClearLog);
        logPanel.add(logBtnRow, BorderLayout.SOUTH);

        tabbedPane.addTab("  Nhật Ký Thời Gian Thực (Logs)  ", logPanel);

        // Tab 2: User Management in DB
        JPanel usersPanel = new JPanel(new BorderLayout(6, 6));
        usersTableModel = new DefaultTableModel(new String[]{"Tài Khoản", "Mật Khẩu", "Thư Mục Gốc", "Quyền Ghi", "Quyền Xóa"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable usersTable = new JTable(usersTableModel);
        usersTable.setRowHeight(24);
        usersPanel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JPanel userBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddUser = new JButton("Thêm Người Dùng");
        btnAddUser.addActionListener(e -> showAddUserDialog());
        JButton btnDeleteUser = new JButton("Xóa Người Dùng");
        btnDeleteUser.addActionListener(e -> {
            int selected = usersTable.getSelectedRow();
            if (selected >= 0) {
                String u = (String) usersTableModel.getValueAt(selected, 0);
                if ("admin".equalsIgnoreCase(u)) {
                    JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản admin mặc định!");
                    return;
                }
                server.getDatabase().deleteUser(u);
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng để xóa.");
            }
        });
        JButton btnRefreshUsers = new JButton("Làm Mới");
        btnRefreshUsers.addActionListener(e -> refreshData());
        userBtns.add(btnAddUser);
        userBtns.add(btnDeleteUser);
        userBtns.add(btnRefreshUsers);
        usersPanel.add(userBtns, BorderLayout.SOUTH);

        tabbedPane.addTab("  Quản Lý Tài Khoản (Database)  ", usersPanel);

        // Tab 3: Database Audit Logs
        JPanel dbLogsPanel = new JPanel(new BorderLayout(6, 6));
        logsTableModel = new DefaultTableModel(new String[]{"Thời Gian", "Client IP", "Người Dùng", "Hành Động", "Tệp/Đường Dẫn", "Trạng Thái", "Dung Lượng (Bytes)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable logsTable = new JTable(logsTableModel);
        logsTable.setRowHeight(22);
        dbLogsPanel.add(new JScrollPane(logsTable), BorderLayout.CENTER);

        JPanel dbLogBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefreshLogs = new JButton("Làm Mới Dữ Liệu DB");
        btnRefreshLogs.addActionListener(e -> refreshData());
        dbLogBtns.add(btnRefreshLogs);
        dbLogsPanel.add(dbLogBtns, BorderLayout.SOUTH);

        tabbedPane.addTab("  Lịch Sử Hoạt Động (Database Logs)  ", dbLogsPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void showAddUserDialog() {
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JTextField txtHome = new JTextField("ftp_root");
        JCheckBox chkWrite = new JCheckBox("Cho phép tải lên (Upload)", true);
        JCheckBox chkDelete = new JCheckBox("Cho phép xóa tệp (Delete)", false);

        form.add(new JLabel("Tên đăng nhập:"));
        form.add(txtUser);
        form.add(new JLabel("Mật khẩu:"));
        form.add(txtPass);
        form.add(new JLabel("Thư mục gốc:"));
        form.add(txtHome);
        form.add(new JLabel("Quyền hạn:"));
        form.add(chkWrite);
        form.add(new JLabel(""));
        form.add(chkDelete);

        int res = JOptionPane.showConfirmDialog(this, form, "Thêm Người Dùng FTP Mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            String h = txtHome.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập và mật khẩu không được rỗng!");
                return;
            }
            FtpUser newUser = new FtpUser(u, p, h.isEmpty() ? "ftp_root" : h, chkWrite.isSelected(), chkDelete.isSelected());
            server.getDatabase().addUser(newUser);
            refreshData();
            JOptionPane.showMessageDialog(this, "Đã thêm người dùng " + u + " vào cơ sở dữ liệu!");
        }
    }

    public void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    public void updateClientCount(int count) {
        SwingUtilities.invokeLater(() -> {
            if (clientsLabel != null) {
                clientsLabel.setText("Đang kết nối: " + count + " client  ");
            }
        });
    }

    public void refreshData() {
        SwingUtilities.invokeLater(() -> {
            // Update users table
            usersTableModel.setRowCount(0);
            List<FtpUser> users = server.getDatabase().getAllUsers();
            for (FtpUser u : users) {
                usersTableModel.addRow(new Object[]{
                        u.getUsername(),
                        u.getPassword(),
                        u.getHomeDirectory(),
                        u.isCanWrite() ? "✓ Có" : "✗ Không",
                        u.isCanDelete() ? "✓ Có" : "✗ Không"
                });
            }

            // Update logs table
            logsTableModel.setRowCount(0);
            List<FtpLog> logs = server.getDatabase().getAllLogs();
            for (int i = logs.size() - 1; i >= 0; i--) {
                FtpLog l = logs.get(i);
                logsTableModel.addRow(new Object[]{
                        l.getFormattedTime(),
                        l.getClientIp(),
                        l.getUsername(),
                        l.getAction(),
                        l.getTargetPath(),
                        l.getStatus(),
                        l.getBytesTransferred()
                });
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            FtpServer server = new FtpServer();
            FtpServerGUI gui = new FtpServerGUI(server);
            gui.setVisible(true);
            server.start();
        });
    }
}
