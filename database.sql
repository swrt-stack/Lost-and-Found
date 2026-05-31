CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50),
  phone VARCHAR(20),
  avatar_url VARCHAR(255),
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE item_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lost_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  location VARCHAR(200),
  lost_time DATETIME,
  contact VARCHAR(100),
  images VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id),
  FOREIGN KEY (category_id) REFERENCES item_category(id)
);

CREATE TABLE found_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  location VARCHAR(200),
  found_time DATETIME,
  pickup_method VARCHAR(200),
  images VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id),
  FOREIGN KEY (category_id) REFERENCES item_category(id)
);

CREATE TABLE message_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  message_type VARCHAR(50) NOT NULL,
  content VARCHAR(500) NOT NULL,
  target_path VARCHAR(255),
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE item_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id VARCHAR(64) NOT NULL,
  sender_user_id BIGINT NOT NULL,
  receiver_user_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_item_chat_item (item_id),
  KEY idx_item_chat_sender (sender_user_id),
  KEY idx_item_chat_receiver (receiver_user_id),
  KEY idx_item_chat_created (created_at)
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  action VARCHAR(100) NOT NULL,
  detail VARCHAR(500),
  ip_address VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE claim_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  found_item_id BIGINT NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  message VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  review_remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (found_item_id) REFERENCES found_item(id),
  FOREIGN KEY (applicant_user_id) REFERENCES sys_user(id),
  FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE TABLE announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE item_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id VARCHAR(40) NOT NULL,
  item_type VARCHAR(20) NOT NULL,
  reporter_user_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  review_remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (reporter_user_id) REFERENCES sys_user(id)
);

CREATE TABLE system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  site_name VARCHAR(100) NOT NULL,
  review_enabled TINYINT NOT NULL DEFAULT 1,
  max_image_size INT NOT NULL DEFAULT 5,
  notice_enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO sys_user (username, password, nickname, phone, role, status)
VALUES
('demo', '$2a$10$7QJk0oD0lJYj7V6TQ2xE2uxV2m1dQxW8m2F6yKQv1cQm4f2mG4L8K', 'Demo User', '13800000000', 'USER', 1),
('reviewer', '$2a$10$7QJk0oD0lJYj7V6TQ2xE2uxV2m1dQxW8m2F6yKQv1cQm4f2mG4L8K', 'Review Admin', '13900000000', 'REVIEW_ADMIN', 1),
('sysadmin', '$2a$10$7QJk0oD0lJYj7V6TQ2xE2uxV2m1dQxW8m2F6yKQv1cQm4f2mG4L8K', 'System Admin', '13700000000', 'SYS_ADMIN', 1);

INSERT INTO item_category (name)
VALUES
('电子产品'),
('证件卡片'),
('书籍资料'),
('生活用品'),
('其他');

INSERT INTO lost_item (user_id, category_id, title, description, location, lost_time, contact, images, status)
VALUES
(1, 1, '黑色无线蓝牙耳机', '黑色入耳式蓝牙耳机，充电盒右下角有轻微划痕，于图书馆三楼自习区遗失。', '图书馆三楼自习区', '2026-05-02 09:20:00', '13800000001', '/uploads/demo/lost-01-earbuds.jpg', 1),
(1, 2, '蓝色校园一卡通', '蓝色校园卡，背面贴有“计科2201”标签，午餐后在第一食堂门口遗失。', '第一食堂门口', '2026-05-03 12:15:00', '13800000002', '/uploads/demo/lost-02-card.jpg', 1),
(15, 3, '高等数学第七版教材', '同济大学版高数上册，封面写有姓名“张同学”，内页夹有复习便签。', '教学楼A座201教室', '2026-05-04 14:30:00', '13800000003', '/uploads/demo/lost-03-book.jpg', 1),
(1, 4, '银色不锈钢保温杯', '500毫升保温杯，杯身印有校徽贴纸，保温效果良好。', '体育馆看台二层', '2026-05-05 18:40:00', '13800000004', '/uploads/demo/lost-04-thermos.jpg', 1),
(15, 4, '黑色双肩书包', '黑色双肩包，侧袋有雨伞收纳位，主仓内有笔记本和充电器。', '校车候车站', '2026-05-06 07:50:00', '13800000005', '/uploads/demo/lost-05-backpack.jpg', 0),
(1, 1, '深空灰色智能手机', '6.1英寸屏幕手机，贴有钢化膜，手机壳为透明软壳。', '篮球场休息区', '2026-05-07 16:20:00', '13800000006', '/uploads/demo/lost-06-phone.jpg', 1),
(15, 1, '青轴机械键盘', '87键机械键盘，白色键帽，青轴手感清脆，用于机房练习。', '计算机学院机房302', '2026-05-08 20:10:00', '13800000007', '/uploads/demo/lost-07-keyboard.jpg', 1),
(1, 2, '居民身份证', '二代身份证，证件套为棕色皮质，于校医院大厅办理业务时遗失。', '校医院大厅', '2026-05-09 10:05:00', '13800000008', '/uploads/demo/lost-08-idcard.jpg', 0),
(15, 4, '折叠晴雨伞', '藏青色三折晴雨伞，伞柄有小熊挂件，雨天在南门公交站台遗失。', '南门公交站台', '2026-05-10 17:35:00', '13800000009', '/uploads/demo/lost-09-umbrella.jpg', 1),
(1, 5, '黑框近视眼镜', '黑框眼镜，镜片为防蓝光，镜腿内侧刻有编号。', '实验楼B座二楼走廊', '2026-05-11 11:45:00', '13800000010', '/uploads/demo/lost-10-glasses.jpg', 1);

