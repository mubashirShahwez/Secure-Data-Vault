package com.vault.ui;

import javax.swing.*;
import java.awt.*;

public class ViewCipherDialog extends JDialog {

    public ViewCipherDialog(Frame owner, String keyName, String ciphertext) {
        super(owner, "Stored Ciphertext", true);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("🔒 Ciphertext for: " + keyName);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        content.add(title, BorderLayout.NORTH);

        JTextArea area = new JTextArea(12, 40);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(ciphertext != null ? ciphertext : "(null)");
        JScrollPane scroll = new JScrollPane(area);
        content.add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }
}

