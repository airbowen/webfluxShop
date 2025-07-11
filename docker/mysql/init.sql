-- Create database if not exists
CREATE DATABASE IF NOT EXISTS store_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE store_db;

-- Create user and grant permissions
CREATE USER IF NOT EXISTS 'store_user'@'%' IDENTIFIED BY 'store_password';
GRANT ALL PRIVILEGES ON store_db.* TO 'store_user'@'%';
FLUSH PRIVILEGES; 