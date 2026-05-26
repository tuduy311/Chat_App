package client;

import db.DatabaseManager;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {
    private JTabbedPane tabbedPane;
    private JPanel loginPanel;
    private JPanel registerPanel;
    
    // Login components
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JButton loginButton;
    private JLabel loginStatusLabel;
    
    // Register components
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JPasswordField registerConfirmPasswordField;
    private JTextField registerEmailField;
    private JButton registerButton;
    private JLabel registerStatusLabel;
    
    private String loggedInUsername;

    public LoginFrame() {
        setTitle("Chat Application - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 350);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create login panel
        loginPanel = createLoginPanel();
        tabbedPane.addTab("Login", loginPanel);
        
        // Create register panel
        registerPanel = createRegisterPanel();
        tabbedPane.addTab("Register", registerPanel);
        
        add(tabbedPane);
        setVisible(true);
    }
    
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("Login to Chat");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Username label and field
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1;
        loginUsernameField = new JTextField(20);
        panel.add(loginUsernameField, gbc);
        
        // Password label and field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        loginPasswordField = new JPasswordField(20);
        panel.add(loginPasswordField, gbc);
        
        // Login button
        gbc.gridx = 1;
        gbc.gridy = 3;
        loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });
        panel.add(loginButton, gbc);
        
        // Status label
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        loginStatusLabel = new JLabel("");
        loginStatusLabel.setForeground(Color.RED);
        panel.add(loginStatusLabel, gbc);
        
        return panel;
    }
    
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("Register New Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Username label and field
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1;
        registerUsernameField = new JTextField(20);
        panel.add(registerUsernameField, gbc);
        
        // Email label and field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        registerEmailField = new JTextField(20);
        panel.add(registerEmailField, gbc);
        
        // Password label and field
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        registerPasswordField = new JPasswordField(20);
        panel.add(registerPasswordField, gbc);
        
        // Confirm password label and field
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Confirm Password:"), gbc);
        
        gbc.gridx = 1;
        registerConfirmPasswordField = new JPasswordField(20);
        panel.add(registerConfirmPasswordField, gbc);
        
        // Register button
        gbc.gridx = 1;
        gbc.gridy = 5;
        registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegister();
            }
        });
        panel.add(registerButton, gbc);
        
        // Status label
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        registerStatusLabel = new JLabel("");
        registerStatusLabel.setForeground(Color.RED);
        panel.add(registerStatusLabel, gbc);
        
        return panel;
    }
    
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            loginStatusLabel.setText("❌ Please fill all fields!");
            loginStatusLabel.setForeground(Color.RED);
            return;
        }
        
        // Check if user exists and password is correct
        User user = DatabaseManager.loginUser(username, password);
        if (user != null) {
            loginStatusLabel.setText("✓ Login successful!");
            loginStatusLabel.setForeground(new Color(0, 150, 0));
            
            // Open chat UI after 1 second
            Timer timer = new Timer(1000, e -> {
                loggedInUsername = username;
                openChatUI();
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            loginStatusLabel.setText("❌ Invalid username or password!");
            loginStatusLabel.setForeground(Color.RED);
            loginPasswordField.setText("");
        }
    }
    
    private void handleRegister() {
        String username = registerUsernameField.getText().trim();
        String email = registerEmailField.getText().trim();
        String password = new String(registerPasswordField.getPassword());
        String confirmPassword = new String(registerConfirmPasswordField.getPassword());
        
        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            registerStatusLabel.setText("❌ Please fill all fields!");
            registerStatusLabel.setForeground(Color.RED);
            return;
        }
        
        if (username.length() < 3) {
            registerStatusLabel.setText("❌ Username must be at least 3 characters!");
            registerStatusLabel.setForeground(Color.RED);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            registerStatusLabel.setText("❌ Passwords do not match!");
            registerStatusLabel.setForeground(Color.RED);
            registerPasswordField.setText("");
            registerConfirmPasswordField.setText("");
            return;
        }
        
        if (password.length() < 6) {
            registerStatusLabel.setText("❌ Password must be at least 6 characters!");
            registerStatusLabel.setForeground(Color.RED);
            return;
        }
        
        if (!email.contains("@")) {
            registerStatusLabel.setText("❌ Invalid email format!");
            registerStatusLabel.setForeground(Color.RED);
            return;
        }
        
        // Check if username already exists
        if (DatabaseManager.usernameExists(username)) {
            registerStatusLabel.setText("❌ Username already exists!");
            registerStatusLabel.setForeground(Color.RED);
            return;
        }
        
        // Register user
        if (DatabaseManager.registerUser(username, password, email)) {
            registerStatusLabel.setText("✓ Registration successful! Please login.");
            registerStatusLabel.setForeground(new Color(0, 150, 0));
            
            // Clear fields
            registerUsernameField.setText("");
            registerEmailField.setText("");
            registerPasswordField.setText("");
            registerConfirmPasswordField.setText("");
            
            // Switch to login tab after 2 seconds
            Timer timer = new Timer(2000, e -> {
                tabbedPane.setSelectedIndex(0);
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            registerStatusLabel.setText("❌ Registration failed! Please try again.");
            registerStatusLabel.setForeground(Color.RED);
        }
    }
    
    private void openChatUI() {
        // Open chat UI with logged-in username
        new chatClientUI(loggedInUsername);
        
        // Close login frame
        this.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}
