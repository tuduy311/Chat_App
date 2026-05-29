package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;




public class chatClientUI extends JFrame {
    // Multi-tab chat
    JTabbedPane chatTabs;
    Map<String, ChatSessionPanel> sessions;

    JTextField commandField;
    JButton commandSendBtn;
    JButton clearWelcomeBtn;

    String pendingHistoryTabKey;

    JTextArea welcomeArea;
    JTextArea onlineArea;
    JTextArea groupArea;

    Socket socket;
    PrintWriter pw;
    BufferedReader br;

    String username;

    // Constructor with username parameter (from LoginFrame)
    public chatClientUI(String username) {
        this.username = username;
        initUI();
        connectServer();
        receiveMessages();
    }
    
    // Old constructor for backward compatibility (shows prompt)
    public chatClientUI() {
        username = JOptionPane.showInputDialog("Enter username:");
       
        initUI();
        connectServer();
        receiveMessages();
    }

    private void initUI() {
        setTitle("ChatApp");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
        setLayout(new BorderLayout());

        // ____________ Left panel: Online users _______________
        onlineArea = new JTextArea();
        onlineArea.setEditable(false);
        groupArea = new JTextArea();
        groupArea.setEditable(false);

        //JScrollPane leftPanel = new JScrollPane(onlineArea);
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayout(2, 1));

        JScrollPane onlineScroll = new JScrollPane(onlineArea);
        JScrollPane groupScroll = new JScrollPane(groupArea);

        leftPanel.add(onlineScroll);
        leftPanel.add(groupScroll);


        // ___________ Right panel: Chat tabs __________________
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Global command bar: send raw slash commands here
        JPanel commandBar = new JPanel(new BorderLayout());
        commandField = new JTextField();
        commandField.setToolTipText("Commands: /createGroup, /join, /leave, /history, /mygroups, /delete, /msg ...");
        commandSendBtn = new JButton("Run Command");
        commandBar.add(new JLabel("Command: "), BorderLayout.WEST);
        commandBar.add(commandField, BorderLayout.CENTER);
        commandBar.add(commandSendBtn, BorderLayout.EAST);

        JPanel topRightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        clearWelcomeBtn = new JButton("Clear Welcome");
        clearWelcomeBtn.addActionListener(e -> welcomeArea.setText(""));
        topRightBar.add(clearWelcomeBtn);

        chatTabs = new JTabbedPane();
        sessions = new HashMap<>();

        // Welcome tab = system notifications / command feedback
        welcomeArea = new JTextArea("Welcome. System notifications will appear here.\n");
        welcomeArea.setEditable(false);
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.add(new JScrollPane(welcomeArea), BorderLayout.CENTER);
        welcomePanel.add(topRightBar, BorderLayout.SOUTH);
        chatTabs.addTab("Welcome", welcomePanel);

        rightPanel.add(commandBar, BorderLayout.NORTH);
        rightPanel.add(chatTabs, BorderLayout.CENTER);



