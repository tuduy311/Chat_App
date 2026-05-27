package db;

import model.User;
import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // Load .env file
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    
    // Database connection details for MySQL (from .env file)
    private static final String DB_URL = dotenv.get("DB_URL", "jdbc:mysql://localhost:3306/chat_app");
    private static final String DB_USER = dotenv.get("DB_USER", "root");
    private static final String DB_PASSWORD = dotenv.get("DB_PASSWORD", "123456");
    
    // Load JDBC Driver
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }
    
    // Get database connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    // Test connection
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✓ Database connection successful!");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("✗ Database connection failed!");
            e.printStackTrace();
        }
        return false;
    }
    
    // Register new user
    public static boolean registerUser(String username, String password, String email) {
        String query = "INSERT INTO users (username, password, email, status) VALUES (?, ?, ?, 'OFFLINE')";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, email);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Register error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Login user - verify username and password
    public static User loginUser(String username, String password) {
        String query = "SELECT id, username, password, email, status, created_at FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // Check if username exists
    public static boolean usernameExists(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);          
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Update user status
    public static boolean updateUserStatus(int userId, String status) {
        String query = "UPDATE users SET status = ? WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, status);
            ps.setInt(2, userId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Get user by ID
    public static User getUserById(int userId) {
        String query = "SELECT id, username, password, email, status, created_at FROM users WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get user id by username
    public static Integer getUserIdByUsername(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get or create group id by name
    public static Integer getOrCreateGroupId(String groupName) {
        String select = "SELECT id FROM `groups` WHERE group_name = ?";
        String insert = "INSERT INTO `groups` (group_name) VALUES (?)";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, groupName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }

            try (PreparedStatement ps2 = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps2.setString(1, groupName);
                int rows = ps2.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = ps2.getGeneratedKeys()) {
                        if (keys.next()) return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Save private message
    public static boolean savePrivateMessage(int senderId, int receiverId, String message) {
        String query = "INSERT INTO messages (sender_id, receiver_id, message) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, message);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Save group message
    public static boolean saveGroupMessage(int groupId, int userId, String message) {
        String query = "INSERT INTO group_messages (group_id, user_id, message) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.setString(3, message);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get private chat history between two users (most recent first)
    public static List<String> getPrivateHistory(int userAId, int userBId, int limit) {
        String query = "SELECT sender_id, message, created_at FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY created_at DESC LIMIT ?";
        List<String> out = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userAId);
            ps.setInt(2, userBId);
            ps.setInt(3, userBId);
            ps.setInt(4, userAId);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int senderId = rs.getInt("sender_id");
                    String msg = rs.getString("message");
                    String senderName = getUserById(senderId) != null ? getUserById(senderId).getUsername() : "unknown";
                    out.add(senderName + ": " + msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    // Get group chat history (most recent first)
    public static List<String> getGroupHistory(int groupId, int limit) {
        String query = "SELECT user_id, message, created_at FROM group_messages WHERE group_id = ? ORDER BY created_at DESC LIMIT ?";
        List<String> out = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, groupId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String msg = rs.getString("message");
                    String senderName = getUserById(userId) != null ? getUserById(userId).getUsername() : "unknown";
                    out.add(senderName + ": " + msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}