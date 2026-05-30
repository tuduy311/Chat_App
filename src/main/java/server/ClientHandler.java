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

            // Save to database and return generated id
            try {
                Integer senderId = DatabaseManager.getUserIdByUsername(username);
                Integer receiverId = DatabaseManager.getUserIdByUsername(user);
                Integer msgId = null;
                if (senderId != null && receiverId != null) {
                    msgId = DatabaseManager.savePrivateMessageReturnId(senderId, receiverId, msg);
                }

                String outMsg = "[Private] from " + username + ": " + msg + (msgId != null ? " [#" + msgId + "]" : "");
                PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                targetOut.println(outMsg);

                // Also send confirmation to sender so they see the message with id
                String selfMsg = "[Private] to " + user + ": " + msg + (msgId != null ? " [#" + msgId + "]" : "");
                pw.println(selfMsg);
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

            // Restore current memberships from DB so the user only sees active groups.
            Integer userId = DatabaseManager.getUserIdByUsername(username);
            if (userId != null) {
                for (String groupName : DatabaseManager.getGroupsForUser(userId)) {
                    Server.groupMember.putIfAbsent(groupName, new ArrayList<>());
                    if (!Server.groupMember.get(groupName).contains(socket)) {
                        Server.groupMember.get(groupName).add(socket);
                    }
                }
            }

            Server.broadcastOnline();
            Server.broadcastGroupList(socket);
           
            Server.broadcast("[SYSTEM] " + username + " joined", socket);
          
            String message;
            while ((message = br.readLine()) != null) {
                // Command routing:
                // - /history, /createGroup, /join, /leave, /delete: system commands from the client command bar
                // - /msg <user> <text>: private chat from a private tab
                // - /<group> <text>: group chat from a group tab
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

                if (message.equalsIgnoreCase("/mygroups")) {
                    Integer myId = DatabaseManager.getUserIdByUsername(username);
                    List<String> groups = myId != null ? DatabaseManager.getGroupsForUser(myId) : new ArrayList<>();
                    pw.println("[System] Current groups for " + username + ":");
                    if (groups.isEmpty()) {
                        pw.println("[System] (none)");
                    } else {
                        for (String groupName : groups) {
                            pw.println("[System] - " + groupName);
                        }
                    }
                    continue;
                }

                // Private chat command used by the client private tab.
                if (message.startsWith("/msg")) {
                    handlePrivateMessage(message);
                    continue;
                }

                // Create group command from the client command bar.
                else if (message.startsWith("/createGroup ")) {
                    String group = message.split(" ")[1];
                    if(Server.groupMember.putIfAbsent(group, new ArrayList<>()) == null ) {
                        pw.println("Group created: " + group);
                    }
                    else pw.println("Group already exists: " + group);

                    // Add current user to the new group without leaving other joined groups.
                    if (!Server.groupMember.get(group).contains(socket)) {
                        Server.groupMember.get(group).add(socket);
                    }

                    // Persist group in DB first so broadcast reads up-to-date data
                    try {
                        Integer groupId = DatabaseManager.getOrCreateGroupId(group);
                        Integer myId = DatabaseManager.getUserIdByUsername(username);
                        if (groupId != null && myId != null) {
                            DatabaseManager.addGroupMember(groupId, myId);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    pw.println("[System] Joined group: " + group);
                    Server.broadcastGroupList(socket);

                    continue;
                }

                // Join group command from the client command bar.
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

                    // Join the new group without leaving other groups.
                    if (!Server.groupMember.get(groupName).contains(socket)) {
                        Server.groupMember.get(groupName).add(socket);
                    }

                    // Persist membership first, then broadcast updated list
                    try {
                        Integer groupId = DatabaseManager.getGroupIdByName(groupName);
                        Integer myId = DatabaseManager.getUserIdByUsername(username);
                        if (groupId != null && myId != null) {
                            DatabaseManager.addGroupMember(groupId, myId);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    pw.println("[System] Joined group: " + groupName);
                    Server.broadcastGroupList(socket);
                    continue;
                }

                // Leave group command from the client command bar.
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

                    if (!Server.groupMember.get(group).contains(socket)) {
                        pw.println("You are not in this group");
                        continue;
                    }

                    Server.groupMember.get(group).remove(socket);

                    // Remove membership in DB first, then broadcast update
                    try {
                        Integer groupId = DatabaseManager.getGroupIdByName(group);
                        Integer myId = DatabaseManager.getUserIdByUsername(username);
                        if (groupId != null && myId != null) {
                            DatabaseManager.removeGroupMember(groupId, myId);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    pw.println("Left group: " + group);
                    Server.broadcastGroupList(socket);
                    continue;
                }

                // Delete message command must be handled before the generic /<group> route.
                if (message.startsWith("/delete ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length < 3) {
                        pw.println("Usage: /delete private <messageId> OR /delete group <messageId>");
                        continue;
                    }

                    String scope = parts[1].trim();
                    String idText = parts[2].trim();
                    try {
                        int id = Integer.parseInt(idText);
                        Integer myId = DatabaseManager.getUserIdByUsername(username);
                        boolean deleteForEveryone = false;
                        boolean ok = false;

                        if (scope.equalsIgnoreCase("private")) {
                            Integer senderId = DatabaseManager.getPrivateMessageSenderId(id);
                            if (senderId == null) {
                                pw.println("[System] Private message id not found: " + id);
                                continue;
                            }
                            deleteForEveryone = myId != null && senderId.equals(myId);
                            if (deleteForEveryone) {
                                ok = DatabaseManager.softDeletePrivateMessageById(id);
                            }
                        } else if (scope.equalsIgnoreCase("group")) {
                            Integer senderId = DatabaseManager.getGroupMessageSenderId(id);
                            if (senderId == null) {
                                pw.println("[System] Group message id not found: " + id);
                                continue;
                            }
                            deleteForEveryone = myId != null && senderId.equals(myId);
                            if (deleteForEveryone) {
                                ok = DatabaseManager.softDeleteGroupMessageById(id);
                            }
                        } else {
                            pw.println("[System] Usage: /delete private <messageId> OR /delete group <messageId>");
                            continue;
                        }

                        if (deleteForEveryone && ok) {
                            for (Socket s : Server.clients) {
                                try {
                                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                    out.println("[Deleted] " + scope.toLowerCase() + " " + id);
                                } catch (Exception e) { }
                            }
                            pw.println("[System] Message " + id + " deleted for everyone");
                        } else if (!deleteForEveryone) {
                            pw.println("[DeletedLocal] " + scope.toLowerCase() + " " + id);
                            pw.println("[System] Message " + id + " removed only for you");
                        } else {
                            pw.println("[System] Message id not found: " + id);
                        }
                    } catch (NumberFormatException ex) {
                        pw.println("[System] Invalid message id");
                    }
                    continue;
                }

                // Group chat command used by the client group tab.
                // Format: /groupName message
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

                    // CHECK: Verify socket is a member of the group
                    List<Socket> members = Server.groupMember.get(groupName);
                    if (members == null || !members.contains(socket)) {
                        pw.println("[System] You are not in this group");
                        continue;
                    }

                    String msg = message.substring(fistSpace + 1);
                    // Save group message to DB and broadcast with the generated id
                    try {
                        Integer groupId = DatabaseManager.getGroupIdByName(groupName);
                        Integer user_Id = DatabaseManager.getUserIdByUsername(username);
                        Integer msgId = null;
                        if (groupId != null && user_Id != null && DatabaseManager.isUserInGroup(user_Id, groupId)) {
                            msgId = DatabaseManager.saveGroupMessageReturnId(groupId, user_Id, msg);
                        }
                        String payload = username + ": " + msg + (msgId != null ? " [#" + msgId + "]" : "");
                        Server.broadcastToGroup(groupName, payload);
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
