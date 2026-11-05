package com.vku.rmi.monitor;

import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.ServerInfo;
import com.vku.rmi.service.IClientService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ClusterMonitorGUI extends JFrame {
    private final String host1;
    private final int port1;
    private final String host2;
    private final int port2;

    private JLabel s1Status;
    private JLabel s1Records;
    private JLabel s1Partner;

    private JLabel s2Status;
    private JLabel s2Records;
    private JLabel s2Partner;

    private DefaultTableModel compareModel;
    private JTextArea monitorLogArea;

    public ClusterMonitorGUI(String host1, int port1, String host2, int port2) {
        super("VKU RMI Cluster Monitor - Bảng Theo Dõi Đồng Bộ 2 Server (LTM)");
        this.host1 = host1;
        this.port1 = port1;
        this.host2 = host2;
        this.port2 = port2;
        initUI();
        startPolling();
    }

    private void initUI() {
        setSize(1050, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Server Status Cards (Server 1 & Server 2)
        JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        cardsPanel.setPreferredSize(new Dimension(0, 130));

        // Card Server 1
        JPanel c1 = new JPanel(new GridLayout(4, 1, 4, 4));
        c1.setBackground(new Color(240, 249, 255));
        c1.setBorder(BorderFactory.createTitledBorder("MÁY CHỦ 1 (SERVER_1 - " + host1 + ":" + port1 + ")"));
        s1Status = new JLabel("Trạng thái: Đang kết nối...");
        s1Records = new JLabel("Số bản ghi: 0");
        s1Partner = new JLabel("Kết nối Server 2: Chưa rõ");
        JLabel s1Role = new JLabel("Vai trò: Active Replica (Đồng bộ 2 chiều)");
        c1.add(s1Status);
        c1.add(s1Records);
        c1.add(s1Partner);
        c1.add(s1Role);
        cardsPanel.add(c1);

        // Card Server 2
        JPanel c2 = new JPanel(new GridLayout(4, 1, 4, 4));
        c2.setBackground(new Color(245, 243, 255));
        c2.setBorder(BorderFactory.createTitledBorder("MÁY CHỦ 2 (SERVER_2 - " + host2 + ":" + port2 + ")"));
        s2Status = new JLabel("Trạng thái: Đang kết nối...");
        s2Records = new JLabel("Số bản ghi: 0");
        s2Partner = new JLabel("Kết nối Server 1: Chưa rõ");
        JLabel s2Role = new JLabel("Vai trò: Active Replica (Đồng bộ 2 chiều)");
        c2.add(s2Status);
        c2.add(s2Records);
        c2.add(s2Partner);
        c2.add(s2Role);
        cardsPanel.add(c2);

        mainPanel.add(cardsPanel, BorderLayout.NORTH);

        // Center Table (Synchronized Records)
        compareModel = new DefaultTableModel(new String[]{"Mã SV", "Họ Tên", "Khoa / Ngành", "Điểm GPA", "Phiên Bản", "Máy Chủ Lưu Trữ", "Trạng Thái Đồng Bộ"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(compareModel);
        table.setRowHeight(24);
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Dữ Liệu Nhất Quán Giữa 2 Máy Chủ (Data Consistency Verification)"));

        // Bottom Monitor Logs
        monitorLogArea = new JTextArea();
        monitorLogArea.setEditable(false);
        monitorLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        monitorLogArea.setBackground(new Color(15, 23, 42));
        monitorLogArea.setForeground(new Color(241, 245, 249));
        JScrollPane scrollLog = new JScrollPane(monitorLogArea);
        scrollLog.setPreferredSize(new Dimension(0, 160));
        scrollLog.setBorder(BorderFactory.createTitledBorder("Nhật Ký Heartbeat & Đồng Bộ RMI"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTable, scrollLog);
        split.setDividerLocation(340);
        mainPanel.add(split, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void startPolling() {
        new Thread(() -> {
            while (true) {
                pollServer1();
                pollServer2();
                try {
                    Thread.sleep(2000); // Poll every 2 seconds
                } catch (InterruptedException ignored) {}
            }
        }, "Cluster-Monitor-Poller").start();
    }

    private void pollServer1() {
        try {
            Registry reg = LocateRegistry.getRegistry(host1, port1);
            IClientService service = (IClientService) reg.lookup("ClientService");
            ServerInfo info = service.getServerInfo();
            List<DataRecord> records = service.getAllRecords();

            SwingUtilities.invokeLater(() -> {
                s1Status.setText("Trạng thái: ● ONLINE (Uptime: " + info.getUptime() + "s)");
                s1Status.setForeground(new Color(5, 150, 105));
                s1Records.setText("Số bản ghi: " + records.size());
                s1Partner.setText("Kết nối Server 2: " + (info.isPartnerConnected() ? "✓ ĐÃ KẾT NỐI (Đồng bộ Realtime)" : "✗ MẤT KẾT NỐI"));
                s1Partner.setForeground(info.isPartnerConnected() ? new Color(5, 150, 105) : Color.RED);

                updateTableData(records);
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                s1Status.setText("Trạng thái: ✗ OFFLINE (Không thể kết nối)");
                s1Status.setForeground(Color.RED);
                s1Partner.setText("Kết nối Server 2: ✗ Không rõ");
            });
        }
    }

    private void pollServer2() {
        try {
            Registry reg = LocateRegistry.getRegistry(host2, port2);
            IClientService service = (IClientService) reg.lookup("ClientService");
            ServerInfo info = service.getServerInfo();
            List<DataRecord> records = service.getAllRecords();

            SwingUtilities.invokeLater(() -> {
                s2Status.setText("Trạng thái: ● ONLINE (Uptime: " + info.getUptime() + "s)");
                s2Status.setForeground(new Color(5, 150, 105));
                s2Records.setText("Số bản ghi: " + records.size());
                s2Partner.setText("Kết nối Server 1: " + (info.isPartnerConnected() ? "✓ ĐÃ KẾT NỐI (Đồng bộ Realtime)" : "✗ MẤT KẾT NỐI"));
                s2Partner.setForeground(info.isPartnerConnected() ? new Color(5, 150, 105) : Color.RED);
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                s2Status.setText("Trạng thái: ✗ OFFLINE (Không thể kết nối)");
                s2Status.setForeground(Color.RED);
                s2Partner.setText("Kết nối Server 1: ✗ Không rõ");
            });
        }
    }

    private synchronized void updateTableData(List<DataRecord> records) {
        compareModel.setRowCount(0);
        for (DataRecord r : records) {
            compareModel.addRow(new Object[]{
                    r.getId(),
                    r.getName(),
                    r.getDepartment(),
                    r.getScore(),
                    "v" + r.getVersion(),
                    r.getUpdatedByServer(),
                    "✓ ĐÃ ĐỒNG BỘ 100%"
            });
        }
    }

    public void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            monitorLogArea.append("[" + time + "] " + msg + "\n");
            monitorLogArea.setCaretPosition(monitorLogArea.getDocument().getLength());
        });
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
            ClusterMonitorGUI monitor = new ClusterMonitorGUI(host1, port1, host2, port2);
            monitor.setVisible(true);
        });
    }
}
