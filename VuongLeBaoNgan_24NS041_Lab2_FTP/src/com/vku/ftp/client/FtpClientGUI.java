package com.vku.ftp.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FtpClientGUI extends JFrame {
    private final FtpClient client;
    private File currentLocalDir;

    // Top Connection components
    private JTextField hostField;
    private JTextField portField;
    private JTextField userField;
    private JPasswordField passField;
    private JButton btnConnect;
    private JButton btnDisconnect;

    // Left (Local) components
    private JLabel localPathLabel;
    private DefaultTableModel localTableModel;
    private JTable localTable;

    // Right (Remote) components
    private JLabel remotePathLabel;
    private DefaultTableModel remoteTableModel;
    private JTable remoteTable;

    // Bottom Log
    private JTextArea logArea;

    public FtpClientGUI() {
        super("VKU FTP Client (Tương Tự FileZilla) - LTM");
        this.client = new FtpClient();
        this.currentLocalDir = new File(System.getProperty("user.dir"));
        initUI();
        refreshLocalFiles();
    }

    private void initUI() {
        setSize(1050, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(6, 6));
        mainPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Top Toolbar (FileZilla Quickconnect)
        JPanel topBar = createTopBar();
        mainPanel.add(topBar, BorderLayout.NORTH);

        // Center SplitPane: Local vs Remote
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLocalPane(), createRemotePane());
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(500);

        // Bottom: Log Console
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setPreferredSize(new Dimension(0, 160));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Nhật Ký Lệnh FTP (Command Log)"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(248, 250, 252));
        bottomPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        client.setLogListener(msg -> SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, bottomPanel);
        verticalSplit.setResizeWeight(0.75);
        verticalSplit.setDividerLocation(460);

        mainPanel.add(verticalSplit, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(new Color(241, 245, 249));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(203, 213, 225)));

        bar.add(new JLabel("Host:"));
        hostField = new JTextField("127.0.0.1", 9);
        bar.add(hostField);

        bar.add(new JLabel("Port:"));
        portField = new JTextField("2121", 4);
        bar.add(portField);

        bar.add(new JLabel("User:"));
        userField = new JTextField("admin", 7);
        bar.add(userField);

        bar.add(new JLabel("Pass:"));
        passField = new JPasswordField("admin123", 7);
        bar.add(passField);

        btnConnect = new JButton("Kết Nối Nhanh");
        btnConnect.setBackground(new Color(37, 99, 235));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConnect.addActionListener(e -> performConnect());
        bar.add(btnConnect);

        btnDisconnect = new JButton("Ngắt Kết Nối");
        btnDisconnect.setEnabled(false);
        btnDisconnect.addActionListener(e -> performDisconnect());
        bar.add(btnDisconnect);

        return bar;
    }

    private JPanel createLocalPane() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Máy Cục Bộ (Local Site)"));

        JPanel pathRow = new JPanel(new BorderLayout(4, 0));
        localPathLabel = new JLabel(" " + currentLocalDir.getAbsolutePath());
        localPathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JButton btnLocalUp = new JButton("↑ Lên Thư Mục Cha");
        btnLocalUp.addActionListener(e -> {
            File p = currentLocalDir.getParentFile();
            if (p != null && p.exists()) {
                currentLocalDir = p;
                refreshLocalFiles();
            }
        });
        pathRow.add(localPathLabel, BorderLayout.CENTER);
        pathRow.add(btnLocalUp, BorderLayout.EAST);
        panel.add(pathRow, BorderLayout.NORTH);

        localTableModel = new DefaultTableModel(new String[]{"Tên Tệp", "Kích Thước", "Ngày Sửa"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        localTable = new JTable(localTableModel);
        localTable.setRowHeight(22);
        localTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = localTable.getSelectedRow();
                    if (row >= 0) {
                        String name = (String) localTableModel.getValueAt(row, 0);
                        File target = new File(currentLocalDir, name);
                        if (target.isDirectory()) {
                            currentLocalDir = target;
                            refreshLocalFiles();
                        }
                    }
                }
            }
        });
        panel.add(new JScrollPane(localTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnUpload = new JButton("⬆ Tải Lên Máy Chủ (Upload)");
        btnUpload.setBackground(new Color(16, 185, 129));
        btnUpload.setForeground(Color.WHITE);
        btnUpload.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpload.addActionListener(e -> performUpload());
        btnRow.add(btnUpload);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRemotePane() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Máy Chủ FTP Từ Xa (Remote Site)"));

        JPanel pathRow = new JPanel(new BorderLayout(4, 0));
        remotePathLabel = new JLabel(" /");
        remotePathLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JButton btnRemoteUp = new JButton("↑ Thư Mục Cha");
        btnRemoteUp.addActionListener(e -> {
            if (client.isConnected()) {
                try {
                    client.changeDirectory("..");
                    refreshRemoteFiles();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi chuyển thư mục: " + ex.getMessage());
                }
            }
        });
        pathRow.add(remotePathLabel, BorderLayout.CENTER);
        pathRow.add(btnRemoteUp, BorderLayout.EAST);
        panel.add(pathRow, BorderLayout.NORTH);

        remoteTableModel = new DefaultTableModel(new String[]{"Tên Tệp / Thư Mục", "Kích Thước", "Loại", "Ngày"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        remoteTable = new JTable(remoteTableModel);
        remoteTable.setRowHeight(22);
        remoteTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = remoteTable.getSelectedRow();
                    if (row >= 0) {
                        String name = (String) remoteTableModel.getValueAt(row, 0);
                        String type = (String) remoteTableModel.getValueAt(row, 2);
                        if ("Thư Mục".equals(type) && client.isConnected()) {
                            try {
                                client.changeDirectory(name);
                                refreshRemoteFiles();
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(FtpClientGUI.this, "Lỗi: " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        });
        panel.add(new JScrollPane(remoteTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton btnDownload = new JButton("⬇ Tải Về Máy (Download)");
        btnDownload.setBackground(new Color(37, 99, 235));
        btnDownload.setForeground(Color.WHITE);
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDownload.addActionListener(e -> performDownload());

        JButton btnDelete = new JButton("Xóa Tệp (Delete)");
        btnDelete.addActionListener(e -> performDelete());

        JButton btnNewFolder = new JButton("Tạo Thư Mục (MKD)");
        btnNewFolder.addActionListener(e -> performMakeDir());

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.addActionListener(e -> refreshRemoteFiles());

        btnRow.add(btnDownload);
        btnRow.add(btnDelete);
        btnRow.add(btnNewFolder);
        btnRow.add(btnRefresh);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private void performConnect() {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        new Thread(() -> {
            boolean ok = client.connect(host, port, user, pass);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    btnConnect.setEnabled(false);
                    btnDisconnect.setEnabled(true);
                    refreshRemoteFiles();
                    JOptionPane.showMessageDialog(this, "Kết nối FTP Server thành công!");
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể kết nối tới FTP Server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void performDisconnect() {
        client.disconnect();
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        remoteTableModel.setRowCount(0);
        remotePathLabel.setText(" / (Chưa kết nối)");
    }

    private void refreshLocalFiles() {
        localPathLabel.setText(" " + currentLocalDir.getAbsolutePath());
        localTableModel.setRowCount(0);
        File[] files = currentLocalDir.listFiles();
        if (files != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (File f : files) {
                localTableModel.addRow(new Object[]{
                        f.getName(),
                        f.isDirectory() ? "<DIR>" : (f.length() / 1024) + " KB",
                        sdf.format(new Date(f.lastModified()))
                });
            }
        }
    }

    private void refreshRemoteFiles() {
        if (!client.isConnected()) return;
        new Thread(() -> {
            try {
                List<FtpClient.FtpItem> items = client.listFiles();
                SwingUtilities.invokeLater(() -> {
                    remotePathLabel.setText(" " + client.getCurrentRemoteDir());
                    remoteTableModel.setRowCount(0);
                    for (FtpClient.FtpItem item : items) {
                        remoteTableModel.addRow(new Object[]{
                                item.name,
                                item.isDirectory ? "<DIR>" : (item.size / 1024) + " KB",
                                item.isDirectory ? "Thư Mục" : "Tệp tin",
                                item.date
                        });
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi lấy danh sách tệp: " + e.getMessage()));
            }
        }).start();
    }

    private void performUpload() {
        if (!client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Vui lòng kết nối FTP Server trước!");
            return;
        }
        int row = localTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tệp trên máy cục bộ để tải lên!");
            return;
        }
        String fileName = (String) localTableModel.getValueAt(row, 0);
        File localFile = new File(currentLocalDir, fileName);
        if (localFile.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ tải lên tệp tin (không phải thư mục)!");
            return;
        }

        new Thread(() -> {
            try {
                boolean ok = client.uploadFile(localFile, fileName);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Tải lên thành công: " + fileName);
                        refreshRemoteFiles();
                    } else {
                        JOptionPane.showMessageDialog(this, "Tải lên thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    private void performDownload() {
        if (!client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Vui lòng kết nối FTP Server trước!");
            return;
        }
        int row = remoteTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tệp từ máy chủ từ xa để tải về!");
            return;
        }
        String fileName = (String) remoteTableModel.getValueAt(row, 0);
        String type = (String) remoteTableModel.getValueAt(row, 2);
        if ("Thư Mục".equals(type)) {
            JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ tải về tệp tin (không phải thư mục)!");
            return;
        }

        File destFile = new File(currentLocalDir, fileName);
        new Thread(() -> {
            try {
                boolean ok = client.downloadFile(fileName, destFile);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Tải về thành công vào thư mục cục bộ:\n" + destFile.getAbsolutePath());
                        refreshLocalFiles();
                    } else {
                        JOptionPane.showMessageDialog(this, "Tải về thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    private void performDelete() {
        if (!client.isConnected()) return;
        int row = remoteTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn tệp cần xóa trên máy chủ!");
            return;
        }
        String fileName = (String) remoteTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa tệp: " + fileName + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    boolean ok = client.deleteFile(fileName);
                    SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            JOptionPane.showMessageDialog(this, "Đã xóa tệp: " + fileName);
                            refreshRemoteFiles();
                        } else {
                            JOptionPane.showMessageDialog(this, "Xóa thất bại (Có thể tài khoản không có quyền xóa)!");
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
                }
            }).start();
        }
    }

    private void performMakeDir() {
        if (!client.isConnected()) return;
        String folderName = JOptionPane.showInputDialog(this, "Nhập tên thư mục mới cần tạo trên máy chủ:");
        if (folderName != null && !folderName.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    boolean ok = client.createDirectory(folderName.trim());
                    SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            JOptionPane.showMessageDialog(this, "Đã tạo thư mục thành công: " + folderName);
                            refreshRemoteFiles();
                        } else {
                            JOptionPane.showMessageDialog(this, "Không thể tạo thư mục!");
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
                }
            }).start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            FtpClientGUI gui = new FtpClientGUI();
            gui.setVisible(true);
        });
    }
}
