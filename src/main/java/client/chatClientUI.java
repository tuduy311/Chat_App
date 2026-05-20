package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;




public class chatClientUI extends JFrame {
    JTextArea chatArea;
    JTextField inputField;
    JButton sendBtn;
    JTextArea onlineArea;

    Socket socket;
    PrintWriter pw;
    BufferedReader br;

    String username;

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

        JScrollPane leftPanel = new JScrollPane(onlineArea);


        // ___________ Right panel: Chat area __________________
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        
        JScrollPane chatPane = new JScrollPane(chatArea);

        // Bottom panel
        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("Send");

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        //add(bottom, BorderLayout.SOUTH);

        rightPanel.add(chatPane, BorderLayout.CENTER);
        rightPanel.add(bottom, BorderLayout.SOUTH);



        // split UI
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel
        );
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);



        // send event
        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);
        
    }

    

    private void connectServer() {
        try{
            socket = new Socket("localhost", 8080);

            pw = new PrintWriter(socket.getOutputStream(), true);
            br = new BufferedReader( 
                    new InputStreamReader(socket.getInputStream()));

            chatArea.append("Connected as: " + username + "\n");

            // Gui username truoc khi chat
            pw.println(username);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String msg = inputField.getText();
        if (msg.isEmpty()) return;
         
        pw.println(msg);
        System.out.println(pw);
        chatArea.append("Me: " + msg + "\n");
        inputField.setText("");
    }

    private void receiveMessages() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = br.readLine()) != null) {
                    if (msg.startsWith("ONLINE:")) {

                    String users = msg.substring(7);

                    onlineArea.setText("Online Users:\n");

                    for (String u : users.split(",")) {
                        onlineArea.append("- " + u + "\n");
                        chatArea.append("- " + u + "\n");
                    }

                    } else {
                        chatArea.append(msg + "\n");
                    }
                }

                // tu dong scroll xuong cuoi khi co message moi
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public static void main(String[] args) {
        new chatClientUI();
    }
}
