package com.vku.chat.client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class EmojiDialog extends JDialog {
    private static final String[] EMOJIS = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "😇", "🙂", "😉", "😍", "🥰", "😘", "😗",
            "😋", "😛", "😜", "🤪", "😎", "🤓", "🧐", "🥳",
            "👍", "👎", "👏", "🙌", "❤️", "🔥", "🎉", "🚀",
            "💯", "✨", "⭐", "💪", "🤝", "🙏", "☕", "💻"
    };

    public EmojiDialog(Frame parent, Consumer<String> onEmojiSelected) {
        super(parent, "Biểu tượng cảm xúc", true);
        setLayout(new BorderLayout());
        setSize(320, 240);
        setLocationRelativeTo(parent);

        JPanel grid = new JPanel(new GridLayout(5, 8, 4, 4));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (String emoji : EMOJIS) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.addActionListener(e -> {
                onEmojiSelected.accept(emoji);
                dispose();
            });
            grid.add(btn);
        }

        add(grid, BorderLayout.CENTER);
    }
}
