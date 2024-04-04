CREATE TABLE IF NOT EXISTS customers (
    id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(255) PRIMARY KEY,
    sub_category_id VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(255) PRIMARY KEY,
    value DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    uuid VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    type VARCHAR(255) NOT NULL,
    customer_id INT,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS items (
    id INT,
    order_uuid VARCHAR(36),
    product_id VARCHAR(255),
    quantity INT NOT NULL,
    category_id VARCHAR(255),
    PRIMARY KEY (id, order_uuid),
    FOREIGN KEY (order_uuid) REFERENCES orders(uuid),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);