        // split UI
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel
        );
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);



        // Sidebar click: open or focus tab
        onlineArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    int offset = onlineArea.viewToModel2D(e.getPoint());
                    int line = onlineArea.getLineOfOffset(offset);
                    int start = onlineArea.getLineStartOffset(line);
                    int end = onlineArea.getLineEndOffset(line);
                    String lineText = onlineArea.getText().substring(start, end).trim();
                    if (lineText.startsWith("- ")) {
                        String user = lineText.substring(2).trim();
                        if (!user.isEmpty()) openPrivateTab(user);
                    }
                } catch (Exception ex) { }
            }
        });

        groupArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    int offset = groupArea.viewToModel2D(e.getPoint());
                    int line = groupArea.getLineOfOffset(offset);
                    int start = groupArea.getLineStartOffset(line);
                    int end = groupArea.getLineEndOffset(line);
                    String lineText = groupArea.getText().substring(start, end).trim();
                    if (lineText.startsWith("- ")) {
                        String group = lineText.substring(2).trim();
                        if (!group.isEmpty()) openGroupTab(group);
                    }
                } catch (Exception ex) { }
            }
        });

        commandSendBtn.addActionListener(e -> sendRawCommand(commandField.getText().trim()));
        commandField.addActionListener(e -> sendRawCommand(commandField.getText().trim()));

        setVisible(true);
        
    }

    // Helper: tab key naming
    private String keyForPrivate(String user) { return "p:" + user; }
    private String keyForGroup(String group) { return "g:" + group; }

    private void openPrivateTab(String user) {
        String key = keyForPrivate(user);
        if (sessions.containsKey(key)) {
            chatTabs.setSelectedComponent(sessions.get(key));
            return;
        }
        ChatSessionPanel p = new ChatSessionPanel(user, false);
        sessions.put(key, p);
        addClosableTab(user, p);
        chatTabs.setSelectedComponent(p);
    }

    private void openGroupTab(String group) {
        String key = keyForGroup(group);
        if (sessions.containsKey(key)) {
            chatTabs.setSelectedComponent(sessions.get(key));
            return;
        }
        ChatSessionPanel p = new ChatSessionPanel(group, true);
        sessions.put(key, p);
        addClosableTab(group, p);
        chatTabs.setSelectedComponent(p);
    }

    private void addClosableTab(String title, Component content) {
        chatTabs.addTab(title, content);
        int index = chatTabs.indexOfComponent(content);
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeader.setOpaque(false);
        tabHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));

        JButton close = new JButton("×");
        close.setMargin(new Insets(0, 0, 0, 0));
        close.setFocusable(false);
        close.setPreferredSize(new Dimension(16, 16));
        close.setMinimumSize(new Dimension(16, 16));
        close.setMaximumSize(new Dimension(16, 16));
        close.setFont(close.getFont().deriveFont(Font.BOLD, 11f));
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setBackground(new Color(0, 0, 0, 0));
        close.setOpaque(false);
        close.setContentAreaFilled(false);
        close.setForeground(new Color(110, 110, 110));
        close.setRolloverEnabled(true);
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                close.setForeground(new Color(160, 60, 60));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setForeground(new Color(110, 110, 110));
            }
        });
        close.addActionListener(e -> {
            int tabIndex = chatTabs.indexOfComponent(content);
            if (tabIndex >= 0 && tabIndex != 0) {
                if (content instanceof ChatSessionPanel) {
                    ChatSessionPanel panel = (ChatSessionPanel) content;
                    sessions.remove(panel.isGroup ? keyForGroup(panel.target) : keyForPrivate(panel.target));
                }
                chatTabs.remove(tabIndex);
            }
        });
        tabHeader.add(label);
        tabHeader.add(Box.createHorizontalStrut(8));
        tabHeader.add(close);
        chatTabs.setTabComponentAt(index, tabHeader);
    }

    private void appendWelcome(String text) {
        welcomeArea.append(text + "\n");
        welcomeArea.setCaretPosition(welcomeArea.getDocument().getLength());
    }

    // Panel representing one chat session (private or group)
    private class ChatSessionPanel extends JPanel {
        String target;
        boolean isGroup;
        JTextArea transcript;
        JTextField input;
        JButton send;

        ChatSessionPanel(String target, boolean isGroup) {
            super(new BorderLayout());
            this.target = target; this.isGroup = isGroup;
            transcript = new JTextArea(); transcript.setEditable(false);
            input = new JTextField();
            send = new JButton("Send");

            JScrollPane sp = new JScrollPane(transcript);
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.add(input, BorderLayout.CENTER);
            bottom.add(send, BorderLayout.EAST);

            add(sp, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            send.addActionListener(e -> sendFromPanel());
            input.addActionListener(e -> sendFromPanel());
        }

        void appendLine(String line) {
            transcript.append(line + "\n");
            transcript.setCaretPosition(transcript.getDocument().getLength());
        }

        void sendFromPanel() {
            String text = input.getText().trim();
            if (text.isEmpty() || pw == null) return;
            if (text.startsWith("/")) {
                sendRawCommand(text);
            } else if (isGroup) {
                pw.println("/" + target + " " + text);
                appendLine("Me: " + text);
            } else {
                pw.println("/msg " + target + " " + text);
                appendLine("Me: " + text);
            }
            input.setText("");
        }
    }

    private void sendRawCommand(String text) {
        if (text.isEmpty() || pw == null) return;

        preparePendingHistoryTarget(text);
        pw.println(text);
        appendWelcome("[Command] " + text);
        commandField.setText("");
    }

    private void preparePendingHistoryTarget(String text) {
        if (text.startsWith("/history ")) {
            String[] parts = text.split(" ", 3);
            if (parts.length >= 3) {
                String type = parts[1].trim();
                String target = parts[2].trim();
                if (type.equalsIgnoreCase("private")) {
                    openPrivateTab(target);
                    pendingHistoryTabKey = keyForPrivate(target);
                } else if (type.equalsIgnoreCase("group")) {
                    openGroupTab(target);
                    pendingHistoryTabKey = keyForGroup(target);
                }
            }
        } else if (text.startsWith("/msg ")) {
            // If user typed /msg in the global command bar, open the private tab
            // and append the local "Me: ..." line so the chat shows immediately.
            String[] parts = text.split(" ", 3);
            if (parts.length >= 3) {
                String target = parts[1].trim();
                String body = parts[2].trim();
                openPrivateTab(target);
                String k = keyForPrivate(target);
                if (sessions.containsKey(k)) {
                    sessions.get(k).appendLine("Me: " + body);
                }
            }
        } else if (text.equalsIgnoreCase("/mygroups")) {
            pendingHistoryTabKey = null;
        }
    }

    

    private void connectServer() {
        try{
            socket = new Socket("localhost", 8080);

            pw = new PrintWriter(socket.getOutputStream(), true);
            br = new BufferedReader( 
                    new InputStreamReader(socket.getInputStream()));

            // set window title and send username
            setTitle("ChatApp - " + username);
            pw.println(username);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    private void receiveMessages() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = br.readLine()) != null) {
                    if (msg.startsWith("ONLINE:")) {
                        String users = msg.substring(7);
                        onlineArea.setText("Online:\n");

                        for (String u : users.split(",")) {
                            onlineArea.append("- " + u + "\n");
                        }

                        appendWelcome("[System] Online users updated: " + users);

                    }
                    else if (msg.startsWith("GROUP:")) {
                        String groups = msg.substring(6);
                        groupArea.setText("Groups:\n");

                        if (!groups.isEmpty()) {
                            for (String g : groups.split(",")) {
                                groupArea.append("- " + g + "\n");
                            }
                        }
                        appendWelcome("[System] Group list updated: " + groups);
                        //continue;
                    }
                    else if (msg.startsWith("[Private] from ")) {
                        // format: [Private] from alice: hello
                        try {
                            int idx = msg.indexOf(":");
                            String head = msg.substring(0, idx);
                            String from = head.substring("[Private] from ".length()).trim();
                            String content = msg.substring(idx+1).trim();
                            String key = keyForPrivate(from);
                            if (!sessions.containsKey(key)) openPrivateTab(from);
                            sessions.get(key).appendLine("" + from + ": " + content);
                        } catch (Exception ex) {
                            // fallback append to welcome
                            Component c = chatTabs.getSelectedComponent();
                            if (c instanceof ChatSessionPanel) ((ChatSessionPanel)c).appendLine(msg);
                        }
                    }
                    else if (msg.startsWith("[") && msg.contains("]: ") && !msg.startsWith("[History]")) {
                        // group: [groupName]: username: message
                        try {
                            int endBracket = msg.indexOf("]:");
                            String group = msg.substring(1, endBracket);
                            String rest = msg.substring(endBracket+3).trim();
                            String key = keyForGroup(group);
                            if (!sessions.containsKey(key)) openGroupTab(group);
                            sessions.get(key).appendLine(rest);
                        } catch (Exception ex) {
                            Component c = chatTabs.getSelectedComponent();
                            if (c instanceof ChatSessionPanel) ((ChatSessionPanel)c).appendLine(msg);
                        }
                    }
                    else if (msg.startsWith("[History]")) {
                        if (pendingHistoryTabKey != null && sessions.containsKey(pendingHistoryTabKey)) {
                            sessions.get(pendingHistoryTabKey).appendLine(msg);
                        } else {
                            appendWelcome(msg);
                        }
                    }
                    else {
                        appendWelcome(msg);
                    }
                }

                // auto-scroll handled per-panel
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public static void main(String[] args) {
        // Launch LoginFrame instead of directly showing chat
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}
