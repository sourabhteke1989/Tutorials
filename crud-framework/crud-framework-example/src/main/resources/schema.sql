-- ============================================================
-- Schema for CRUD Framework Example Application
-- ============================================================

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(2000),
    price       DECIMAL(10, 2) NOT NULL,
    category    VARCHAR(100)   NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'active',
    stock_quantity INT         NOT NULL DEFAULT 0,
    deleted     BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Customers table (UUID primary key)
CREATE TABLE IF NOT EXISTS customers (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    first_name  VARCHAR(100)  NOT NULL,
    last_name   VARCHAR(100)  NOT NULL,
    email       VARCHAR(255)  NOT NULL UNIQUE,
    phone       VARCHAR(20),
    address     VARCHAR(500),
    city        VARCHAR(100),
    country     VARCHAR(100)  NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'active',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);

-- Tags table
CREATE TABLE IF NOT EXISTS tags (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL UNIQUE,
    color       VARCHAR(7),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Junction table: Products ↔ Tags (many-to-many)
CREATE TABLE IF NOT EXISTS product_tags (
    product_id  BIGINT       NOT NULL,
    tag_id      BIGINT       NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (tag_id)     REFERENCES tags(id)
);

CREATE INDEX IF NOT EXISTS idx_product_tags_product ON product_tags(product_id);
CREATE INDEX IF NOT EXISTS idx_product_tags_tag ON product_tags(tag_id);

-- Customer Profiles table (one-to-one with customers)
CREATE TABLE IF NOT EXISTS customer_profiles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(36)   NOT NULL UNIQUE,
    bio         VARCHAR(1000),
    avatar_url  VARCHAR(500),
    loyalty_tier VARCHAR(20)  NOT NULL DEFAULT 'bronze',
    total_orders INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Orders table (one-to-many with customers)
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     VARCHAR(36)    NOT NULL,
    order_number    VARCHAR(50)    NOT NULL UNIQUE,
    total_amount    DECIMAL(10, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'pending',
    shipping_address VARCHAR(500),
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX IF NOT EXISTS idx_customer_profiles_customer ON customer_profiles(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
