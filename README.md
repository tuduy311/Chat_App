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
- **Database**: MySQL
- **Data Format**: JSON 
- **Networking**: TCP Socket
- **Architecture**: Client - Server

## Cấu trúc dự án

```

ChatApp/
├── src/
│   ├── main/
│   │   ├── java/
│   │       ├── server/        # Server (Socket, Thread, ClientHandler)
│   │       ├── client/        # Client connection logic
│   │       ├── model/        # DTO (User, Message,...)
│   │       ├── service/      # Business logic (Auth, ChatService,...)
│   │       ├── db/           # JDBC / Database connection
│   │       └── util/         # Constants, helper, enums
│   │
│   └── test/                     # test code (optional)
│
├── pom.xml                       # Maven config
├── README.md                     # project description
├── .gitignore                    # ignore file

```