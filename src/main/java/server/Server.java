package server;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Server is listening on port 8080 ...");

            Socket socket = serverSocket.accept();
            System.out.println("Client connected: " + socket.getInetAddress());

            BufferedReader br = new BufferedReader(
                                 new InputStreamReader(socket.getInputStream()));

            String mess = br.readLine();
            System.out.println("Client says: " + mess);


            socket.close();
            serverSocket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}