package client;

import java.io.*;
import java.net.*;



public class Client {
    public static void main(String[] args) { 
        try {
            Socket socket = new Socket("localhost", 8080);
            System.out.println("Connected to the server ...");

            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            pw.println("Hello Server.");


            
            socket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}