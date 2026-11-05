CREATE TABLE businesses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  slug VARCHAR(60) NOT NULL,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(1000),
  phone VARCHAR(20),
  email VARCHAR(150),
  address VARCHAR(255),
  logo_url VARCHAR(500),
  brand_color VARCHAR(7) NOT NULL DEFAULT '#2f7d6b',
  category VARCHAR(30) NOT NULL DEFAULT 'SPA',
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  payment_mode VARCHAR(20) NOT NULL DEFAULT 'NONE',
  deposit_percent INT NOT NULL DEFAULT 30,
  bank_bin VARCHAR(20),
  bank_account_no VARCHAR(30),
  bank_account_name VARCHAR(100),
  min_lead_minutes INT NOT NULL DEFAULT 60,
  booking_window_days INT NOT NULL DEFAULT 14,
  cancel_before_minutes INT NOT NULL DEFAULT 120,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_business_slug UNIQUE (slug)
);

CREATE TABLE branches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  address VARCHAR(255),
  phone VARCHAR(20),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_branch_business FOREIGN KEY (business_id) REFERENCES businesses (id)
);
CREATE INDEX idx_branch_business ON branches (business_id);

CREATE TABLE branch_hours (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  day_of_week INT NOT NULL,
  open_time TIME NOT NULL,
  close_time TIME NOT NULL,
  closed BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_hours_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
  CONSTRAINT uk_branch_day UNIQUE (branch_id, day_of_week)
);

CREATE TABLE holidays (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  branch_id BIGINT,
  holiday_date DATE NOT NULL,
  name VARCHAR(150) NOT NULL,
  CONSTRAINT fk_holiday_business FOREIGN KEY (business_id) REFERENCES businesses (id),
  CONSTRAINT fk_holiday_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
);
CREATE INDEX idx_holiday_business_date ON holidays (business_id, holiday_date);

CREATE TABLE service_categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_category_business FOREIGN KEY (business_id) REFERENCES businesses (id)
);
CREATE INDEX idx_category_business ON service_categories (business_id);

CREATE TABLE services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  category_id BIGINT,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(1000),
  duration_minutes INT NOT NULL,
  price BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_service_business FOREIGN KEY (business_id) REFERENCES businesses (id),
  CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES service_categories (id) ON DELETE SET NULL
);
CREATE INDEX idx_service_business ON services (business_id);

CREATE TABLE staff (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  title VARCHAR(100),
  phone VARCHAR(20),
  email VARCHAR(150),
  avatar_url VARCHAR(500),
  color VARCHAR(7) NOT NULL DEFAULT '#3b82f6',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_staff_business FOREIGN KEY (business_id) REFERENCES businesses (id),
  CONSTRAINT fk_staff_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);
CREATE INDEX idx_staff_business ON staff (business_id);

CREATE TABLE service_staff (
  service_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  PRIMARY KEY (service_id, staff_id),
  CONSTRAINT fk_ss_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE,
  CONSTRAINT fk_ss_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE
);

CREATE TABLE staff_shifts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT NOT NULL,
  day_of_week INT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  CONSTRAINT fk_shift_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE
);
CREATE INDEX idx_shift_staff ON staff_shifts (staff_id);

CREATE TABLE staff_time_off (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  staff_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(255),
  CONSTRAINT fk_timeoff_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE
);
CREATE INDEX idx_timeoff_staff ON staff_time_off (staff_id);

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT,
  staff_id BIGINT,
  email VARCHAR(150) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_user_email UNIQUE (email),
  CONSTRAINT fk_user_business FOREIGN KEY (business_id) REFERENCES businesses (id),
  CONSTRAINT fk_user_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE SET NULL
);

CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_refresh_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(150),
  notes VARCHAR(2000),
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_customer_phone UNIQUE (business_id, phone),
  CONSTRAINT fk_customer_business FOREIGN KEY (business_id) REFERENCES businesses (id)
);

CREATE TABLE bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  code VARCHAR(12) NOT NULL,
  customer_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  source VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
  price BIGINT NOT NULL,
  amount_due BIGINT NOT NULL DEFAULT 0,
  amount_paid BIGINT NOT NULL DEFAULT 0,
  payment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
  payment_expires_at DATETIME,
  customer_note VARCHAR(500),
  internal_note VARCHAR(1000),
  cancel_reason VARCHAR(255),
  cancelled_at DATETIME,
  reminder_24h_sent_at DATETIME,
  reminder_2h_sent_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_booking_code UNIQUE (code),
  CONSTRAINT fk_booking_business FOREIGN KEY (business_id) REFERENCES businesses (id),
  CONSTRAINT fk_booking_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
  CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_booking_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
  CONSTRAINT fk_booking_service FOREIGN KEY (service_id) REFERENCES services (id)
);
CREATE INDEX idx_booking_business_start ON bookings (business_id, start_at);
CREATE INDEX idx_booking_staff_start ON bookings (staff_id, start_at);
CREATE INDEX idx_booking_customer ON bookings (customer_id);
CREATE INDEX idx_booking_status_start ON bookings (status, start_at);

CREATE TABLE booking_slots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  slot_start DATETIME NOT NULL,
  CONSTRAINT uk_staff_slot UNIQUE (staff_id, slot_start),
  CONSTRAINT fk_slot_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
);
CREATE INDEX idx_slot_booking ON booking_slots (booking_id);

CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  booking_id BIGINT,
  provider VARCHAR(20) NOT NULL,
  provider_txn_id VARCHAR(100) NOT NULL,
  amount BIGINT NOT NULL,
  content VARCHAR(500),
  matched BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_payment_txn UNIQUE (provider, provider_txn_id),
  CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE SET NULL
);

CREATE TABLE notification_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  booking_id BIGINT,
  channel VARCHAR(20) NOT NULL,
  type VARCHAR(30) NOT NULL,
  recipient VARCHAR(150),
  status VARCHAR(20) NOT NULL,
  error VARCHAR(500),
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_notif_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
);
CREATE INDEX idx_notif_booking ON notification_logs (booking_id);
