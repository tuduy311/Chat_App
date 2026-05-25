-- Create chat_app database
-- DROP DATABASE IF EXISTS chat_app;
CREATE DATABASE IF NOT EXISTS chat_app;
USE chat_app;

-- Drop existing tables if they exist
DROP TABLE IF EXISTS chat_history;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS group_messages;
DROP TABLE IF EXISTS group_members;
DROP TABLE IF EXISTS `groups`;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS users;

-- Users table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    status ENUM('ONLINE', 'OFFLINE') DEFAULT 'OFFLINE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Private messages
CREATE TABLE messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);

-- Groups (escaped with backticks because 'groups' is a reserved keyword)
CREATE TABLE `groups` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(255) UNIQUE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Group members (escaped because 'group' is a reserved keyword)
CREATE TABLE `group_members` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `groups`(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Group messages (escaped because 'group' is a reserved keyword)
CREATE TABLE `group_messages` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    user_id INT NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `groups`(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Files
CREATE TABLE files (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT,
    group_id INT,
    file_path VARCHAR(255) NOT NULL,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    FOREIGN KEY (group_id) REFERENCES `groups`(id)
);

-- Chat history
CREATE TABLE chat_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id INT,
    user_id INT NOT NULL,
    message_text TEXT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
