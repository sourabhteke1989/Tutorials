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

-- Customers (UUID-based IDs)
INSERT INTO customers (id, first_name, last_name, email, phone, address, city, country, status) VALUES
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'John', 'Doe', 'john.doe@example.com', '+1-555-0101', '123 Main St', 'New York', 'USA', 'active'),
('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0102', '456 Oak Ave', 'Los Angeles', 'USA', 'active'),
('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f', 'Bob', 'Johnson', 'bob.j@example.com', '+44-20-7946-0958', '10 Downing St', 'London', 'UK', 'active'),
('d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80', 'Alice', 'Williams', 'alice.w@example.com', NULL, '78 Rue de Paris', 'Paris', 'France', 'inactive'),
('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091', 'Carlos', 'Garcia', 'carlos.g@example.com', '+34-91-123-4567', 'Calle Mayor 1', 'Madrid', 'Spain', 'active');

-- Tags
INSERT INTO tags (name, color) VALUES
('electronics', '#3498DB'),
('sale',        '#E74C3C'),
('new-arrival', '#2ECC71'),
('premium',     '#F39C12'),
('ergonomic',   '#9B59B6');

-- Product ↔ Tag associations (many-to-many)
INSERT INTO product_tags (product_id, tag_id) VALUES
(1, 1),  -- Wireless Mouse      → electronics
(1, 5),  -- Wireless Mouse      → ergonomic
(2, 1),  -- Mechanical Keyboard → electronics
(2, 4),  -- Mechanical Keyboard → premium
(3, 1),  -- USB-C Hub           → electronics
(3, 3),  -- USB-C Hub           → new-arrival
(4, 5),  -- Standing Desk Mat   → ergonomic
(5, 4),  -- Monitor Arm         → premium
(5, 5),  -- Monitor Arm         → ergonomic
(6, 1),  -- Webcam HD           → electronics
(6, 2),  -- Webcam HD           → sale
(7, 3),  -- Desk Lamp           → new-arrival
(8, 2),  -- Laptop Stand        → sale
(8, 3);  -- Laptop Stand        → new-arrival

-- Customer Profiles (one-to-one with customers)
INSERT INTO customer_profiles (customer_id, bio, avatar_url, loyalty_tier, total_orders) VALUES
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'Tech enthusiast and early adopter.', 'https://example.com/avatars/john.png', 'gold', 15),
('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 'Freelance designer who loves minimalism.', 'https://example.com/avatars/jane.png', 'silver', 8),
('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f', 'Software engineer based in London.', NULL, 'bronze', 3),
('d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80', 'Casual shopper.', NULL, 'bronze', 1),
('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091', 'Gamer and content creator.', 'https://example.com/avatars/carlos.png', 'platinum', 42);

-- Orders (one-to-many with customers)
INSERT INTO orders (customer_id, order_number, total_amount, status, shipping_address) VALUES
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'ORD-2026-0001', 129.97, 'delivered', '123 Main St, New York, USA'),
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'ORD-2026-0002', 89.99,  'shipped',   '123 Main St, New York, USA'),
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'ORD-2026-0003', 44.99,  'pending',   '123 Main St, New York, USA'),
('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 'ORD-2026-0004', 59.99,  'delivered', '456 Oak Ave, Los Angeles, USA'),
('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 'ORD-2026-0005', 159.98, 'shipped',   '456 Oak Ave, Los Angeles, USA'),
('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f', 'ORD-2026-0006', 49.99,  'delivered', '10 Downing St, London, UK'),
('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091', 'ORD-2026-0007', 209.97, 'delivered', 'Calle Mayor 1, Madrid, Spain'),
('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091', 'ORD-2026-0008', 34.99,  'pending',   'Calle Mayor 1, Madrid, Spain');
