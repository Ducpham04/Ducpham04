CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  zalo_id       VARCHAR(64)  NULL,
  email         VARCHAR(190) NULL,
  password_hash VARCHAR(100) NULL,
  name          VARCHAR(150) NOT NULL,
  avatar_url    VARCHAR(500) NULL,
  phone         VARCHAR(20)  NULL,
  role          VARCHAR(20)  NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  last_lat      DECIMAL(10,7) NULL,
  last_lng      DECIMAL(10,7) NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_users_zalo_id UNIQUE (zalo_id),
  CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  token_hash  VARCHAR(64) NOT NULL,
  expires_at  DATETIME(6) NOT NULL,
  revoked_at  DATETIME(6) NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE articles (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  title         VARCHAR(255) NOT NULL,
  slug          VARCHAR(255) NOT NULL,
  category      VARCHAR(100) NOT NULL,
  summary       VARCHAR(500) NULL,
  content       MEDIUMTEXT   NOT NULL,
  thumbnail_url VARCHAR(500) NULL,
  published     BOOLEAN      NOT NULL DEFAULT FALSE,
  published_at  DATETIME(6)  NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_articles_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_articles_pub ON articles(published, published_at);
CREATE INDEX idx_articles_category ON articles(category);

CREATE TABLE products (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  description TEXT NULL,
  price       BIGINT NOT NULL,
  image_url   VARCHAR(500) NULL,
  stock       INT NOT NULL DEFAULT 0,
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  featured    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  DATETIME(6) NOT NULL,
  updated_at  DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_products_active ON products(active, featured);

CREATE TABLE feedbacks (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(32) NULL,
  user_id     BIGINT NOT NULL,
  category    VARCHAR(100) NOT NULL,
  title       VARCHAR(150) NOT NULL,
  content     TEXT NOT NULL,
  address     VARCHAR(255) NULL,
  lat         DECIMAL(10,7) NULL,
  lng         DECIMAL(10,7) NULL,
  status      VARCHAR(20) NOT NULL,
  admin_reply TEXT NULL,
  created_at  DATETIME(6) NOT NULL,
  updated_at  DATETIME(6) NOT NULL,
  CONSTRAINT uk_feedbacks_code UNIQUE (code),
  CONSTRAINT fk_feedbacks_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_feedbacks_user ON feedbacks(user_id, created_at);
CREATE INDEX idx_feedbacks_status ON feedbacks(status, created_at);

CREATE TABLE feedback_images (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  feedback_id BIGINT NOT NULL,
  url         VARCHAR(500) NOT NULL,
  CONSTRAINT fk_feedback_images_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  code              VARCHAR(32) NOT NULL,
  user_id           BIGINT NOT NULL,
  amount            BIGINT NOT NULL,
  status            VARCHAR(20) NOT NULL,
  description       VARCHAR(255) NOT NULL,
  payment_method    VARCHAR(50) NULL,
  provider_order_id VARCHAR(100) NULL,
  provider_trans_id VARCHAR(100) NULL,
  paid_at           DATETIME(6) NULL,
  created_at        DATETIME(6) NOT NULL,
  updated_at        DATETIME(6) NOT NULL,
  CONSTRAINT uk_orders_code UNIQUE (code),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_orders_user ON orders(user_id, created_at);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id     BIGINT NOT NULL,
  product_id   BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  unit_price   BIGINT NOT NULL,
  quantity     INT NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_logs (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NULL,
  channel     VARCHAR(20) NOT NULL,
  template_id VARCHAR(100) NULL,
  recipient   VARCHAR(100) NULL,
  payload     TEXT NULL,
  status      VARCHAR(20) NOT NULL,
  error       VARCHAR(500) NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_notification_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_notification_logs_created ON notification_logs(created_at);
