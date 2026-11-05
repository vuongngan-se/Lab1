package com.vku.rmi.client;

import com.vku.rmi.model.DataRecord;
import com.vku.rmi.service.IClientService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RmiClientGUI extends JFrame {
    private final FailoverManager failoverManager;
    private final List<DataRecord> recordList = new ArrayList<>();

    // UI
    private JLabel statusLabel;
    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtDept;
    private JTextField txtScore;
    private JTextField txtSearch;
    private DefaultTableModel tableModel;
    private JTable table;

    public RmiClientGUI() {
        this("127.0.0.1", 1099, "127.0.0.1", 1100);
    }

    public RmiClientGUI(String host1, int port1, String host2, int port2) {
        super("VKU RMI Client - Quản Trị Phân Tán Đa Máy Chủ (LTM)");
        this.failoverManager = new FailoverManager(host1, port1, host2, port2);
        initUI();
        refreshRecords();
    }

    private void initUI() {
        setSize(980, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Status & Failover Banner
        JPanel topBanner = new JPanel(new BorderLayout());
        topBanner.setBackground(new Color(15, 23, 42));
        topBanner.setBorder(new EmptyBorder(10, 14, 10, 14));

        statusLabel = new JLabel("● Đang kết nối tới: Đang dò tìm máy chủ...");
        statusLabel.setForeground(new Color(52, 211, 153));
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnSwitch = new JButton("Chuyển Server (Test Chịu Lỗi Failover)");
        btnSwitch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSwitch.addActionListener(e -> {
            failoverManager.forceSwitchServer();
            refreshRecords();
            JOptionPane.showMessageDialog(this, "Đã chuyển đổi kết nối sang: " + failoverManager.getActiveEndpoint());
        });

        topBanner.add(statusLabel, BorderLayout.WEST);
        topBanner.add(btnSwitch, BorderLayout.EAST);
        mainPanel.add(topBanner, BorderLayout.NORTH);

        // Center Split: Left Form / Right Table
        JPanel formPanel = createFormPanel();
        JPanel tablePanel = createTablePanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, tablePanel);
        split.setDividerLocation(340);
        split.setResizeWeight(0.35);

        mainPanel.add(split, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Thông Tin Bản Ghi"));

        JPanel fields = new JPanel(new GridLayout(4, 2, 8, 8));
        fields.setBorder(new EmptyBorder(8, 8, 8, 8));

        fields.add(new JLabel("Mã Sinh Viên (ID):"));
        txtId = new JTextField("24NS004");
        fields.add(txtId);

        fields.add(new JLabel("Họ và Tên:"));
        txtName = new JTextField("Phạm Văn Dũng");
        fields.add(txtName);

        fields.add(new JLabel("Khoa / Chuyên Ngành:"));
        txtDept = new JTextField("Khoa Học Máy Tính");
        fields.add(txtDept);

        fields.add(new JLabel("Điểm Số (GPA):"));
        txtScore = new JTextField("3.65");
        fields.add(txtScore);

        panel.add(fields, BorderLayout.NORTH);

        // Action Buttons
        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        btnPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JButton btnAdd = new JButton("➕ Thêm Mới Bản Ghi");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.BLACK);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.addActionListener(e -> performAdd());

        JButton btnUpdate = new JButton("✏ Cập Nhật Bản Ghi");
        btnUpdate.setBackground(new Color(37, 99, 235));
        btnUpdate.setForeground(Color.BLACK);
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.addActionListener(e -> performUpdate());

        JButton btnDelete = new JButton("🗑 Xóa Bản Ghi");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.BLACK);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.addActionListener(e -> performDelete());

        JButton btnClear = new JButton("Làm Trống Form");
        btnClear.addActionListener(e -> {
            txtId.setText("");
            txtName.setText("");
            txtDept.setText("");
            txtScore.setText("");
        });

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        panel.add(btnPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Dữ Liệu Đồng Bộ RMI Trên Máy Chủ"));

        // Search Bar & Refresh
        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setBorder(new EmptyBorder(4, 4, 4, 4));

        searchRow.add(new JLabel("Tìm kiếm (Mã/Tên): "), BorderLayout.WEST);
        txtSearch = new JTextField();
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterTable(txtSearch.getText().trim());
            }
        });
        searchRow.add(txtSearch, BorderLayout.CENTER);

        JButton btnRefresh = new JButton("🔄 Làm Mới");
        btnRefresh.addActionListener(e -> refreshRecords());
        searchRow.add(btnRefresh, BorderLayout.EAST);

        panel.add(searchRow, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{"Mã SV", "Họ Tên", "Khoa / Ngành", "GPA", "Phiên Bản", "Server Sửa", "Thời Gian"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    txtId.setText((String) tableModel.getValueAt(row, 0));
                    txtName.setText((String) tableModel.getValueAt(row, 1));
                    txtDept.setText((String) tableModel.getValueAt(row, 2));
                    txtScore.setText(String.valueOf(tableModel.getValueAt(row, 3)));
                }
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void performAdd() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String dept = txtDept.getText().trim();
        String scoreStr = txtScore.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mã SV và Họ tên không được để trống!");
            return;
        }

        double score = 0;
        try { score = Double.parseDouble(scoreStr); } catch (Exception ignored) {}

        DataRecord r = new DataRecord(id, name, dept, score, "CLIENT");
        new Thread(() -> {
            IClientService service = failoverManager.getService();
            if (service == null) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Không thể kết nối tới máy chủ nào!"));
                return;
            }
            try {
                boolean ok = service.createRecord(r);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Thêm mới thành công! Dữ liệu đang được đồng bộ qua RMI tới Server đối tác.");
                        refreshRecords();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi RMI: " + ex.getMessage()));
            }
        }).start();
    }

    private void performUpdate() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String dept = txtDept.getText().trim();
        double score = Double.parseDouble(txtScore.getText().trim());

        DataRecord r = new DataRecord(id, name, dept, score, "CLIENT");
        new Thread(() -> {
            IClientService service = failoverManager.getService();
            if (service == null) return;
            try {
                boolean ok = service.updateRecord(r);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Cập nhật thành công! Phiên bản mới đã được đồng bộ sang Server đối tác.");
                        refreshRecords();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    private void performDelete() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) return;
        int conf = JOptionPane.showConfirmDialog(this, "Xác nhận xóa bản ghi: " + id + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                IClientService service = failoverManager.getService();
                if (service == null) return;
                try {
                    boolean ok = service.deleteRecord(id);
                    SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            JOptionPane.showMessageDialog(this, "Đã xóa bản ghi! Thao tác xóa đã được đồng bộ qua RMI.");
                            refreshRecords();
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
                }
            }).start();
        }
    }

    private void refreshRecords() {
        new Thread(() -> {
            IClientService service = failoverManager.getService();
            if (service != null) {
                try {
                    String activeServer = service.getServerId();
                    List<DataRecord> list = service.getAllRecords();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● Đang kết nối tới: " + activeServer + " (" + failoverManager.getActiveEndpoint().host + ":" + failoverManager.getActiveEndpoint().port + ")");
                        statusLabel.setForeground(new Color(52, 211, 153));
                        recordList.clear();
                        recordList.addAll(list);
                        populateTable(recordList);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● Mất kết nối! Đang thử failover...");
                        statusLabel.setForeground(Color.RED);
                    });
                }
            } else {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("● Không thể kết nối tới cụm máy chủ!");
                    statusLabel.setForeground(Color.RED);
                });
            }
        }).start();
    }

    private void populateTable(List<DataRecord> list) {
        tableModel.setRowCount(0);
        for (DataRecord r : list) {
            tableModel.addRow(new Object[]{
                    r.getId(),
                    r.getName(),
                    r.getDepartment(),
                    r.getScore(),
                    "v" + r.getVersion(),
                    r.getUpdatedByServer(),
                    r.getFormattedDate()
            });
        }
    }

    private void filterTable(String query) {
        if (query.isEmpty()) {
            populateTable(recordList);
            return;
        }
        String q = query.toLowerCase();
        List<DataRecord> filtered = new ArrayList<>();
        for (DataRecord r : recordList) {
            if (r.getId().toLowerCase().contains(q) || r.getName().toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }
        populateTable(filtered);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            String host1 = "127.0.0.1";
            int port1 = 1099;
            String host2 = "127.0.0.1";
            int port2 = 1100;
            if (args.length >= 2) {
                host1 = args[0];
                port1 = Integer.parseInt(args[1]);
            }
            if (args.length >= 4) {
                host2 = args[2];
                port2 = Integer.parseInt(args[3]);
            }
            RmiClientGUI gui = new RmiClientGUI(host1, port1, host2, port2);
            gui.setVisible(true);
        });
    }
}
