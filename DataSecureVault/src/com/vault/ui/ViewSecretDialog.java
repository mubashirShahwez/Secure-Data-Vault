package com.vault.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ViewSecretDialog extends JDialog {

    private final String keyName;
    private final String plainValue;

    public ViewSecretDialog(Frame owner, String keyName, String plainValue) {
        super(owner, "View Secret", true);
        this.keyName = keyName;
        this.plainValue = plainValue;

        initUI();
        setMinimumSize(new Dimension(520, 360));
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("👁️ View Secret");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("Decrypted value is shown below");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(Color.GRAY);
        header.add(sub, BorderLayout.SOUTH);

        content.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        body.add(new JLabel("Key Name:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField keyField = new JTextField(keyName);
        keyField.setEditable(false);
        keyField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        body.add(keyField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.weightx = 0;
        body.add(new JLabel("Secret Value:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1; gbc.weighty = 1;
        JTextArea valueArea = new JTextArea(8, 28);
        valueArea.setText(plainValue != null ? plainValue : "");
        valueArea.setEditable(false);
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(valueArea);
        body.add(scroll, gbc);

        content.add(body, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton copyBtn = new JButton("Copy");
        JButton closeBtn = new JButton("Close");
        footer.add(copyBtn);
        footer.add(closeBtn);
        content.add(footer, BorderLayout.SOUTH);

        copyBtn.addActionListener(e -> {
            StringSelection sel = new StringSelection(valueArea.getText());
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(sel, sel);
            JOptionPane.showMessageDialog(this, "Copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        closeBtn.addActionListener(e -> dispose());

        setContentPane(content);
    }
}
