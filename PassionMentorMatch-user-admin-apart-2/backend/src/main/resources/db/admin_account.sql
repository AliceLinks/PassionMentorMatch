-- Minimal admin_account and admin_token tables
USE mentor;
DROP TABLE IF EXISTS admin_token;
DROP TABLE IF EXISTS admin_account;

CREATE TABLE IF NOT EXISTS admin_account (
  id INT PRIMARY KEY AUTO_INCREMENT,
  password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(128) NOT NULL,
  expire_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin password (MD5 of '123456')
INSERT INTO admin_account (password_hash) VALUES ('e10adc3949ba59abbe56e057f20f883e');
