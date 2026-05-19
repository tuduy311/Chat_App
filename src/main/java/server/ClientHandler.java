package server;

import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    
    Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader (
                new InputStreamReader(socket.getInputStream()));

            String username = br.readLine(); // dòng đầu tiên
            Server.userMap.put(socket, username);
            System.out.println("User connected: " + username);
          
            String message;
            while ((message = br.readLine()) != null) {
                String user = Server.userMap.get(socket);

                System.out.println(user + ": " + message);
                Server.broadcast(user + ": " + message, socket);
            }
        }
        catch (Exception e) {
            System.out.println("Client disconnected: " + socket);
        }
        finally {
            try {
                socket.close();
                Server.removeClient(socket);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}
