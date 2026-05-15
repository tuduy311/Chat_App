package client;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    public static void main(String[] args) { 
        try {
            Socket socket = new Socket("localhost", 8080);
            System.out.println("Connected to the server ...");

            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader br = new BufferedReader (
                                    new InputStreamReader(socket.getInputStream()));
                
            // thread nhan message
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = br.readLine()) != null) {
                        System.out.println("Server: " + msg);
                    }
                }
                catch (Exception e) {
                   System.out.println("Disconnected from server ...");
                }
            }).start();

            // thread gui message
            BufferedReader console = new BufferedReader(
            new InputStreamReader(System.in));
            String msg;
            while ((msg = console.readLine()) != null) {
                pw.println(msg);
}
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}