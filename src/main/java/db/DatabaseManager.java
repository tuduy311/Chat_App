package db;

import model.User;
import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}