package com.vku.mail.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.function.Consumer;

public class ComposeDialog extends JDialog {
    public static class ComposeData {
        public String to;
        public String subject;
        public String body;
        public String attachName;
        public byte[] attachData;
    }

    private JTextField toField;
    private JTextField subjectField;
    private JTextArea bodyArea;
    private JLabel attachLabel;
    private File attachedFile;

    public ComposeDialog(Frame parent, String defaultFrom, Consumer<ComposeData> onSend) {
        super(parent, "Soạn Thư Mới (Compose Email)", true);
        setSize(650, 520);
        setLocationRelativeTo(parent);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));
        main.setBackground(Color.WHITE);

        // Header fields
        JPanel fieldsPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        fieldsPanel.setOpaque(false);

        JPanel fromRow = new JPanel(new BorderLayout(8, 0));
        fromRow.setOpaque(false);
        fromRow.add(new JLabel("Từ (From):   "), BorderLayout.WEST);
        JLabel fromLabel = new JLabel(defaultFrom);
        fromLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fromRow.add(fromLabel, BorderLayout.CENTER);
        fieldsPanel.add(fromRow);

        JPanel toRow = new JPanel(new BorderLayout(8, 0));
        toRow.setOpaque(false);
        toRow.add(new JLabel("Đến (To):     "), BorderLayout.WEST);
        toField = new JTextField("bob@vku.udn.vn");
        toRow.add(toField, BorderLayout.CENTER);
        fieldsPanel.add(toRow);

        JPanel subRow = new JPanel(new BorderLayout(8, 0));
        subRow.setOpaque(false);
        subRow.add(new JLabel("Tiêu đề:       "), BorderLayout.WEST);
        subjectField = new JTextField();
        subRow.add(subjectField, BorderLayout.CENTER);
        fieldsPanel.add(subRow);

        main.add(fieldsPanel, BorderLayout.NORTH);

        // Body area
        bodyArea = new JTextArea();
        bodyArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JScrollPane scrollBody = new JScrollPane(bodyArea);
        scrollBody.setBorder(BorderFactory.createTitledBorder("Nội Dung Email"));
        main.add(scrollBody, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);

        JPanel attachPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        attachPanel.setOpaque(false);
        JButton btnAttach = new JButton("📎 Đính Kèm Tệp");
        btnAttach.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                attachedFile = chooser.getSelectedFile();
                attachLabel.setText(attachedFile.getName() + " (" + (attachedFile.length() / 1024) + " KB)");
            }
        });
        attachLabel = new JLabel("Chưa có tệp đính kèm");
        attachLabel.setForeground(Color.GRAY);
        attachPanel.add(btnAttach);
        attachPanel.add(attachLabel);
        bottom.add(attachPanel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionPanel.setOpaque(false);
        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSend = new JButton(" Gửi Thư (Send) ");
        btnSend.setBackground(new Color(24, 119, 242));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.addActionListener(e -> {
            String to = toField.getText().trim();
            if (to.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập địa chỉ người nhận (To)!");
                return;
            }
            ComposeData data = new ComposeData();
            data.to = to;
            data.subject = subjectField.getText().trim();
            data.body = bodyArea.getText();
            if (attachedFile != null) {
                try {
                    data.attachName = attachedFile.getName();
                    data.attachData = Files.readAllBytes(attachedFile.toPath());
                } catch (Exception ex) {
                    System.err.println("Lỗi đọc file đính kèm: " + ex.getMessage());
                }
            }
            onSend.accept(data);
            dispose();
        });

        actionPanel.add(btnCancel);
        actionPanel.add(btnSend);
        bottom.add(actionPanel, BorderLayout.EAST);

        main.add(bottom, BorderLayout.SOUTH);
        add(main);
    }
}
