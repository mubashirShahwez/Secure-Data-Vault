package com.vault.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;

public class ViewFileCipherDialog extends JDialog {

    public ViewFileCipherDialog(Frame owner, String fileName, byte[] encryptedBytes) {
        super(owner, "Stored File Ciphertext", true);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("🔒 Ciphertext (Base64) for: " + fileName);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        content.add(title, BorderLayout.NORTH);

        JTextArea area = new JTextArea(14, 60);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        String b64 = encryptedBytes != null ? Base64.getEncoder().encodeToString(encryptedBytes) : "(null)";
        area.setText(b64);
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
