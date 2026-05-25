package db;

public class TestConnection {
     public static void main(String[] args) {
        if (DatabaseManager.testConnection()) {
            System.out.println("✓ MySQL connection OK!");
        } else {
            System.out.println("✗ Connection failed!");
        }
    }
}
