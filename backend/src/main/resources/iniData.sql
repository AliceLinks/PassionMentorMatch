SET NAMES utf8mb4;

-- 如果你的 MySQL 客户端已选择数据库，可移除下一行
USE mentor;

-- 管理员账号（幂等）
-- 默认密码：admin123，MD5 加密
INSERT IGNORE INTO admins (username, phone, password_hash, role, status)
VALUES 
  ('admin',  '13000000001', '0192023a7bbd73250516f069df18b500', 'admin', 'active'),
  ('admin2', '13000000002', '0192023a7bbd73250516f069df18b500', 'admin', 'active');

-- 用户（幂等）
INSERT IGNORE INTO users (openid, nickname, avatar, status)
VALUES 
  ('wx_openid_u1', 'lyp', 'https://ts4.tc.mm.bing.net/th/id/OIP-C.ypdU5KK5cdz6SE1YDH2BiwAAAA?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3', 'active'),
  ('wx_openid_u2', '小红', 'https://ts4.tc.mm.bing.net/th/id/OIP-C.ypdU5KK5cdz6SE1YDH2BiwAAAA?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3', 'active');

-- 取用户IDs
SET @u1 := (SELECT id FROM users WHERE openid='wx_openid_u1');
SET @u2 := (SELECT id FROM users WHERE openid='wx_openid_u2');

-- 持久化 Token（30 天）
INSERT IGNORE INTO user_token (user_id, token, expired_at)
VALUES 
  (@u1, 'token_user1', DATE_ADD(NOW(), INTERVAL 30 DAY)),
  (@u2, 'token_user2', DATE_ADD(NOW(), INTERVAL 30 DAY));

-- 导师卡（u1 有效学期卡；u2 未激活）
INSERT IGNORE INTO cards (user_id, card_number, card_type, status, start_date, end_date, created_at, updated_at)
VALUES
  (@u1, 'CARD-SEM-001', 'semester', 'active', DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), NOW(), NOW()),
  (@u2, 'CARD-SEM-002', 'semester', 'inactive', NULL, NULL, NOW(), NOW());

-- Optional: issue a lifetime card for user id=1
INSERT INTO cards (user_id, card_number, card_type, status, start_date, end_date, created_at, updated_at)
SELECT 1, 'CARD-DEMO-0001', 'lifetime', 'active', NULL, NULL, NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM cards WHERE user_id = 1 AND status = 'active'
);

-- 未来一周的课程（幂等）
INSERT IGNORE INTO courses (course_date, start_time, end_time, teacher, dance_type, capacity, current_participants, status, created_at, updated_at)
VALUES
  (DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00', '21:00:00', 'Teacher A', 'HipHop', 25, 0, 'published', NOW(), NOW()),
  (DATE_ADD(CURDATE(), INTERVAL 3 DAY), '19:00:00', '21:00:00', 'Teacher B', 'Popping', 20, 0, 'published', NOW(), NOW());

-- 取课程ID
SET @c1 := (
  SELECT id FROM courses 
  WHERE course_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND teacher='Teacher A' AND start_time='19:00:00'
  LIMIT 1
);
SET @c2 := (
  SELECT id FROM courses 
  WHERE course_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY) AND teacher='Teacher B' AND start_time='19:00:00'
  LIMIT 1
);

-- 预约（u1 预约第一个课程，幂等）
INSERT IGNORE INTO reservations (user_id, course_id, status, checkin_status)
SELECT @u1, @c1, 'reserved', 'not_checked'
WHERE @u1 IS NOT NULL AND @c1 IS NOT NULL;

-- 同步课程当前人数
UPDATE courses c
SET current_participants = (
  SELECT COUNT(*) FROM reservations r WHERE r.course_id = c.id AND r.status = 'reserved'
)
WHERE c.id IN (@c1, @c2);

-- 额外课程：用于时间冲突与满员校验
INSERT IGNORE INTO courses (course_date, start_time, end_time, teacher, dance_type, capacity, current_participants, status, created_at, updated_at)
VALUES
  (DATE_ADD(CURDATE(), INTERVAL 1 DAY), '20:00:00', '22:00:00', 'Teacher A', 'HipHop', 25, 0, 'published', NOW(), NOW()), -- 与 @c1 冲突（同一天 19-21 vs 20-22）
  (DATE_ADD(CURDATE(), INTERVAL 2 DAY), '18:00:00', '19:00:00', 'Teacher C', 'Jazz', 1, 0, 'published', NOW(), NOW()); -- 小容量课程，用于满员

SET @c_conflict := (
  SELECT id FROM courses 
  WHERE course_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND teacher='Teacher A' AND start_time='20:00:00'
  LIMIT 1
);
SET @c_full := (
  SELECT id FROM courses 
  WHERE course_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND teacher='Teacher C' AND start_time='18:00:00'
  LIMIT 1
);

-- 让小容量课程达到满员：u1 占位
INSERT IGNORE INTO reservations (user_id, course_id, status, checkin_status)
SELECT @u1, @c_full, 'reserved', 'not_checked'
WHERE @u1 IS NOT NULL AND @c_full IS NOT NULL;

-- 同步课程当前人数（包含新增）
UPDATE courses c
SET current_participants = (
  SELECT COUNT(*) FROM reservations r WHERE r.course_id = c.id AND r.status = 'reserved'
)
WHERE c.id IN (@c1, @c2, @c_conflict, @c_full);

-- 本周更多课程（多舞种），便于前端演示周视图
INSERT IGNORE INTO courses (course_date, start_time, end_time, teacher, dance_type, capacity, current_participants, status, created_at, updated_at)
VALUES
  (DATE_ADD(CURDATE(), INTERVAL 0 DAY), '18:30:00', '20:00:00', 'Teacher D', 'Breaking', 22, 0, 'published', NOW(), NOW()),
  (DATE_ADD(CURDATE(), INTERVAL 2 DAY), '20:00:00', '21:30:00', 'Teacher E', 'Jazz', 18, 0, 'published', NOW(), NOW()),
  (DATE_ADD(CURDATE(), INTERVAL 4 DAY), '19:00:00', '20:30:00', 'Teacher F', 'Locking', 20, 0, 'published', NOW(), NOW()),
  (DATE_ADD(CURDATE(), INTERVAL 5 DAY), '18:00:00', '19:30:00', 'Teacher G', 'Waacking', 16, 0, 'published', NOW(), NOW());

-- 同步新增课程的当前人数
UPDATE courses c
SET current_participants = (
  SELECT COUNT(*) FROM reservations r WHERE r.course_id = c.id AND r.status = 'reserved'
)
WHERE c.course_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 0 DAY) AND DATE_ADD(CURDATE(), INTERVAL 7 DAY);
