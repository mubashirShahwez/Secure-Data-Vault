package com.vault.ui;

import com.vault.model.User;
import com.vault.service.VaultService;
import com.vault.service.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class UpdateSecretDialog extends JDialog {

    private final User currentUser;
    private final VaultService vaultService;
    private final UserService userService;

    private JTextField keyField;
    private JTextArea valueArea;
    private JCheckBox showValueCheckbox;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel characterCountLabel;

    private boolean saved = false;
    private String originalKeyName;
    private String originalSecretValue;
    private int secretId;

    public UpdateSecretDialog(Frame parent, User currentUser, int secretId,
                              String keyName, String secretValue,
                              VaultService vaultService, UserService userService) {
        super(parent, "Update Secret", true);
        this.currentUser = currentUser;
        this.vaultService = vaultService;
        this.userService = userService;
        this.secretId = secretId;
        this.originalKeyName = keyName;
        this.originalSecretValue = secretValue;

        initializeComponents();
        setupEventHandlers();
        populateFields();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(parent);
        setResizable(true);
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));

        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        panel.setBackground(new Color(250, 250, 250));

        JLabel titleLabel = new JLabel("✏️ Update Secret");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(60, 60, 60));
        panel.add(titleLabel, BorderLayout.WEST);

        JLabel instructionLabel = new JLabel("Modify the secret information below");
        instructionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instructionLabel.setForeground(Color.GRAY);
        panel.add(instructionLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Key Name Label and Field
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel keyLabel = new JLabel("Key Name:");
        keyLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        panel.add(keyLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        keyField = new JTextField(25);
        keyField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        keyField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        panel.add(keyField, gbc);

        // Secret Value Label
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel valueLabel = new JLabel("Secret Value:");
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        panel.add(valueLabel, gbc);

        // Secret Value Text Area with Scroll
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;

        valueArea = new JTextArea(8, 25);
        valueArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        valueArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(valueArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        panel.add(scrollPane, gbc);

        // Show/Hide Value Checkbox
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;

        showValueCheckbox = new JCheckBox("Show value (uncheck to hide)");
        showValueCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        showValueCheckbox.setSelected(true);
        panel.add(showValueCheckbox, gbc);

        // Character Count Label
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;

        characterCountLabel = new JLabel("0 characters");
        characterCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        characterCountLabel.setForeground(Color.GRAY);
        panel.add(characterCountLabel, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panel.setBackground(new Color(245, 245, 245));

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cancelButton.setPreferredSize(new Dimension(100, 35));

        saveButton = new JButton("💾 Update");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(120, 35));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);

        panel.add(cancelButton);
        panel.add(saveButton);

        return panel;
    }

    private void setupEventHandlers() {
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });

        // Character count update
        valueArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateCharacterCount();
            }
        });

        // Enter key handling for key field
        keyField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    valueArea.requestFocus();
                }
            }
        });

        // Form validation
        KeyAdapter validationListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                validateForm();
            }
        };
        keyField.addKeyListener(validationListener);
        valueArea.addKeyListener(validationListener);
    }

    private void populateFields() {
        if (originalKeyName != null) {
            keyField.setText(originalKeyName);
        }
        if (originalSecretValue != null) {
            valueArea.setText(originalSecretValue);
        }
        updateCharacterCount();
        validateForm();
    }

    private void handleSave() {
        String keyName = keyField.getText().trim();
        String secretValue = valueArea.getText();

        if (keyName.isEmpty()) {
            showError("Key name cannot be empty.");
            keyField.requestFocus();
            return;
        }

        if (secretValue.isEmpty()) {
            showError("Secret value cannot be empty.");
            valueArea.requestFocus();
            return;
        }

        if (keyName.length() > 100) {
            showError("Key name too long. Maximum 100 characters allowed.");
            keyField.requestFocus();
            return;
        }

        try {
            if (vaultService != null && currentUser != null) {
                vaultService.updateSecret(currentUser.getId(), secretId, keyName, secretValue);

                if (userService != null) {
                    userService.logAccess(currentUser.getId(), "UPDATE", keyName);
                }
            }

            saved = true;
            showSuccess("Secret '" + keyName + "' updated successfully!");
            dispose();

        } catch (Exception ex) {
            showError("Failed to update secret: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleCancel() {
        if (hasChanges()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You have unsaved changes. Are you sure you want to cancel?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        saved = false;
        dispose();
    }

    private void updateCharacterCount() {
        int count = valueArea.getText().length();
        characterCountLabel.setText(count + " character" + (count != 1 ? "s" : ""));

        if (count > 1000) {
            characterCountLabel.setForeground(Color.RED);
        } else if (count > 500) {
            characterCountLabel.setForeground(Color.ORANGE);
        } else {
            characterCountLabel.setForeground(Color.GRAY);
        }
    }

    private void validateForm() {
        String keyName = keyField.getText().trim();
        String secretValue = valueArea.getText();

        boolean valid = !keyName.isEmpty() && !secretValue.isEmpty() && keyName.length() <= 100;
        saveButton.setEnabled(valid);

        if (valid) {
            saveButton.setToolTipText("Update this secret");
        } else {
            saveButton.setToolTipText("Please fill in all required fields");
        }
    }

    private boolean hasChanges() {
        String currentKey = keyField.getText().trim();
        String currentValue = valueArea.getText();

        return !currentKey.equals(originalKeyName) || !currentValue.equals(originalSecretValue);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public boolean isSaved() {
        return saved;
    }
}

