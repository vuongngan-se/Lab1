package com.vku.web.browser;

import com.vku.web.common.HistoryItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class HistoryDialog extends JDialog {
    private DefaultTableModel model;
    private JTable table;

    public HistoryDialog(Frame parent, List<HistoryItem> historyList, Consumer<String> onSelectUrl) {
        super(parent, "Lịch Sử Duyệt Web (Browsing History)", true);
        setSize(650, 420);
        setLocationRelativeTo(parent);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        model = new DefaultTableModel(new String[]{"Thời Gian", "Phương Thức", "Mã Phản Hồi", "Địa Chỉ URL"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(24);

        for (int i = historyList.size() - 1; i >= 0; i--) {
            HistoryItem it = historyList.get(i);
            model.addRow(new Object[]{it.getFormattedTime(), it.getMethod(), it.getStatusCode(), it.getUrl()});
        }

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        String url = (String) model.getValueAt(row, 3);
                        onSelectUrl.accept(url);
                        dispose();
                    }
                }
            }
        });

        main.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGo = new JButton("Truy Cập URL Này");
        btnGo.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String url = (String) model.getValueAt(row, 3);
                onSelectUrl.accept(url);
                dispose();
            }
        });
        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());

        bottom.add(btnGo);
        bottom.add(btnClose);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);
    }
}
