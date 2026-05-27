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

### 4. Build Project

```powershell
cd ....\chat_app

# Cài dependencies & build
mvn clean install


### Database Connection Test

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
mvn exec:java "-Dexec.mainClass=db.TestConnection"
```

## Chạy Ứng Dụng

### Start Server

```powershell
mvn exec:java
# hoặc
mvn exec:java "-Dexec.mainClass=server.Server"
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
mvn exec:java "-Dexec.mainClass=client.chatClientUI"
```

Test commands trong client:
```
/msg username message      # Private message
/createGroup groupname     # Tạo group
/join groupname           # Join group
/leave groupname          # Leave group
/groupname message         # Gửi message vào group
 /history private username # Load recent private history with username
 /history group groupname  # Load recent group history
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
