package server;

import db.DatabaseManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ServerUI extends JFrame {

    private final JTextField portField;
    private final JLabel statusLabel;
    private final JLabel clientCountLabel;
    private final DefaultListModel<String> clientListModel;
    private final JTextArea logArea;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton dbCheckButton;
    private final JButton clearLogButton;

    public ServerUI() {
        super("Chat Server Dashboard");
        Server.setDashboard(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel configPanel = new JPanel(new BorderLayout(10, 8));
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        formPanel.add(new JLabel("Host: localhost"));
        formPanel.add(new JLabel("Port:"));
        portField = new JTextField("8080", 8);
        formPanel.add(portField);
        statusLabel = new JLabel("STOPPED");
        formPanel.add(new JLabel("Status:"));
        formPanel.add(statusLabel);
        configPanel.add(formPanel, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        startButton = new JButton("Open Server");
        stopButton = new JButton("Close Server");
        dbCheckButton = new JButton("Check DB");
        clearLogButton = new JButton("Clear Log");
        actionPanel.add(startButton);
        actionPanel.add(stopButton);
        actionPanel.add(dbCheckButton);
        actionPanel.add(clearLogButton);
        configPanel.add(actionPanel, BorderLayout.CENTER);

        add(configPanel, BorderLayout.NORTH);

        clientListModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientListModel);
        clientList.setVisibleRowCount(12);
        clientList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        JPanel clientsPanel = new JPanel(new BorderLayout());
        clientsPanel.setBorder(BorderFactory.createTitledBorder("Connected Clients"));
        clientsPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);
        clientsPanel.setPreferredSize(new Dimension(240, 300));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Server Log"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        centerPanel.add(clientsPanel, BorderLayout.WEST);
        centerPanel.add(logPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        clientCountLabel = new JLabel("Clients: 0");
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JSeparator(), BorderLayout.NORTH);
        bottomPanel.add(clientCountLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            int port = parsePort();
            if (port <= 0) {
                appendLog("Invalid port number");
                return;
            }
            Server.startServer(port);
            refreshState(Server.getStatusText(), Server.getConnectedClientNames());
        });

        stopButton.addActionListener(e -> {
            Server.stopServer();
            refreshState(Server.getStatusText(), Server.getConnectedClientNames());
        });

        dbCheckButton.addActionListener(e -> {
            boolean ok = DatabaseManager.testConnection();
            appendLog(ok ? "Database connection OK" : "Database connection failed");
        });

        clearLogButton.addActionListener(e -> {
            logArea.setText("");
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Server.stopServer();
            }
        });

        refreshState(Server.getStatusText(), Server.getConnectedClientNames());
        appendLog("Server dashboard ready");
        setVisible(true);
    }

    private int parsePort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public void refreshState(String status, List<String> clients) {
        statusLabel.setText(status);
        clientListModel.clear();
        for (String client : clients) {
            clientListModel.addElement(client);
        }
        clientCountLabel.setText("Clients: " + clients.size());
        startButton.setEnabled(!Server.isRunning());
        stopButton.setEnabled(Server.isRunning());
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
