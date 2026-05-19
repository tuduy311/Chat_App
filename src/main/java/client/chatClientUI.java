package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;




public class chatClientUI extends JFrame {
    JTextArea chatArea;
    JTextField inputField;
    JButton sendBtn;

    Socket socket;
    PrintWriter pw;
    BufferedReader br;

    String username;

    public chatClientUI() {
       
        initUI();
        connectServer();
        receiveMessages();
    }

    private void initUI() {
        setTitle("ChatApp");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Bottom panel
        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("Send");

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // send event
        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);

        username = JOptionPane.showInputDialog("Enter username:");
    }

    

    private void connectServer() {
        try{
            socket = new Socket("localhost", 8080);

            pw = new PrintWriter(socket.getOutputStream(), true);
            br = new BufferedReader( 
                    new InputStreamReader(socket.getInputStream()));

            chatArea.append("Connected to the server ...\n");

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
                    chatArea.append(msg + "\n"); 
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
