package server;

import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {

    Socket socket;
    BufferedReader br;
    PrintWriter pw;
    String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void handlePrivateMessage(String message) {
        try {
            // format: /msg username "message"
            String[] parts = message.split(" ", 3);

            if (parts.length < 3) return;

            String user = parts[1];
            String msg = parts[2];

            Socket targetSocket = Server.nameToSocket.get(user);
            if (targetSocket == null) {
                pw.println("[System] User " + user + " not found");
                return;
            }

            PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
            targetOut.println("[Private] from " + username + ": " + msg);
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            br = new BufferedReader (
                new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);

            username = br.readLine(); // dòng đầu tiên lấy username

            Server.userMap.put(socket, username);
            System.out.println("User connected: " + username);

            Server.nameToSocket.put(username, socket);

            Server.broadcastOnline();
            Server.broadcast("[SYSTEM] " + username + " joined", socket);
          
            String message;
            while ((message = br.readLine()) != null) {
                 // nếu muốn command /list
                if (message.equals("/list")) {
                    pw.println("ONLINE:" + String.join(",", Server.userMap.values()));
                    continue;
                }

                if (message.startsWith("/msg")) {
                    handlePrivateMessage(message);
                    continue;
                }

                System.out.println(username + ": " + message);
                Server.broadcast(username + ": " + message, socket);
            }
            

        }
        catch (Exception e) {
           // e.printStackTrace();
            System.out.println(username + " disconnected");
        }
        finally {
            try {
                Server.userMap.remove(socket);
                Server.removeClient(socket);
                socket.close();
               // Server.broadcast("[SYSTEM] " + user + " left", socket);
                Server.broadcastOnline();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}
