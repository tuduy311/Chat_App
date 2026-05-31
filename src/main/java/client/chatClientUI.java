package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;




public class chatClientUI extends JFrame {
    // Multi-tab chat
    JTabbedPane chatTabs;
    Map<String, ChatSessionPanel> sessions;

    JTextField commandField;
    JButton commandSendBtn;
    JButton clearWelcomeBtn;
    JCheckBox enterSendCheckbox;

    Deque<String> pendingHistoryTabKeys;
    String activeHistoryTabKey;
    Map<String, IncomingFileTransfer> incomingFiles;

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
        enterSendCheckbox = new JCheckBox("Enter sends", true);
        clearWelcomeBtn = new JButton("Clear Welcome");
        clearWelcomeBtn.addActionListener(e -> welcomeArea.setText(""));
        topRightBar.add(enterSendCheckbox);
        topRightBar.add(clearWelcomeBtn);

        chatTabs = new JTabbedPane();
        sessions = new HashMap<>();
        pendingHistoryTabKeys = new ArrayDeque<>();
        incomingFiles = new HashMap<>();

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
        openPrivateTab(user, true);
    }

    private void openPrivateTab(String user, boolean autoLoadHistory) {
        String key = keyForPrivate(user);
        if (sessions.containsKey(key)) {
            chatTabs.setSelectedComponent(sessions.get(key));
            return;
        }
        ChatSessionPanel p = new ChatSessionPanel(user, false);
        sessions.put(key, p);
        addClosableTab(user, p);
        chatTabs.setSelectedComponent(p);
        if (autoLoadHistory) {
            requestHistory("private", user);
        }
    }

    private void openGroupTab(String group) {
        openGroupTab(group, true);
    }

    private void openGroupTab(String group, boolean autoLoadHistory) {
        String key = keyForGroup(group);
        if (sessions.containsKey(key)) {
            chatTabs.setSelectedComponent(sessions.get(key));
            return;
        }
        ChatSessionPanel p = new ChatSessionPanel(group, true);
        sessions.put(key, p);
        addClosableTab(group, p);
        chatTabs.setSelectedComponent(p);
        if (autoLoadHistory) {
            requestHistory("group", group);
        }
    }

    private void requestHistory(String type, String target) {
        if (pw == null) return;
        pendingHistoryTabKeys.addLast("private".equalsIgnoreCase(type) ? keyForPrivate(target) : keyForGroup(target));
        pw.println("/history " + type + " " + target);
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

    private void showSystemNotice(String text) {
        appendWelcome(text);
        Component selected = chatTabs != null ? chatTabs.getSelectedComponent() : null;
        if (selected instanceof ChatSessionPanel) {
            ((ChatSessionPanel) selected).appendLine(text);
        }
        JOptionPane.showMessageDialog(this, text, "ChatApp", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class IncomingFileTransfer {
        String scope;
        String target;
        String sender;
        String fileName;
        long fileSize;
        int totalParts;
        Map<Integer, String> chunks = new LinkedHashMap<>();

        IncomingFileTransfer(String scope, String target, String sender, String fileName, long fileSize, int totalParts) {
            this.scope = scope;
            this.target = target;
            this.sender = sender;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.totalParts = totalParts;
        }

        boolean isComplete() {
            return chunks.size() >= totalParts && totalParts > 0;
        }

        byte[] assemble() throws Exception {
            StringBuilder encoded = new StringBuilder();
            for (int i = 0; i < totalParts; i++) {
                String chunk = chunks.get(i);
                if (chunk == null) {
                    throw new IllegalStateException("Missing chunk " + i);
                }
                encoded.append(chunk);
            }
            return Base64.getDecoder().decode(encoded.toString());
        }
    }

    private String fileTransferKey(String scope, String target, String sender, String fileName) {
        return scope + "|" + target + "|" + sender + "|" + fileName;
    }

    private String findFileSessionKey(String scope, String target, String sender) {
        if (scope.equalsIgnoreCase("group")) {
            return keyForGroup(target);
        }
        return sender.equalsIgnoreCase(username) ? keyForPrivate(target) : keyForPrivate(sender);
    }

    private void showSaveDialogAndWriteFile(byte[] data, String fileName) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(fileName));
                int result = chooser.showSaveDialog(chatClientUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();
                    Files.write(selected.toPath(), data);
                    appendWelcome("[System] Đã lưu file: " + selected.getAbsolutePath());
                } else {
                    appendWelcome("[System] Đã nhận file: " + fileName + " (chưa lưu)");
                }
            } catch (Exception ex) {
                appendWelcome("[System] Không thể lưu file: " + fileName);
            }
        });
    }

    private void handleFileMetaMessage(String msg) {
        String[] parts = msg.split("\\|", 7);
        if (parts.length < 7) return;

        String scope = parts[1].trim();
        String target = parts[2].trim();
        String sender = parts[3].trim();
        String fileName = parts[4].trim();
        long fileSize = 0L;
        int totalParts = 0;
        try {
            fileSize = Long.parseLong(parts[5].trim());
            totalParts = Integer.parseInt(parts[6].trim());
        } catch (NumberFormatException ignored) { }

        String sessionKey = findFileSessionKey(scope, target, sender);
        if (scope.equalsIgnoreCase("private") && sender.equalsIgnoreCase(username)) {
            if (!sessions.containsKey(sessionKey)) openPrivateTab(target, false);
            sessions.get(sessionKey).appendLine("[File] Đã gửi " + fileName + " tới " + target + " (" + fileSize + " bytes)");
            return;
        }

        if (scope.equalsIgnoreCase("group") && sender.equalsIgnoreCase(username)) {
            if (!sessions.containsKey(sessionKey)) openGroupTab(target, false);
            sessions.get(sessionKey).appendLine("[File] Bạn đã gửi " + fileName + " vào nhóm " + target + " (" + fileSize + " bytes)");
            return;
        }

        if (!sessions.containsKey(sessionKey)) {
            if (scope.equalsIgnoreCase("group")) {
                openGroupTab(target, false);
            } else {
                openPrivateTab(sender, false);
            }
        }

        ChatSessionPanel panel = sessions.get(sessionKey);
        if (panel != null) {
            panel.appendLine("[File] " + sender + " đang gửi " + fileName + " (" + fileSize + " bytes)");
        }

        String key = fileTransferKey(scope, target, sender, fileName);
        incomingFiles.put(key, new IncomingFileTransfer(scope, target, sender, fileName, fileSize, totalParts));
    }

    private void handleFilePartMessage(String msg) {
        String[] parts = msg.split("\\|", 7);
        if (parts.length < 7) return;

        String scope = parts[1].trim();
        String target = parts[2].trim();
        String sender = parts[3].trim();
        String fileName = parts[4].trim();
        int index;
        try {
            index = Integer.parseInt(parts[5].trim());
        } catch (NumberFormatException ex) {
            return;
        }
        String chunk = parts[6];

        if (sender.equalsIgnoreCase(username)) {
            return;
        }

        String key = fileTransferKey(scope, target, sender, fileName);
        IncomingFileTransfer transfer = incomingFiles.get(key);
        if (transfer == null) return;

        transfer.chunks.put(index, chunk);
        if (transfer.isComplete()) {
            try {
                byte[] data = transfer.assemble();
                incomingFiles.remove(key);
                String sessionKey = findFileSessionKey(scope, target, sender);
                ChatSessionPanel panel = sessions.get(sessionKey);
                if (panel != null) {
                    panel.appendLine("[File] Nhận xong: " + fileName + " từ " + sender);
                }
                showSaveDialogAndWriteFile(data, fileName);
            } catch (Exception ex) {
                appendWelcome("[System] Lỗi ghép file: " + fileName);
                incomingFiles.remove(key);
            }
        }
    }

    // Panel representing one chat session (private or group)
    private class ChatSessionPanel extends JPanel {
        String target;
        boolean isGroup;
        JTextArea transcript;
        JTextArea input;
        JButton send;

        ChatSessionPanel(String target, boolean isGroup) {
            super(new BorderLayout());
            this.target = target; this.isGroup = isGroup;
            transcript = new JTextArea(); transcript.setEditable(false);
            // Add right-click context menu on transcript lines for Copy ID / Delete
            transcript.addMouseListener(new MouseAdapter() {
                private void showMenu(MouseEvent e) {
                    try {
                        int offset = transcript.viewToModel2D(e.getPoint());
                        int line = transcript.getLineOfOffset(offset);
                        int start = transcript.getLineStartOffset(line);
                        int end = transcript.getLineEndOffset(line);
                        String lineText = transcript.getText().substring(start, end).trim();
                        // find pattern [#123]
                        String id = null;
                        int idx = lineText.indexOf("[#");
                        if (idx >= 0) {
                            int endIdx = lineText.indexOf(']', idx);
                            if (endIdx > idx) {
                                String inner = lineText.substring(idx+2, endIdx);
                                if (inner.matches("\\d+")) id = inner;
                            }
                        }
                        final String foundId = id;

                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem copy = new JMenuItem("Copy ID");
                        copy.addActionListener(ae -> {
                            if (foundId != null) {
                                try {
                                    java.awt.datatransfer.StringSelection ss = new java.awt.datatransfer.StringSelection(foundId);
                                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                                    appendWelcome("[System] Copied id: " + foundId);
                                } catch (Exception ex) { }
                            }
                        });
                        menu.add(copy);

                        JMenuItem del = new JMenuItem("Delete Message");
                        del.addActionListener(ae -> {
                            if (foundId != null) {
                                String scope = isGroup ? "group" : "private";
                                // send scope-aware delete command so the server can decide global vs local delete
                                sendRawCommand("/delete " + scope + " " + foundId);
                            } else {
                                appendWelcome("[System] No message id found on this line");
                            }
                        });
                        menu.add(del);

                        menu.show(transcript, e.getX(), e.getY());
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
                @Override
                public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            });
            // multiline input with compact emoji picker
            input = new JTextArea(3, 40);
            input.setLineWrap(true);
            input.setWrapStyleWord(true);
            send = new JButton("Send");
            send.setMargin(new Insets(2, 8, 2, 8));
            send.setPreferredSize(new Dimension(64, 30));

            JScrollPane sp = new JScrollPane(transcript);
            JScrollPane inputScroll = new JScrollPane(input);

            JButton fileButton = new JButton("📎");
            fileButton.setMargin(new Insets(2, 8, 2, 8));
            fileButton.setPreferredSize(new Dimension(40, 30));

            JButton emojiPicker = new JButton("😊");
            emojiPicker.setMargin(new Insets(2, 8, 2, 8));
            emojiPicker.setPreferredSize(new Dimension(40, 30));

            JPopupMenu emojiMenu = new JPopupMenu();
            JPanel emojiGrid = new JPanel(new GridLayout(2, 5, 4, 4));
            emojiGrid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            String[] emojis = {"😀", "😂", "😍", "👍", "🎉", "❤️", "😮", "😢", "😎", "🔥"};
            for (String em : emojis) {
                JButton emojiItem = new JButton(em);
                emojiItem.setMargin(new Insets(2, 4, 2, 4));
                emojiItem.setFocusPainted(false);
                emojiItem.setPreferredSize(new Dimension(36, 32));
                emojiItem.addActionListener(ae -> {
                    input.append(em);
                    input.requestFocusInWindow();
                    emojiMenu.setVisible(false);
                });
                emojiGrid.add(emojiItem);
            }
            emojiMenu.add(emojiGrid);
            emojiPicker.addActionListener(ae -> emojiMenu.show(emojiPicker, 0, emojiPicker.getHeight()));

            fileButton.addActionListener(ae -> sendFile());

            JPanel bottom = new JPanel(new BorderLayout());
            JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            actionBar.add(fileButton);
            actionBar.add(emojiPicker);
            actionBar.add(send);

            bottom.add(inputScroll, BorderLayout.CENTER);
            bottom.add(actionBar, BorderLayout.EAST);

            add(sp, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            // Key bindings: Shift+Enter -> newline; Enter -> send if checkbox selected
            InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = input.getActionMap();
            im.put(KeyStroke.getKeyStroke("shift ENTER"), "insert-newline");
            im.put(KeyStroke.getKeyStroke("ENTER"), "enter-pressed");
            am.put("insert-newline", new AbstractAction() { public void actionPerformed(ActionEvent e) { input.append("\n"); } });
            am.put("enter-pressed", new AbstractAction() { public void actionPerformed(ActionEvent e) {
                if (enterSendCheckbox.isSelected()) sendFromPanel(); else input.append("\n");
            } });

            send.addActionListener(e -> sendFromPanel());
        }

        private void sendFile() {
            if (pw == null) return;
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(ChatSessionPanel.this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File selectedFile = chooser.getSelectedFile();
            if (selectedFile == null || !selectedFile.exists()) return;

            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                long maxBytes = 5L * 1024L * 1024L; // 5 MB limit
                if (fileBytes.length > maxBytes) {
                    showSystemNotice("[System] File quá lớn. Giới hạn 5 MB.");
                    return;
                }
                String encoded = Base64.getEncoder().encodeToString(fileBytes);
                int chunkSize = 3500;
                int totalParts = (encoded.length() + chunkSize - 1) / chunkSize;
                String scope = isGroup ? "group" : "private";

                appendLine("[File] Đang gửi " + selectedFile.getName() + " (" + fileBytes.length + " bytes)");
                pw.println("FILEMETA|" + scope + "|" + target + "|" + username + "|" + selectedFile.getName() + "|" + fileBytes.length + "|" + totalParts);

                for (int i = 0; i < totalParts; i++) {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, encoded.length());
                    String part = encoded.substring(start, end);
                    pw.println("FILEPART|" + scope + "|" + target + "|" + username + "|" + selectedFile.getName() + "|" + i + "|" + part);
                }
            } catch (Exception ex) {
                showSystemNotice("[System] Không thể gửi file: " + selectedFile.getName());
            }
        }

        void appendLine(String line) {
            transcript.append(line + "\n");
            transcript.setCaretPosition(transcript.getDocument().getLength());
        }

        void markDeleted(int messageId) {
            String marker = "[#" + messageId + "]";
            String[] lines = transcript.getText().split("\n");
            StringBuilder sb = new StringBuilder();
            for (String l : lines) {
                if (l.contains(marker)) {
                    // Replace message body with [deleted]
                    int colon = l.indexOf(":");
                    if (colon >= 0) {
                        String prefix = l.substring(0, colon+1);
                        sb.append(prefix).append(" [deleted]");
                    } else {
                        sb.append("[deleted]");
                    }
                } else {
                    sb.append(l);
                }
                sb.append("\n");
            }
            transcript.setText(sb.toString());
        }

        void sendFromPanel() {
            String text = input.getText().trim();
            if (text.isEmpty() || pw == null) return;
            if (text.startsWith("/")) {
                sendRawCommand(text);
            } else if (isGroup) {
                pw.println("/" + target + " " + text);
            } else {
                pw.println("/msg " + target + " " + text);
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
                    openPrivateTab(target, false);
                    pendingHistoryTabKeys.addLast(keyForPrivate(target));
                } else if (type.equalsIgnoreCase("group")) {
                    openGroupTab(target, false);
                    pendingHistoryTabKeys.addLast(keyForGroup(target));
                }
            }
        } else if (text.startsWith("/msg ")) {
            // If user typed /msg in the global command bar, just open the private tab;
            // the server will echo back a '[Private] to' message which we display as 'Me:'
            String[] parts = text.split(" ", 3);
            if (parts.length >= 2) {
                String target = parts[1].trim();
                openPrivateTab(target);
            }
        } else if (text.equalsIgnoreCase("/mygroups")) {
            activeHistoryTabKey = null;
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
                    else if (msg.startsWith("RECENT:")) {
                        String payload = msg.substring("RECENT:".length()).trim();
                        if (!payload.isEmpty()) {
                            String[] users = payload.split(",");
                            for (String u : users) {
                                String user = u.trim();
                                if (!user.isEmpty()) {
                                    // open private tab and auto-load history
                                    openPrivateTab(user, true);
                                }
                            }
                        }
                    }
                    else if (msg.startsWith("FILEACK|")) {
                        try {
                            String[] parts = msg.split("\\|", 6);
                            if (parts.length < 6) {
                                appendWelcome(msg);
                                continue;
                            }
                            String scope = parts[1].trim();
                            String target = parts[2].trim();
                            String fileName = parts[3].trim();
                            String fileSize = parts[4].trim();
                            String sessionKey = scope.equalsIgnoreCase("group") ? keyForGroup(target) : keyForPrivate(target);
                            if (!sessions.containsKey(sessionKey)) {
                                if (scope.equalsIgnoreCase("group")) {
                                    openGroupTab(target, false);
                                } else {
                                    openPrivateTab(target, false);
                                }
                            }
                            sessions.get(sessionKey).appendLine("[File] Đã gửi " + fileName + " tới " + target + " (" + fileSize + " bytes)");
                        } catch (Exception ex) {
                            appendWelcome(msg);
                        }
                    }
                    else if (msg.startsWith("[System] File too large")) {
                        showSystemNotice(msg);
                    }
                    else if (msg.startsWith("FILEMETA|")) {
                        handleFileMetaMessage(msg);
                    }
                    else if (msg.startsWith("FILEPART|")) {
                        handleFilePartMessage(msg);
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
                    else if (msg.startsWith("[Private] to ")) {
                        // format: [Private] to bob: hello [#123]
                        try {
                            int idx = msg.indexOf(":");
                            String head = msg.substring(0, idx);
                            String toUser = head.substring("[Private] to ".length()).trim();
                            String content = msg.substring(idx+1).trim();
                            String key = keyForPrivate(toUser);
                            if (!sessions.containsKey(key)) openPrivateTab(toUser);
                            // show as Me: content
                            sessions.get(key).appendLine("Me: " + content);
                        } catch (Exception ex) {
                            appendWelcome(msg);
                        }
                    }
                    else if (msg.startsWith("[") && msg.contains("]: ") && !msg.startsWith("[History]")) {
                        // group: [groupName]: username: message
                        try {
                            int endBracket = msg.indexOf("]:");
                            if (endBracket > 0) {
                                // group name is between '[' and ']'
                                String group = msg.substring(1, endBracket);
                                // skip "]:" and the following space to get "username: message"
                                String rest = msg.substring(endBracket + 3).trim();
                                String key = keyForGroup(group);
                                if (!sessions.containsKey(key)) openGroupTab(group);
                                sessions.get(key).appendLine(rest);
                            } else {
                                Component c = chatTabs.getSelectedComponent();
                                if (c instanceof ChatSessionPanel) ((ChatSessionPanel)c).appendLine(msg);
                            }
                        } catch (Exception ex) {
                            Component c = chatTabs.getSelectedComponent();
                            if (c instanceof ChatSessionPanel) ((ChatSessionPanel)c).appendLine(msg);
                        }
                    }
                    else if (msg.startsWith("[History]")) {
                        String historyText = msg.substring("[History]".length()).trim();
                        if (msg.startsWith("[History] Last ")) {
                            activeHistoryTabKey = pendingHistoryTabKeys.pollFirst();
                        }
                        if (activeHistoryTabKey != null && sessions.containsKey(activeHistoryTabKey)) {
                            sessions.get(activeHistoryTabKey).appendLine(historyText);
                        } else {
                            appendWelcome(historyText);
                        }
                    }
                    else if (msg.startsWith("[Deleted] ") || msg.startsWith("[DeletedLocal] ")) {
                        try {
                            String prefix = msg.startsWith("[DeletedLocal] ") ? "[DeletedLocal] " : "[Deleted] ";
                            String payload = msg.substring(prefix.length()).trim();
                            String[] parts = payload.split(" ", 2);
                            if (parts.length < 2) {
                                appendWelcome(msg);
                                continue;
                            }
                            String scope = parts[0].trim();
                            int id = Integer.parseInt(parts[1].trim());
                            boolean groupScope = scope.equalsIgnoreCase("group");
                            // Mark deleted only in sessions matching the scope
                            for (ChatSessionPanel p : sessions.values()) {
                                if (p.isGroup == groupScope) {
                                    p.markDeleted(id);
                                }
                            }
                            if (msg.startsWith("[DeletedLocal] ")) {
                                appendWelcome("[System] Message " + id + " removed only for you");
                            } else {
                                appendWelcome("[System] Message " + id + " deleted for everyone");
                            }
                        } catch (Exception ex) {
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
