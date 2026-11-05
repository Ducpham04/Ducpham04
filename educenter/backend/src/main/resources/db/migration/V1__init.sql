-- EduCenter schema v1 (MySQL 8). Mọi bảng nghiệp vụ có tenant_id.

CREATE TABLE tenants (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  name              VARCHAR(150) NOT NULL,
  slug              VARCHAR(60)  NOT NULL,
  phone             VARCHAR(20),
  email             VARCHAR(150),
  address           VARCHAR(255),
  plan              VARCHAR(20)  NOT NULL DEFAULT 'TRIAL',
  status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  bank_code         VARCHAR(20),
  bank_account_no   VARCHAR(30),
  bank_account_name VARCHAR(100),
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  CONSTRAINT uk_tenants_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id  BIGINT       NOT NULL,
  code       VARCHAR(20)  NOT NULL,
  full_name  VARCHAR(120) NOT NULL,
  dob        DATE,
  gender     VARCHAR(10),
  phone      VARCHAR(20),
  email      VARCHAR(150),
  school     VARCHAR(150),
  level      VARCHAR(60),
  address    VARCHAR(255),
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  joined_at  DATE         NOT NULL,
  note       VARCHAR(1000),
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  CONSTRAINT uk_students_code UNIQUE (tenant_id, code),
  CONSTRAINT fk_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_students_tenant_status (tenant_id, status),
  INDEX idx_students_tenant_joined (tenant_id, joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id     BIGINT,
  email         VARCHAR(150) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name     VARCHAR(120) NOT NULL,
  phone         VARCHAR(20),
  role          VARCHAR(20)  NOT NULL,
  student_id    BIGINT,
  enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
  last_login_at DATETIME(6),
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  CONSTRAINT fk_users_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE SET NULL,
  INDEX idx_users_tenant_role (tenant_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT      NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_refresh_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE guardians (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id    BIGINT       NOT NULL,
  student_id   BIGINT       NOT NULL,
  full_name    VARCHAR(120) NOT NULL,
  relationship VARCHAR(30),
  phone        VARCHAR(20),
  email        VARCHAR(150),
  zalo_phone   VARCHAR(20),
  is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at   DATETIME(6)  NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  CONSTRAINT fk_guardians_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  INDEX idx_guardians_tenant_student (tenant_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE courses (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id      BIGINT       NOT NULL,
  code           VARCHAR(20)  NOT NULL,
  name           VARCHAR(150) NOT NULL,
  level          VARCHAR(60),
  total_sessions INT          NOT NULL DEFAULT 0,
  tuition_fee    DECIMAL(15,0) NOT NULL DEFAULT 0,
  description    VARCHAR(1000),
  active         BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     DATETIME(6)  NOT NULL,
  updated_at     DATETIME(6)  NOT NULL,
  CONSTRAINT uk_courses_code UNIQUE (tenant_id, code),
  CONSTRAINT fk_courses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rooms (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id  BIGINT      NOT NULL,
  name       VARCHAR(60) NOT NULL,
  capacity   INT         NOT NULL DEFAULT 20,
  active     BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_rooms_name UNIQUE (tenant_id, name),
  CONSTRAINT fk_rooms_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classes (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id    BIGINT       NOT NULL,
  course_id    BIGINT       NOT NULL,
  code         VARCHAR(30)  NOT NULL,
  name         VARCHAR(150) NOT NULL,
  teacher_id   BIGINT,
  room_id      BIGINT,
  max_students INT          NOT NULL DEFAULT 20,
  start_date   DATE         NOT NULL,
  end_date     DATE,
  status       VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
  note         VARCHAR(1000),
  created_at   DATETIME(6)  NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  CONSTRAINT uk_classes_code UNIQUE (tenant_id, code),
  CONSTRAINT fk_classes_course FOREIGN KEY (course_id) REFERENCES courses(id),
  CONSTRAINT fk_classes_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_classes_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
  INDEX idx_classes_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_schedules (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT  NOT NULL,
  class_id    BIGINT  NOT NULL,
  day_of_week INT     NOT NULL,
  start_time  TIME    NOT NULL,
  end_time    TIME    NOT NULL,
  CONSTRAINT fk_schedules_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
  INDEX idx_schedules_tenant_dow (tenant_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE enrollments (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id  BIGINT      NOT NULL,
  class_id   BIGINT      NOT NULL,
  student_id BIGINT      NOT NULL,
  start_date DATE        NOT NULL,
  end_date   DATE,
  status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_enroll_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
  CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  INDEX idx_enroll_class_status (class_id, status),
  INDEX idx_enroll_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_sessions (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id    BIGINT      NOT NULL,
  class_id     BIGINT      NOT NULL,
  session_date DATE        NOT NULL,
  start_time   TIME        NOT NULL,
  end_time     TIME        NOT NULL,
  room_id      BIGINT,
  teacher_id   BIGINT,
  status       VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  note         VARCHAR(500),
  created_at   DATETIME(6) NOT NULL,
  updated_at   DATETIME(6) NOT NULL,
  CONSTRAINT fk_sessions_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
  CONSTRAINT fk_sessions_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
  CONSTRAINT fk_sessions_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_sessions_tenant_date (tenant_id, session_date),
  INDEX idx_sessions_class_date (class_id, session_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendances (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id  BIGINT      NOT NULL,
  session_id BIGINT      NOT NULL,
  student_id BIGINT      NOT NULL,
  status     VARCHAR(20) NOT NULL,
  note       VARCHAR(255),
  marked_by  BIGINT,
  marked_at  DATETIME(6) NOT NULL,
  CONSTRAINT uk_attendance UNIQUE (session_id, student_id),
  CONSTRAINT fk_att_session FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
  CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  INDEX idx_att_tenant_student (tenant_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_plans (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT        NOT NULL,
  name        VARCHAR(150)  NOT NULL,
  course_id   BIGINT,
  amount      DECIMAL(15,0) NOT NULL,
  sessions    INT,
  description VARCHAR(500),
  active      BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  DATETIME(6)   NOT NULL,
  updated_at  DATETIME(6)   NOT NULL,
  CONSTRAINT fk_feeplans_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL,
  INDEX idx_feeplans_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoices (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id        BIGINT        NOT NULL,
  code             VARCHAR(30)   NOT NULL,
  student_id       BIGINT        NOT NULL,
  class_id         BIGINT,
  fee_plan_id      BIGINT,
  description      VARCHAR(255)  NOT NULL,
  subtotal         DECIMAL(15,0) NOT NULL,
  discount         DECIMAL(15,0) NOT NULL DEFAULT 0,
  total            DECIMAL(15,0) NOT NULL,
  paid_amount      DECIMAL(15,0) NOT NULL DEFAULT 0,
  due_date         DATE          NOT NULL,
  status           VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
  note             VARCHAR(500),
  created_by       BIGINT,
  last_reminded_at DATETIME(6),
  created_at       DATETIME(6)   NOT NULL,
  updated_at       DATETIME(6)   NOT NULL,
  CONSTRAINT uk_invoices_code UNIQUE (tenant_id, code),
  CONSTRAINT fk_invoices_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_invoices_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL,
  CONSTRAINT fk_invoices_plan FOREIGN KEY (fee_plan_id) REFERENCES fee_plans(id) ON DELETE SET NULL,
  INDEX idx_invoices_tenant_status_due (tenant_id, status, due_date),
  INDEX idx_invoices_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id   BIGINT        NOT NULL,
  invoice_id  BIGINT        NOT NULL,
  receipt_no  VARCHAR(30)   NOT NULL,
  amount      DECIMAL(15,0) NOT NULL,
  method      VARCHAR(20)   NOT NULL,
  paid_at     DATETIME(6)   NOT NULL,
  note        VARCHAR(255),
  received_by BIGINT,
  created_at  DATETIME(6)   NOT NULL,
  CONSTRAINT uk_payments_receipt UNIQUE (tenant_id, receipt_no),
  CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
  INDEX idx_payments_tenant_paid (tenant_id, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_logs (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id  BIGINT       NOT NULL,
  channel    VARCHAR(20)  NOT NULL,
  type       VARCHAR(30)  NOT NULL,
  recipient  VARCHAR(150) NOT NULL,
  subject    VARCHAR(255),
  status     VARCHAR(20)  NOT NULL,
  error      VARCHAR(500),
  ref_key    VARCHAR(100) NOT NULL,
  created_at DATETIME(6)  NOT NULL,
  INDEX idx_notif_tenant_created (tenant_id, created_at),
  INDEX idx_notif_ref (ref_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE code_sequences (
  tenant_id  BIGINT      NOT NULL,
  seq_key    VARCHAR(30) NOT NULL,
  next_value BIGINT      NOT NULL,
  PRIMARY KEY (tenant_id, seq_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
