package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    static List<Socket> clients = new ArrayList<>();
    // xem user online
    static Map<Socket, String> userMap = new HashMap<>();
    // chat private tung user
    static Map<String, Socket> nameToSocket = new HashMap<>();

    public static void main(String[] args) {

        try {
            ServerSocket server = new ServerSocket(8080);
            System.out.println("Server is listening on port 8080 ...");

            while (true) {
                Socket socket = server.accept();

                clients.add(socket);

                // moi client 1 thread
                new ClientHandler(socket).start();

            }
 
        }
       
        catch (Exception e) {
            e.printStackTrace();
        }
       
    }

    public static void broadcast(String message, Socket sender) {
        for (Socket s : clients) {
            try {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                if (s == sender) continue; // khong gui lai cho client gui
                out.println(message);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void broadcastOnline() {
       String users = "ONLINE:" + String.join(",", userMap.values());
        for (Socket s : clients) {
            try {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println(users);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void removeClient(Socket socket) {
        clients.remove(socket);
    }

    

}