package server;

import db.DatabaseManager;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class Server {

    static final List<Socket> clients = Collections.synchronizedList(new ArrayList<>());
    // xem user online
    static final Map<Socket, String> userMap = Collections.synchronizedMap(new HashMap<>());
    // chat private tung user
    static final Map<String, Socket> nameToSocket = Collections.synchronizedMap(new HashMap<>());

    static final Map<Socket, String> userGroup = Collections.synchronizedMap(new HashMap<>());
    static final Map<String, List<Socket>> groupMember = Collections.synchronizedMap(new HashMap<>());

    private static volatile boolean running = false;
    private static ServerSocket serverSocket;
    private static Thread acceptThread;
    private static ServerUI dashboard;

    public static void setDashboard(ServerUI ui) {
        dashboard = ui;
        refreshDashboard();
    }

    public static boolean isRunning() {
        return running;
    }

    public static synchronized void startServer(int port) {
        if (running) {
            log("Server is already running");
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            log("Server started on port " + port);
            refreshDashboard();
            acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        clients.add(socket);
                        log("Client connected: " + socket.getRemoteSocketAddress());
                        refreshDashboard();
                        new ClientHandler(socket).start();
                    } catch (SocketException ex) {
                        if (running) {
                            log("Server socket error: " + ex.getMessage());
                        }
                        break;
                    } catch (IOException ex) {
                        if (running) {
                            log("Accept error: " + ex.getMessage());
                        }
                    }
                }
                log("Accept loop stopped");
            }, "server-accept-loop");
            acceptThread.start();
        } catch (IOException ex) {
            running = false;
            log("Cannot start server: " + ex.getMessage());
            refreshDashboard();
        }
    }

    public static synchronized void stopServer() {
        if (!running) {
            log("Server is not running");
            return;
        }
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ex) {
            log("Error closing server socket: " + ex.getMessage());
        }

        List<Socket> snapshot;
        synchronized (clients) {
            snapshot = new ArrayList<>(clients);
        }
        for (Socket socket : snapshot) {
            try {
                socket.close();
            } catch (IOException ignored) { }
        }

        clients.clear();
        userMap.clear();
        nameToSocket.clear();
        groupMember.clear();
        userGroup.clear();

        log("Server stopped");
        refreshDashboard();
    }

    public static void log(String message) {
        System.out.println(message);
        if (dashboard != null) {
            dashboard.appendLog(message);
        }
    }

    public static List<String> getConnectedClientNames() {
        synchronized (userMap) {
            return new ArrayList<>(userMap.values());
        }
    }

    public static String getStatusText() {
        return running ? "RUNNING" : "STOPPED";
    }

    public static void refreshDashboard() {
        if (dashboard != null) {
            SwingUtilities.invokeLater(() -> dashboard.refreshState(getStatusText(), getConnectedClientNames()));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerUI::new);
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

    public static void broadcastGroupList(Socket socket) {
        try {
            String username = userMap.get(socket);
            Integer userId = username != null ? DatabaseManager.getUserIdByUsername(username) : null;
            List<String> groups = userId != null ? DatabaseManager.getGroupsForUser(userId) : new ArrayList<>();
            String groupList = "GROUP:" + String.join(",", groups);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(groupList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(Socket socket) {
        clients.remove(socket);
        refreshDashboard();
    }

    public static void broadcastToGroup(String groupName, String message) {
        List<Socket> member = groupMember.get(groupName);
        if (member == null) return;
        for (Socket s: member) {
            try {
                if (s == null) continue;
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println("[" + groupName + "]: " + message);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}