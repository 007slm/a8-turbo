CREATE DATABASE IF NOT EXISTS `shopdb`;
USE `shopdb`;

-- 基于 JPA Entity 自动生成策略定义的表结构
-- User Entity
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(255),
    `email` VARCHAR(255),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Product Entity
CREATE TABLE IF NOT EXISTS `products` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255),
    `price` DECIMAL(19,2),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Order Entity
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT,
    `order_date` DATETIME(6),
    PRIMARY KEY (`id`),
    CONSTRAINT `FK_order_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- OrderItem Entity
CREATE TABLE IF NOT EXISTS `order_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `quantity` INT NOT NULL,
    `order_id` BIGINT,
    `product_id` BIGINT,
    PRIMARY KEY (`id`),
    CONSTRAINT `FK_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `FK_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Review Entity
CREATE TABLE IF NOT EXISTS `reviews` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rating` INT NOT NULL,
    `comment` TEXT,
    `product_id` BIGINT,
    `user_id` BIGINT,
    PRIMARY KEY (`id`),
    CONSTRAINT `FK_review_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
    CONSTRAINT `FK_review_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化数据
-- Users
INSERT INTO `users` (`username`, `email`) VALUES 
('alice', 'alice@example.com'),
('bob', 'bob@example.com'),
('charlie', 'charlie@example.com');

-- Products
INSERT INTO `products` (`name`, `price`) VALUES 
('Laptop', 999.99),
('Smartphone', 499.50),
('Headphones', 79.99),
('Keyboard', 49.99),
('Mouse', 25.00);

-- Orders
INSERT INTO `orders` (`user_id`, `order_date`) VALUES 
(1, NOW()), -- Alice's order
(2, NOW()); -- Bob's order

-- Order Items
-- Alice bought a Laptop and a Mouse
INSERT INTO `order_items` (`order_id`, `product_id`, `quantity`) VALUES 
(1, 1, 1),
(1, 5, 1);

-- Bob bought a Smartphone
INSERT INTO `order_items` (`order_id`, `product_id`, `quantity`) VALUES 
(2, 2, 1);

-- Reviews
INSERT INTO `reviews` (`user_id`, `product_id`, `rating`, `comment`) VALUES 
(1, 1, 5, 'Great laptop, very fast!'),
(2, 2, 4, 'Good phone but battery life could be better.');

