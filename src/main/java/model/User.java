package model;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private String status;  // ONLINE, OFFLINE
    private String createdAt;
    
    // Constructor 1: Dùng khi register
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.status = "OFFLINE";
    }
    
    // Constructor 2: Dùng khi load từ database
    public User(int id, String username, String password, String email, String status, String createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // Constructor 3: Dùng khi login
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getters & Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
