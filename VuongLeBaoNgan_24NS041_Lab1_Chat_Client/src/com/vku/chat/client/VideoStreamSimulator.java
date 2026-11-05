package com.vku.chat.client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class VideoStreamSimulator {

    public static byte[] generateVideoSnapshot(String title, String subtitle) {
        try {
            int width = 320;
            int height = 240;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Gradient background
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 60, 114), width, height, new Color(42, 82, 152));
            g2.setPaint(gp);
            g2.fillRect(0, 0, width, height);

            // Camera overlay icon
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillOval(width / 2 - 35, height / 2 - 50, 70, 70);
            g2.setColor(new Color(24, 119, 242));
            g2.fillPolygon(
                    new int[]{width / 2 - 15, width / 2 - 15, width / 2 + 20},
                    new int[]{height / 2 - 35, height / 2 - 5, height / 2 - 20},
                    3
            );

            // Text
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            g2.drawString(title, (width - titleWidth) / 2, height / 2 + 45);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(200, 220, 255));
            int subWidth = g2.getFontMetrics().stringWidth(subtitle);
            g2.drawString(subtitle, (width - subWidth) / 2, height / 2 + 65);

            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static void showVideoPlayer(Frame parent, String title, byte[] imageData) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(parent, "Video Player - " + title, false);
            dialog.setSize(380, 320);
            dialog.setLocationRelativeTo(parent);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(Color.BLACK);

            if (imageData != null && imageData.length > 0) {
                ImageIcon icon = new ImageIcon(imageData);
                JLabel imageLabel = new JLabel(icon, JLabel.CENTER);
                panel.add(imageLabel, BorderLayout.CENTER);
            } else {
                JLabel errLabel = new JLabel("Không thể hiển thị video!", JLabel.CENTER);
                errLabel.setForeground(Color.WHITE);
                panel.add(errLabel, BorderLayout.CENTER);
            }

            JLabel statusLabel = new JLabel("  Đang phát luồng UDP Media (60 fps mô phỏng)  ", JLabel.CENTER);
            statusLabel.setForeground(new Color(0, 255, 128));
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            panel.add(statusLabel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.setVisible(true);
        });
    }
}
