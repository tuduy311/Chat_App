package server;

import db.DatabaseManager;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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
            // Save to database if possible
            try {
                Integer senderId = DatabaseManager.getUserIdByUsername(username);
                Integer receiverId = DatabaseManager.getUserIdByUsername(user);
                if (senderId != null && receiverId != null) {
                    DatabaseManager.savePrivateMessage(senderId, receiverId, msg);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
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
                // History request: /history private username OR /history group groupname
                if (message.startsWith("/history")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length >= 3) {
                        String type = parts[1];
                        String target = parts[2];
                        if (type.equalsIgnoreCase("private")) {
                            Integer myId = DatabaseManager.getUserIdByUsername(username);
                            Integer otherId = DatabaseManager.getUserIdByUsername(target);
                            if (myId != null && otherId != null) {
                                List<String> hist = DatabaseManager.getPrivateHistory(myId, otherId, 50);
                                pw.println("[History] Last " + hist.size() + " messages with " + target + ":");
                                for (int i = hist.size()-1; i >=0; i--) {
                                    pw.println("[History] " + hist.get(i));
                                }
                            } else {
                                pw.println("[History] No history found or user not found");
                            }
                        } else if (type.equalsIgnoreCase("group")) {
                            Integer groupId = DatabaseManager.getOrCreateGroupId(target);
                            if (groupId != null) {
                                List<String> hist = DatabaseManager.getGroupHistory(groupId, 50);
                                pw.println("[History] Last " + hist.size() + " messages in group " + target + ":");
                                for (int i = hist.size()-1; i >=0; i--) {
                                    pw.println("[History] " + hist.get(i));
                                }
                            } else {
                                pw.println("[History] No group history found");
                            }
                        }
                    } else {
                        pw.println("Usage: /history private username OR /history group groupname");
                    }
                    continue;
                }
                if (message.startsWith("/msg")) {
                    handlePrivateMessage(message);
                    continue;
                }

                else if (message.startsWith("/createGroup ")) {
                    String group = message.split(" ")[1];
                    if(Server.groupMember.putIfAbsent(group, new ArrayList<>()) == null ) {
                        pw.println("Group created: " + group);
                    }
                    else pw.println("Group already exists: " + group);
                    
                    //  Remove khỏi group cũ
                    String currentGroup = Server.userGroup.get(socket);
                    if (currentGroup != null) {
                        Server.groupMember.get(currentGroup).remove(socket);
                        pw.println("[System] Left group: " + currentGroup);
                    }

                    // Chỉ add nếu socket chưa ở group này
                    if (!Server.groupMember.get(group).contains(socket)) {
                        Server.groupMember.get(group).add(socket);
                    }
                    Server.userGroup.put(socket, group);

                    pw.println("[System] Joined group: " + group);
                    Server.broadcastGroupList(socket);

                    // Persist group in DB
                    try {
                        DatabaseManager.getOrCreateGroupId(group);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    continue;
                }

                else if (message.startsWith("/join")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length < 2) {
                        pw.println("Usage: /join groupName");
                        continue;
                    }

                    String groupName = parts[1];
                    if (!Server.groupMember.containsKey(groupName)) {
                        pw.println("Group does not exist: " + groupName);
                        continue;
                    }
                    
                    // Remove khỏi group cũ nếu có
                    String currentGroup = Server.userGroup.get(socket);
                    if (currentGroup != null) {
                        Server.groupMember.get(currentGroup).remove(socket);  
                        pw.println("[System] Left group: " + currentGroup);
                    }

                    // Join vào group mới
                    Server.userGroup.put(socket, groupName);  
                    Server.groupMember.get(groupName).add(socket);
                    pw.println("[System] Joined group: " + groupName);
                    Server.broadcastGroupList(socket);
                    continue;
                }

                else if (message.startsWith("/leave ")) {
                    String[] parts = message.split(" ", 2); 
                        if (parts.length < 2) {
                            pw.println("Usage: /leave groupName");
                            continue;
                        }
                        
                    String group = parts[1].trim();
                    if (!Server.groupMember.containsKey(group)) {
                        pw.println("Group not found");
                        continue;
                    }

                    if (!Server.userGroup.containsKey(socket) ||
                        !Server.userGroup.get(socket).equals(group)) {
                        pw.println("You are not in this group");
                        continue;
                    }

                    Server.groupMember.get(group).remove(socket);
                    Server.userGroup.remove(socket);

                    pw.println("Left group: " + group);

                    Server.broadcastGroupList(socket);
                    continue;
                }

                else if (message.startsWith("/")) {
                    int fistSpace = message.indexOf(" ");
                    if (fistSpace == -1) {
                        pw.println("Invalid command");
                        continue;
                    }
                    String groupName = message.substring(1, fistSpace);
                    if (!Server.groupMember.containsKey(groupName)) {
                        pw.println("Group does not exist: " + groupName);
                        continue;
                    }

                    // CHECK: Verify user ở trong group
                    String currentGroup = Server.userGroup.get(socket);
                    if (currentGroup == null || !currentGroup.equals(groupName)) {
                        pw.println("[System] You are not in this group");
                        continue;
                    }

                    String msg = message.substring(fistSpace + 1);
                    Server.broadcastToGroup(groupName, username + ": " + msg);

                    // Save group message to DB
                    try {
                        Integer groupId = DatabaseManager.getOrCreateGroupId(groupName);
                        Integer userId = DatabaseManager.getUserIdByUsername(username);
                        if (groupId != null && userId != null) {
                            DatabaseManager.saveGroupMessage(groupId, userId, msg);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    continue;
                }
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
