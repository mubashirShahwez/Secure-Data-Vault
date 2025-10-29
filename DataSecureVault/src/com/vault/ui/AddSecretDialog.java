package com.vault.ui;

import com.vault.model.User;
import com.vault.service.UserService;
import com.vault.service.VaultService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class AddSecretDialog extends JDialog {

    private final User currentUser;
    private final VaultService vaultService;
    private final UserService userService;

    private JTextField keyField;
    private JTextArea valueArea;
    private JCheckBox showValueCheckbox;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel charCount;

    private boolean isSaved = false;

    public AddSecretDialog(Frame owner, User currentUser, VaultService vaultService, UserService userService) {
        super(owner, "Add New Secret", true);
        this.currentUser = currentUser;
        this.vaultService = vaultService;
        this.userService = userService;

        initializeComponents();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(520, 360));
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    private void initializeComponents() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("➕ Add New Secret");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel("Enter a label and the value you want encrypted");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(Color.GRAY);
        header.add(sub, BorderLayout.SOUTH);
        content.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Key Name:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        keyField = new JTextField(28);
        keyField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        form.add(keyField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Secret Value:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        valueArea = new JTextArea(8, 28);
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(valueArea);
        form.add(scroll, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        showValueCheckbox = new JCheckBox("Show value (uncheck to hide)");
        showValueCheckbox.setSelected(true);
        form.add(showValueCheckbox, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        charCount = new JLabel("0 characters");
        charCount.setForeground(Color.GRAY);
        form.add(charCount, gbc);

        content.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        cancelButton = new JButton("Cancel");
        saveButton = new JButton("💾 Save");
        buttons.add(cancelButton);
        buttons.add(saveButton);
        content.add(buttons, BorderLayout.SOUTH);

        // Events
        saveButton.addActionListener(e -> saveSecret());
        cancelButton.addActionListener(e -> onCancel());
        keyField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { validateForm(); }
        });
        valueArea.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                validateForm();
                updateCharCount();
            }
        });

        setContentPane(content);
        validateForm();
        updateCharCount();
    }

    private void updateCharCount() {
        int n = valueArea.getText().length();
        charCount.setText(n + " character" + (n == 1 ? "" : "s"));
        if (n > 1000) charCount.setForeground(Color.RED);
        else if (n > 500) charCount.setForeground(new Color(200, 120, 0));
        else charCount.setForeground(Color.GRAY);
    }

    private void validateForm() {
        boolean ok = !keyField.getText().trim().isEmpty() && !valueArea.getText().isEmpty();
        saveButton.setEnabled(ok);
    }

    private void saveSecret() {
        String keyName = keyField.getText().trim();
        String value = valueArea.getText();

        if (keyName.isEmpty() || value.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both key name and value.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Service handles encryption and DB insert
            vaultService.addSecret(currentUser.getId(), keyName, value);
            userService.logAccess(currentUser.getId(), "ADD", keyName);

            JOptionPane.showMessageDialog(this,
                    "Secret '" + keyName + "' added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            isSaved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to add secret: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void onCancel() {
        if (!keyField.getText().trim().isEmpty() || !valueArea.getText().isEmpty()) {
            int c = JOptionPane.showConfirmDialog(this,
                    "Discard input and close?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }
        dispose();
    }

    public boolean isSaved() {
        return isSaved;
    }
}

