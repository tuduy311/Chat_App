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
- [x] Multi-tab chat UI cho private/group chat
- [x] Nút đóng tab riêng cho từng cuộc chat
- [x] Command bar cho lệnh hệ thống (/createGroup grName, /join grName, /leave grName, /history private userName, /history group grName, /delete private msgID, /delete group msgID, /grName msg, /msg userName msg)
- [x] Welcome tab nhận thông báo chung, online list, group list và phản hồi lệnh
- [x] Tự động mở lại tab private gần đây và load lịch sử khi đăng nhập lại
- [x] `/history private`, `/history group` đổ vào đúng tab tương ứng
- [x] Message delete (soft-delete) + client UI: message ids, `/delete` and right-click delete
- [x] Cho phép Enter gửi hoặc xuống dòng trong ô chat
- [x] Cho phép chèn emoji đơn giản trong ô chat
- [x] File gửi được lưu metadata vào DB và hiển thị lại trong lịch sử chat

Ghi chú ngắn:
- **Tự động phục hồi**: khi đăng nhập lại client sẽ tự mở các tab private gần đây và tự yêu cầu lịch sử tương ứng.
- **Xóa tin nhắn**: nếu bạn là người gửi ban đầu, xóa sẽ soft-delete ở DB và broadcast cho mọi client; nếu không phải người gửi, xóa chỉ ẩn cục bộ trên client của bạn.

### Database - ✅ Hoàn Thành
- [x] MySQL schema
- [x] User model & DatabaseManager
- [x] JDBC connection (.env auth)
- [x] Lưu user và nhóm chat vào database
- [x] Lưu lịch sử chat private/group

### Todo - ⏳
- [~] File transfer (đang triển khai — thiết kế tạm: Base64 chunk qua giao thức text)
- [ ] Server GUI
- [ ] Client server list config

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
```

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
MySQL JDBC Driver loaded successfully
✓ Database connection successful!
Server started on port 8080
Waiting for clients...
```

### Start Client GUI

Mở terminal thứ 2:

```powershell
mvn exec:java "-Dexec.mainClass=client.chatClientUI"
```

Trong client GUI:
- Gõ lệnh hệ thống như `/createGroup`, `/join`, `/leave`, `/history`, `/delete` ở ô `Command` phía trên danh sách tab.
- Gõ tin nhắn riêng hoặc tin nhắn nhóm trong ô nhập ở từng tab chat.
- Ô chat hỗ trợ nhiều dòng; bật/tắt `Enter sends` để chọn Enter là gửi hoặc xuống dòng.
- Có thanh emoji đơn giản để chèn nhanh 10 emoji cơ bản, gồm tim, vui, buồn và các biểu tượng phổ biến khác.
- `groupArea` chỉ là danh sách group, không phải nơi nhập lệnh.
- Tab `Welcome` dùng để hiện thông báo chung, trạng thái online/group, và phản hồi hệ thống.
- Có thể đóng từng tab chat bằng nút `x` ở tiêu đề tab.
- `/history private <username>` và `/history group <groupname>` sẽ hiển thị trong tab tương ứng.
- Khi đăng nhập lại, client sẽ tự mở lại các cuộc chat gần đây và nạp lịch sử cũ.
- File đã gửi sẽ được lưu metadata vào bảng `files` và cũng hiện lại trong `/history` như một dòng file riêng.

Test commands trong client:
Lệnh (cú pháp, ví dụ, hành vi)

```text
/msg <username> <message>
    - Gửi tin nhắn riêng tới một người dùng.
    - Ví dụ: `/msg alice Chào Alice!` → lưu vào DB và server trả về echo kèm id `[#123]`.

/createGroup <groupname>
    - Tạo nhóm mới và tự động tham gia; thông tin thành viên được lưu vào DB.
    - Ví dụ: `/createGroup devteam`

/join <groupname>
    - Tham gia vào nhóm đã có. Sau khi join, bạn mới có thể gửi tin cho nhóm đó.
    - Ví dụ: `/join devteam`

/leave <groupname>
    - Rời khỏi nhóm (xóa membership trong DB).
    - Ví dụ: `/leave devteam`

/<groupname> <message>
    - Gửi tin nhắn tới nhóm đã tham gia (viết tắt cho gửi nhóm).
    - Ví dụ: `/devteam Họp lúc 10h` → mọi thành viên nhận được `[devteam]: user: message [#id]`.

/history private <username>
    - Hiển thị lịch sử tin nhắn riêng giữa bạn và `<username>`. Các tin có id sẽ hiển thị `[#id]`.
    - Ví dụ: `/history private alice`

/history group <groupname>
    - Hiển thị lịch sử nhóm `<groupname>`; yêu cầu bạn là thành viên của nhóm. Lịch sử có kèm id.
    - Ví dụ: `/history group devteam`

/mygroups
    - Liệt kê các nhóm bạn đang tham gia (lấy từ DB).

/delete <id>
    - Xóa tin nhắn theo id. Hành vi:
        - Nếu bạn là người gửi ban đầu của tin nhắn, `/delete <id>` sẽ thực hiện soft-delete trên DB và server sẽ broadcast sự kiện xóa tới tất cả client.
        - Nếu bạn KHÔNG phải là người gửi, thao tác xóa chỉ áp dụng cục bộ trên client của bạn (không thay đổi DB hoặc ảnh hưởng tới người khác).
    - Ví dụ: `/delete 123`

UI: chuột phải vào dòng trong transcript → `Copy ID` hoặc `Delete Message`. `Copy ID` sẽ copy id số vào clipboard; `Delete Message` tương đương với `/delete <id>` (theo quyền như trên).

Ghi chú:
- Tin nhắn nhóm hiển thị dạng `[tênnhóm]: username: message [#id]`. Phần `[#id]` cần để server có thể xóa server-side.
- Nếu mục lịch sử không có `[#id]` thì không thể xóa trên server (chỉ ẩn cục bộ).
```

## Phát Triển Tiếp Theo

1. **Server GUI** - màn hình quản lý server và client connected
2. **Client server list config** - lưu danh sách server và chọn server để kết nối

