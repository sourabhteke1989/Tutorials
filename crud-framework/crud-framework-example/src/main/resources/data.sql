-- ============================================================
-- Sample data for CRUD Framework Example Application
-- ============================================================

-- Products
INSERT INTO products (name, description, price, category, status, stock_quantity) VALUES
('Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 'Electronics', 'active', 150),
('Mechanical Keyboard', 'RGB mechanical keyboard with Cherry MX switches', 89.99, 'Electronics', 'active', 75),
('USB-C Hub', '7-in-1 USB-C hub with HDMI and ethernet', 49.99, 'Electronics', 'active', 200),
('Standing Desk Mat', 'Anti-fatigue standing desk mat', 39.99, 'Furniture', 'active', 50),
('Monitor Arm', 'Adjustable single monitor arm clamp', 119.99, 'Furniture', 'active', 30),
('Webcam HD', '1080p HD webcam with microphone', 59.99, 'Electronics', 'inactive', 0),
('Desk Lamp', 'LED desk lamp with adjustable brightness', 34.99, 'Furniture', 'active', 100),
('Laptop Stand', 'Aluminum laptop stand with ventilation', 44.99, 'Accessories', 'active', 80);

-- Customers
INSERT INTO customers (first_name, last_name, email, phone, address, city, country, status) VALUES
('John', 'Doe', 'john.doe@example.com', '+1-555-0101', '123 Main St', 'New York', 'USA', 'active'),
('Jane', 'Smith', 'jane.smith@example.com', '+1-555-0102', '456 Oak Ave', 'Los Angeles', 'USA', 'active'),
('Bob', 'Johnson', 'bob.j@example.com', '+44-20-7946-0958', '10 Downing St', 'London', 'UK', 'active'),
('Alice', 'Williams', 'alice.w@example.com', NULL, '78 Rue de Paris', 'Paris', 'France', 'inactive'),
('Carlos', 'Garcia', 'carlos.g@example.com', '+34-91-123-4567', 'Calle Mayor 1', 'Madrid', 'Spain', 'active');