INSERT INTO found_item (user_id, category_id, title, description, location, found_time, pickup_method, images, status)
VALUES
(1, 1, '黑色头戴式耳机', '黑色头戴式耳机，耳罩有使用痕迹，在图书馆入口闸机旁拾到。', '图书馆入口', '2026-05-02 08:30:00', '请携带有效证件到失物招领处核验后领取。', '/uploads/demo/found-01-headphone.jpg', 1),
(15, 2, '学生证卡片', '学生证一张，卡面照片清晰，请失主核对学号与学院信息。', '教学楼A座正门', '2026-05-03 09:10:00', '工作日 9:00-18:00 到行政楼一楼失物招领台领取。', '/uploads/demo/found-02-student-card.jpg', 1),
(1, 3, '线性代数教材', '线性代数教材一本，书脊有磨损，内页有课堂笔记。', '第二教学楼台阶', '2026-05-04 13:20:00', '联系发布者当面核验书籍特征后领取。', '/uploads/demo/found-03-textbook.jpg', 1),
(15, 4, '陶瓷马克杯', '白色陶瓷杯，杯身印有卡通图案，在咖啡厅靠窗座位发现。', '学生活动中心咖啡厅', '2026-05-05 15:00:00', '请描述杯身图案并预约领取时间。', '/uploads/demo/found-04-mug.jpg', 0),
(1, 4, '灰色运动腰包', '灰色腰包，拉链完好，内有纸巾和公交卡套（无卡）。', '田径场跑道旁', '2026-05-06 06:45:00', '请提供腰包内物品细节以便核验。', '/uploads/demo/found-05-bag.jpg', 1),
(15, 1, '白色充电宝', '10000毫安白色充电宝，电量指示灯正常，在宿舍楼下长椅拾到。', '梅园宿舍楼下长椅', '2026-05-07 21:30:00', '核验品牌与接口类型后领取。', '/uploads/demo/found-06-powerbank.jpg', 1),
(1, 1, '无线鼠标', '黑色无线鼠标，USB接收器收纳在底部，在图书馆电子阅览室拾到。', '图书馆电子阅览室', '2026-05-08 19:15:00', '请说明鼠标品牌及使用痕迹后联系领取。', '/uploads/demo/found-07-mouse.jpg', 1),
(15, 2, '银行卡', '某银行借记卡一张，已交至保卫处，请失主尽快挂失并认领。', '校园ATM机旁', '2026-05-09 12:40:00', '需本人持身份证到保卫处办理认领。', '/uploads/demo/found-08-bankcard.jpg', 1),
(1, 4, '棒球遮阳帽', '深蓝色棒球帽，帽檐有轻微污渍，在樱花大道长椅发现。', '樱花大道', '2026-05-10 14:55:00', '请描述帽子内侧标签或购买渠道。', '/uploads/demo/found-09-hat.jpg', 1),
(15, 5, '金属钥匙串', '三把钥匙加一个小熊挂件，其中一把为宿舍钥匙，在行政楼一楼拾到。', '行政楼一楼服务台', '2026-05-11 08:20:00', '请准确描述钥匙数量与挂件特征后领取。', '/uploads/demo/found-10-keys.jpg', 1);

INSERT INTO message_notice (user_id, message_type, content, read_flag)
VALUES
(1, 'REVIEW', 'Your lost item "Bluetooth Earbuds" has been approved.', 0),
(1, 'MATCH', 'The system found a possible matching found-item record for you.', 1);

INSERT INTO item_chat_message (item_id, sender_user_id, receiver_user_id, content, read_flag)
VALUES
('FOUND-1', 2, 1, 'Hello, please confirm whether the found student card belongs to you.', 1),
('FOUND-1', 1, 2, 'Thank you. I can provide the student number and college information for verification.', 0);

INSERT INTO operation_log (user_id, action, detail, ip_address)
VALUES
(1, 'LOGIN', 'Demo user signed in.', '127.0.0.1'),
(2, 'APPROVE_REVIEW', 'Review admin approved a lost-item post.', '127.0.0.1'),
(3, 'UPDATE_CONFIG', 'System admin updated system configuration.', '127.0.0.1');

INSERT INTO claim_application (found_item_id, applicant_user_id, owner_user_id, message, status, review_remark)
VALUES
(1, 1, 1, 'I can describe the student ID details and owner information.', 2, 'Please provide more accurate identity details.');

INSERT INTO announcement (title, content, status)
VALUES
('Lost and Found Desk Hours', 'The campus lost and found desk is open from 09:00 to 18:00 on weekdays.', 1),
('Claim Reminder', 'Bring a valid ID or enough item details when you submit a claim request.', 1),
('System Upgrade', 'Review and notification modules were upgraded this week.', 1);

INSERT INTO item_report (item_id, item_type, reporter_user_id, reason, status, review_remark)
VALUES
('FOUND-1', 'FOUND', 1, 'The post content looks suspicious and may not match the real owner.', 2, 'Checked by admin, no further issue found.');

INSERT INTO system_config (id, site_name, review_enabled, max_image_size, notice_enabled)
VALUES
(1, 'Campus Lost and Found', 1, 5, 1);
