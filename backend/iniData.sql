
--初始化（？仅供参考
USE mentor;

-- 用户（幂等）
INSERT INTO users (openid, nickname, avatar, status)
VALUES 
  ('wx_openid_u1', 'lyp', 'https://example.com/u1.png', 'active'),
  ('wx_openid_u2', '小红', 'https://example.com/u2.png', 'active')
ON DUPLICATE KEY UPDATE openid = VALUES(openid);

-- 取用户ID
SET @u1 := (SELECT id FROM users WHERE openid='wx_openid_u1');
SET @u2 := (SELECT id FROM users WHERE openid='wx_openid_u2');

-- 管理员（演示用，密码随意放置占位）
INSERT INTO admins (username, password_hash, role, status)
VALUES ('admin', '$2a$10$demo.demo.demo.demo.demo.demo.demo.demo.demo.demo', 'admin', 'active')
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 持久化 Token（30 天）
INSERT INTO user_token (user_id, token, expired_at)
VALUES 
  (@u1, 'token_user1', DATE_ADD(NOW(), INTERVAL 30 DAY)),
  (@u2, 'token_user2', DATE_ADD(NOW(), INTERVAL 30 DAY))
ON DUPLICATE KEY UPDATE token = VALUES(token);

-- 导师卡（u1 有效学期卡；u2 未激活）
INSERT INTO cards (user_id, card_number, card_type, status, start_date, end_date, created_at, updated_at)
VALUES
  (@u1, 'CARD-SEM-001', 'semester', 'active', DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), NOW(), NOW()),
  (@u2, 'CARD-SEM-002', 'semester', 'inactive', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE card_number = VALUES(card_number);

-- 未来一周的课程
INSERT INTO courses (course_date, start_time, end_time, teacher, dance_type, capacity, current_participants, status, created_at, updated_at)
VALUES
  (DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00', '21:00:00', 'Teacher A', 'HipHop', 25, 0, 'published', NOW(), NOW()),
  (DATE_ADD(CURDATE(), INTERVAL 3 DAY), '19:00:00', '21:00:00', 'Teacher B', 'Popping', 20, 0, 'published', NOW(), NOW())
ON DUPLICATE KEY UPDATE course_date = VALUES(course_date);

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

-- 预约（u1 预约第一个课程）
INSERT IGNORE INTO reservations (user_id, course_id, status, checkin_status)
VALUES (@u1, @c1, 'reserved', 'not_checked');

-- 同步课程当前人数
UPDATE courses c
SET current_participants = (
  SELECT COUNT(*) FROM reservations r WHERE r.course_id = c.id AND r.status = 'reserved'
)
WHERE c.id IN (@c1, @c2);