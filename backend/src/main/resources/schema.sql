-- 数据库：南京大学 Passion 街舞社导师课预约系统
-- MySQL 8+ / InnoDB / UTF8MB4

CREATE DATABASE IF NOT EXISTS `mentor` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mentor`;

-- 清理旧表（如存在）
DROP TABLE IF EXISTS checkin_log;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS cards;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS user_token;
DROP TABLE IF EXISTS users;

-- 用户表（微信用户）
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  openid VARCHAR(128) UNIQUE NOT NULL,
  nickname VARCHAR(64),
  real_name VARCHAR(64),
  phone VARCHAR(32),
  avatar VARCHAR(255),
  status ENUM('active','disabled') DEFAULT 'active',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 管理员表
CREATE TABLE admins (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('operator','admin','superadmin') DEFAULT 'operator',
  status ENUM('active','disabled') DEFAULT 'active',
  last_login DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户登录持久化 Token
CREATE TABLE user_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(64) NOT NULL,
  expired_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_token (token),
  INDEX idx_user_id (user_id),
  CONSTRAINT fk_user_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 导师卡表
CREATE TABLE cards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  card_number VARCHAR(64) UNIQUE NOT NULL,
  card_type ENUM('semester','annual','lifetime') NOT NULL,
  status ENUM('active','inactive','revoked') DEFAULT 'active',
  start_date DATE,
  end_date DATE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_user_id (user_id),
  INDEX idx_card_type (card_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程表
CREATE TABLE courses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  teacher VARCHAR(128),
  dance_type VARCHAR(64),
  capacity INT NOT NULL DEFAULT 20,
  current_participants INT NOT NULL DEFAULT 0,
  status ENUM('draft','published','cancelled') DEFAULT 'published',
  week_number INT GENERATED ALWAYS AS (WEEK(course_date, 1)) STORED,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_course_date (course_date),
  INDEX idx_teacher (teacher),
  INDEX idx_dance_type (dance_type),
  INDEX idx_course_date_time (course_date, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 预约表
CREATE TABLE reservations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  status ENUM('reserved','cancelled','completed') DEFAULT 'reserved',
  checkin_status ENUM('not_checked','checked_in') DEFAULT 'not_checked',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_reservation_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
  UNIQUE KEY uniq_user_course (user_id, course_id),
  INDEX idx_course_id (course_id),
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 签到日志（管理员核销）
CREATE TABLE checkin_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  admin_id BIGINT NOT NULL,
  method ENUM('scan','manual') DEFAULT 'manual',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_checkin_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  CONSTRAINT fk_checkin_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程批次（可选：用于按周批量发布/复制）
CREATE TABLE IF NOT EXISTS course_batches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  week_start DATE NOT NULL,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 操作日志（可选）
CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  admin_id BIGINT,
  action VARCHAR(255),
  detail JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 视图或约束说明：
-- 1) 有效卡校验：后端逻辑中检查 status='active' 且 (card_type='lifetime' 或 start_date<=today<=end_date)。
-- 2) 时间冲突检查：查询 reservations 与 courses 判断同日时间段重叠（NOT (end_time <= new.start_time OR start_time >= new.end_time)）。
-- 3) 事务预约：SELECT … FOR UPDATE 锁课程行，判容量，再插入预约并更新计数。