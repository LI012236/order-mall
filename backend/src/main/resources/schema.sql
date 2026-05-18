-- 订单商城 建表脚本（幂等：可重复执行）
CREATE DATABASE IF NOT EXISTS mall_db DEFAULT CHARSET utf8mb4;
USE mall_db;

CREATE TABLE IF NOT EXISTS mall_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(32)  NOT NULL,
    password   VARCHAR(128) NOT NULL COMMENT 'sha256(salt + 明文)',
    salt       VARCHAR(32)  NOT NULL,
    nickname   VARCHAR(32),
    phone      VARCHAR(20),
    role       VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户';

CREATE TABLE IF NOT EXISTS product (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128)  NOT NULL,
    category    VARCHAR(32)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    stock       INT           NOT NULL,
    cover_url   VARCHAR(255),
    description TEXT,
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 上架 / 0 下架',
    version     INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category_status (category, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品';

CREATE TABLE IF NOT EXISTS cart (
    id         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    product_id BIGINT   NOT NULL,
    quantity   INT      NOT NULL,
    checked    TINYINT  NOT NULL DEFAULT 1 COMMENT '1 勾选 / 0 未勾选',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '购物车';

CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(40)   NOT NULL,
    user_id          BIGINT        NOT NULL,
    total_amount     DECIMAL(10,2) NOT NULL,
    status           VARCHAR(16)   NOT NULL COMMENT 'CREATED/PAID/SHIPPED/COMPLETED/CANCELLED',
    receiver_name    VARCHAR(32)   NOT NULL,
    receiver_phone   VARCHAR(20)   NOT NULL,
    receiver_address VARCHAR(255)  NOT NULL,
    request_id       VARCHAR(64)   NOT NULL COMMENT '幂等键',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at          DATETIME      NULL,
    shipped_at       DATETIME      NULL,
    completed_at     DATETIME      NULL,
    cancelled_at     DATETIME      NULL,
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_request_id (request_id),
    KEY idx_user_time (user_id, created_at),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单';

CREATE TABLE IF NOT EXISTS order_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(128)  NOT NULL COMMENT '下单快照',
    price        DECIMAL(10,2) NOT NULL COMMENT '下单快照',
    quantity     INT           NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单明细';

-- 演示商品（重复执行不会覆盖库存）
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (1, '机械键盘 87 键', '数码', 299.00, 100, '', '青轴，PBT 键帽')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (2, '无线鼠标', '数码', 99.00, 50, '', '2.4G 无线，静音微动')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (3, '4K 显示器 27 寸', '数码', 1299.00, 10, '', 'IPS 屏，Type-C 反向充电')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (4, '人体工学椅', '家居', 899.00, 20, '', '网布透气，四向头枕')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (5, '升降桌 1.2m', '家居', 699.00, 15, '', '双电机，记忆高度')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (6, '降噪耳机', '数码', 599.00, 30, '', '主动降噪，40 小时续航')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (7, 'USB-C 扩展坞', '配件', 199.00, 60, '', '十合一，4K@60Hz')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO product (id, name, category, price, stock, cover_url, description)
VALUES (8, '笔记本支架', '配件', 89.00, 80, '', '铝合金，六档调节')
ON DUPLICATE KEY UPDATE name = VALUES(name);
