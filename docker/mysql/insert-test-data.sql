-- Insert test data (run this after Liquibase creates the tables)
USE store_db;

-- Insert test users
INSERT INTO user (login_name, password, name, phone, email, create_time) VALUES
('testuser1', MD5('password123'), 'Test User 1', '+1234567890', 'test1@example.com', NOW()),
('testuser2', MD5('password123'), 'Test User 2', '+1234567891', 'test2@example.com', NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert test merchants
INSERT INTO merchant (name, contact_name, contact_phone, status, create_time) VALUES
('Tech Store', 'John Doe', '+1234567890', 'ACTIVE', NOW()),
('Fashion Store', 'Jane Smith', '+1234567891', 'ACTIVE', NOW()),
('Book Store', 'Bob Johnson', '+1234567892', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert test products
INSERT INTO product (name, merchant_id, pic, price, stock, status, version, create_time) VALUES
('iPhone 15', 1, 'https://example.com/iphone15.jpg', 999.99, 50, 'ON_SALE', 0, NOW()),
('MacBook Pro', 1, 'https://example.com/macbook.jpg', 1999.99, 25, 'ON_SALE', 0, NOW()),
('Designer T-Shirt', 2, 'https://example.com/tshirt.jpg', 49.99, 100, 'ON_SALE', 0, NOW()),
('Programming Book', 3, 'https://example.com/book.jpg', 29.99, 200, 'ON_SALE', 0, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name); 