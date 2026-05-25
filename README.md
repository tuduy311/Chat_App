# Chat Application 

## Mục tiêu
Xây dựng 1 chương trình chat với Java với các chức năng sau:
- Đăng ký chat user (đăng ký từ ứng dụng client), đăng nhập sau đăng ký.
- Chương trình cho phép một user có thể chat với nhiều user khác (đang online) cùng lúc.
- Chương trình cho phép user tạo các group chat và chat trong các group này.
- Cho phép gởi file trong khi chat.
- Cho người dùng xem lịch sử chat của mình, xoá các dòng lịch sử chat.

## Công nghệ
- **Backend**: Java Core (Socket Programming, Multithreading)
- **Frontend (GUI)**: Java Swing
- **Database**: MySQL 8.0+
- **Data Format**: JSON 
- **Networking**: TCP Socket (Port 8080)
- **Architecture**: Client - Server
- **Build Tool**: Maven 3.6+
- **Configuration**: .env (environment variables)

## Cấu trúc dự án

```
chat_app/
├── src/main/java/
│   ├── server/
│   │   ├── Server.java           # Main server socket (port 8080)
│   │   └── ClientHandler.java    # Thread per client
│   │
│   ├── client/
│   │   ├── Client.java           # Console client
│   │   └── chatClientUI.java     # Swing GUI client
│   │
│   ├── model/
│   │   └── User.java             # User data model
│   │
│   └── db/
│       └── DatabaseManager.java  # MySQL JDBC queries
│
├── database/
│   └── schema.sql                # MySQL schema
│
├── pom.xml                        # Maven configuration
├── .env                          # Environment variables (private)
├── .env.example                  # Template for .env
└── README.md                     # This file
```

## Tiến Độ Hiện Tại
### Core Features - ✅ Hoàn Thành
- [x] Socket Server + Multithreading
- [x] Online users broadcast
- [x] Private messages (/msg)
- [x] Group chat (create/join/leave/send)
- [x] Swing GUI (online users, groups, chat area)

### Database - ✅ Hoàn Thành
- [x] MySQL schema
- [x] User model & DatabaseManager
- [x] JDBC connection (.env auth)

### Todo - ⏳
- [ ] LoginFrame UI
- [ ] DB user authentication
- [ ] Chat history retrieval
- [ ] File transfer

## Setup

### 1. Yêu cầu
- Java 11+
- Maven 3.6+
- MySQL 8.0+
- MySQL Workbench (optional, GUI)

### 2. Tạo Database trong MySQL

Mở **MySQL Workbench** hoặc command line:

```sql
-- Mở Workbench > Click "+" > Execute script
-- Chọn file: database/schema.sql
```

Hoặc dùng command line:

```powershell
mysql -u root -p < database\schema.sql
-- Nhập password khi được yêu cầu
```

### 3. Cấu hình Environment (.env)

Copy `.env.example` thành `.env`:

```powershell
copy .env.example .env
```

Edit `.env` file với MySQL credentials:

```env
DB_URL=jdbc:mysql://localhost:3306/chat_app
DB_USER=root
DB_PASSWORD=your_mysql_password
```

**⚠️ QUAN TRỌNG**: 
- `.env` được thêm vào `.gitignore` - không commit password!
- Mỗi developer có `.env` riêng với credentials của họ

### 4. Build Project

```powershell
cd C:\Users\tuduy\Documents\Java\chat_app

# Cài dependencies & build
mvn clean install

# Build mà không test
mvn clean install -DskipTests
```

## Chạy Ứng Dụng

### Start Server

```powershell
mvn exec:java
# hoặc
mvn exec:java -Dexec.mainClass="server.Server"
```

Output khi thành công:
```
SQL Server JDBC Driver loaded successfully
✓ Database connection successful!
Server started on port 8080
Waiting for clients...
```

### Start Client GUI

Mở terminal thứ 2:

```powershell
mvn exec:java -Dexec.mainClass="client.chatClientUI"
```

## Test Commands

### 1. Build & Compile Test
```powershell
# Compile mã
mvn compile

# Check syntax
mvn validate
```

### 2. Package Test
```powershell
# Tạo JAR file
mvn package

# JAR file ở target/chat_app-1.0.jar
```

### 3. Database Connection Test

Tạo file test:

```java
// src/main/java/db/TestConnection.java
public class TestConnection {
    public static void main(String[] args) {
        if (DatabaseManager.testConnection()) {
            System.out.println("✓ MySQL connection OK!");
        } else {
            System.out.println("✗ Connection failed!");
        }
    }
}
```

Chạy:
```powershell
mvn exec:java -Dexec.mainClass="db.TestConnection"
```

### 4. Unit Test
```powershell
# Chạy tất cả tests
mvn test

# Chạy test class cụ thể
mvn test -Dtest=ServerTest
```

### 5. Integration Test (Manual)
```powershell
# Terminal 1: Start server
mvn exec:java

# Terminal 2: Start client 1
mvn exec:java -Dexec.mainClass="client.chatClientUI"

# Terminal 3: Start client 2
mvn exec:java -Dexec.mainClass="client.chatClientUI"
```

Test commands trong client:
```
/msg username message      # Private message
/createGroup groupname     # Tạo group
/join groupname           # Join group
/leave groupname          # Leave group
groupname message         # Gửi message vào group
```

### 6. Build Jar & Run Standalone
```powershell
# Build
mvn clean package

# Run server từ JAR
java -cp target/chat_app-1.0.jar server.Server

# Run client từ JAR
java -cp target/chat_app-1.0.jar client.chatClientUI
```

### 7. Clean Build
```powershell
# Xóa compiled files
mvn clean

# Clean + rebuild
mvn clean install
```

## Troubleshooting

### 1. MySQL Connection Failed
```
✗ Database connection failed!
com.mysql.cj.jdbc.exceptions.CommunicationsException
```

**Fix**:
- Kiểm tra MySQL đang chạy: `mysql -u root -p`
- Kiểm tra `.env` file có password đúng không
- Kiểm tra database `chat_app` đã tạo chưa

### 2. JDBC Driver Not Found
```
SQL Server JDBC Driver not found!
```

**Fix**:
```powershell
mvn clean install
```

### 3. Port 8080 Already in Use
```
Address already in use
```

**Fix**:
```powershell
# Windows: Tìm process dùng port 8080
netstat -ano | findstr :8080

# Kill process (thay PID)
taskkill /PID 1234 /F

# Hoặc sửa port trong Server.java
private static final int PORT = 8081;
```

### 4. Class Not Found
```
[ERROR] Unknown lifecycle phase ".mainClass=server.Server"
```

**Fix**: Sử dụng đúng syntax
```powershell
# Đúng
mvn exec:java -Dexec.mainClass="server.Server"

# Sai
mvn -Dexec.mainClass="server.Server" exec:java
```

## Phát Triển Tiếp Theo

1. **LoginFrame** - UI cho Register/Login
2. **Database Integration** - Lưu user vào DB
3. **Chat History** - Lấy lịch sử từ DB
4. **File Transfer** - Share files trong chat
5. **Encryption** - Mã hóa messages

## Git Workflow

```powershell
# Sau khi cấu hình .env
git add .
git commit -m "Setup: MySQL + .env configuration"
git push origin main

# Commit code changes (không commit .env!)
git add src/
git commit -m "Feature: Add login authentication"
git push
```
