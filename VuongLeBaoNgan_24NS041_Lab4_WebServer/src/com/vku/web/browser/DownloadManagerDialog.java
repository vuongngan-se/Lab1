package com.vku.web.browser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class DownloadManagerDialog extends JDialog {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel fileLabel;
    private JButton btnOpen;

    public DownloadManagerDialog(Frame parent, String url, File targetFile) {
        super(parent, "Trình Quản Lý Tải Xuống (Download Manager)", false);
        setSize(480, 200);
        setLocationRelativeTo(parent);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(new EmptyBorder(12, 16, 12, 16));

        fileLabel = new JLabel("Đang tải: " + targetFile.getName());
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        main.add(fileLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 6, 6));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        statusLabel = new JLabel("Đang kết nối tới máy chủ...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        center.add(progressBar);
        center.add(statusLabel);
        main.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOpen = new JButton("Mở Tệp / Thư Mục");
        btnOpen.setEnabled(false);
        btnOpen.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(targetFile.getParentFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở: " + ex.getMessage());
            }
        });
        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnOpen);
        bottom.add(btnClose);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);

        // Start async download
        new Thread(() -> {
            try {
                HttpEngine.download(url, targetFile, (downloaded, total) -> {
                    SwingUtilities.invokeLater(() -> {
                        if (total > 0) {
                            int pct = (int) ((downloaded * 100) / total);
                            progressBar.setValue(pct);
                            statusLabel.setText(String.format("Đã tải: %d KB / %d KB (%d%%)", downloaded / 1024, total / 1024, pct));
                        } else {
                            progressBar.setIndeterminate(true);
                            statusLabel.setText(String.format("Đã tải: %d KB", downloaded / 1024));
                        }
                    });
                });
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    statusLabel.setText("✓ Tải xuống hoàn tất! Lưu tại: " + targetFile.getName());
                    btnOpen.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("✗ Lỗi tải xuống: " + e.getMessage());
                });
            }
        }).start();
    }
}
