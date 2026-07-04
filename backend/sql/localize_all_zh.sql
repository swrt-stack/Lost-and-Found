SET NAMES utf8mb4;

UPDATE sys_user SET nickname = '演示用户' WHERE username = 'demo';
UPDATE sys_user SET nickname = '审核管理员' WHERE username = 'reviewer';
UPDATE sys_user SET nickname = '系统管理员' WHERE username = 'sysadmin';

-- 系统配置
UPDATE system_config SET site_name = '校园失物招领平台' WHERE id = 1;

-- 消息通知：替换历史英文物品名、公告文案
UPDATE message_notice SET content = REPLACE(content, 'Bluetooth Earbuds', '黑色无线蓝牙耳机');
UPDATE message_notice SET content = REPLACE(content, 'Student ID Card', '学生证卡片');
UPDATE message_notice SET content = REPLACE(content, 'Cache Smoke Announcement - Created for Redis cache invalidation smoke test', '欢迎使用校园失物招领平台，发布与认领信息请及时关注消息中心通知。');
UPDATE message_notice SET content = REPLACE(content, 'Cache Smoke Announcement', '平台使用提示');

UPDATE message_notice mn
INNER JOIN announcement a ON a.id = 4
SET mn.content = CONCAT('系统公告：', a.title, ' - ', a.content)
WHERE mn.message_type = 'ANNOUNCEMENT';

UPDATE message_notice SET content = '用户 szh 就物品【黑色无线蓝牙耳机】向你发送了一条新消息'
WHERE id = 13 AND message_type = 'CHAT';

UPDATE message_notice SET content = '用户 szh 就物品【黑色无线蓝牙耳机】向你发送了一条新消息'
WHERE id = 8 AND message_type = 'CHAT' AND content LIKE '%Bluetooth Earbuds%';

-- 认领、举报、聊天（若存在种子英文数据）
UPDATE claim_application SET
  message = '我可以描述学生证上的学号、姓名等细节信息。',
  review_remark = '请补充更准确的身份核验信息。'
WHERE message LIKE '%student ID%' OR message LIKE '%I can describe%';

UPDATE item_report SET
  reason = '该发布内容存疑，可能与实际失主信息不符。',
  review_remark = '管理员已核查，未发现进一步问题。'
WHERE reason LIKE '%suspicious%' OR reason LIKE '%The post%';

UPDATE item_chat_message SET
  content = '你好，请确认拾到的学生证是否属于你。'
WHERE content LIKE 'Hello, please confirm%';

UPDATE item_chat_message SET
  content = '谢谢，我可以提供学号和学院信息进行核实。'
WHERE content LIKE 'Thank you. I can provide%';

-- 操作日志明细
UPDATE operation_log SET detail = '演示用户登录' WHERE detail = 'Demo user signed in.';
UPDATE operation_log SET detail = '审核管理员通过一条失物发布审核' WHERE detail = 'Review admin approved a lost-item post.';
UPDATE operation_log SET detail = '系统管理员更新系统配置' WHERE detail = 'System admin updated system configuration.';
UPDATE operation_log SET detail = '用户登录' WHERE detail = 'User logged in';
UPDATE operation_log SET detail = '用户登出' WHERE detail = 'User logged out';
UPDATE operation_log SET detail = '新用户注册' WHERE detail = 'New user registered';
UPDATE operation_log SET detail = '更新个人资料' WHERE detail = 'Updated personal profile';
UPDATE operation_log SET detail = '更新系统配置' WHERE detail = 'Updated system configuration';
UPDATE operation_log SET detail = '标记全部消息已读' WHERE detail = 'Read all messages';

UPDATE operation_log SET detail = REPLACE(detail, 'Submitted claim for ', '提交认领申请：');
UPDATE operation_log SET detail = REPLACE(detail, 'Sent chat message for ', '发送物品沟通消息：');
UPDATE operation_log SET detail = REPLACE(detail, 'Published lost item ', '发布失物信息：');
UPDATE operation_log SET detail = REPLACE(detail, 'Published found item ', '发布招领信息：');
UPDATE operation_log SET detail = REPLACE(detail, 'Updated ', '更新物品：') WHERE detail LIKE 'Updated %' AND detail NOT LIKE '更新%';
UPDATE operation_log SET detail = REPLACE(detail, 'Offlined ', '下架物品：');
UPDATE operation_log SET detail = REPLACE(detail, 'Deleted ', '删除物品：') WHERE detail LIKE 'Deleted %' AND detail NOT LIKE '删除%';
UPDATE operation_log SET detail = REPLACE(detail, 'Reported ', '举报物品：');
UPDATE operation_log SET detail = REPLACE(detail, 'Completed ', '完成物品：');
UPDATE operation_log SET detail = REPLACE(detail, 'Approved claim ', '通过认领申请：');
UPDATE operation_log SET detail = REPLACE(detail, 'Rejected claim ', '驳回认领申请：');
UPDATE operation_log SET detail = REPLACE(detail, 'Approved review ', '通过审核：');
UPDATE operation_log SET detail = REPLACE(detail, 'Rejected review ', '驳回审核：');
UPDATE operation_log SET detail = REPLACE(detail, 'Deleted review ', '删除审核记录：');
UPDATE operation_log SET detail = REPLACE(detail, 'Created announcement ', '创建公告：');
UPDATE operation_log SET detail = REPLACE(detail, 'Updated announcement ', '更新公告：');
UPDATE operation_log SET detail = REPLACE(detail, 'Deleted announcement ', '删除公告：');
UPDATE operation_log SET detail = REPLACE(detail, 'Resolved report ', '处理举报：');
UPDATE operation_log SET detail = REPLACE(detail, 'Rejected report ', '驳回举报：');
UPDATE operation_log SET detail = REPLACE(detail, 'Created category ', '创建分类：');
UPDATE operation_log SET detail = REPLACE(detail, 'Deleted category ', '删除分类：');
UPDATE operation_log SET detail = REPLACE(detail, 'Updated user status: ', '更新用户状态：');
UPDATE operation_log SET detail = REPLACE(detail, 'Updated user role: ', '更新用户角色：');
UPDATE operation_log SET detail = REPLACE(detail, 'Read message ', '标记消息已读：');
UPDATE operation_log SET detail = REPLACE(detail, 'Cache Smoke Announcement', '平台使用提示');
UPDATE operation_log SET detail = REPLACE(detail, 'CacheSmokeCategory20260424', '缓存测试分类');
UPDATE operation_log SET detail = '删除分类：缓存测试分类' WHERE detail LIKE '%category%缓存测试分类%';
UPDATE operation_log SET detail = '更新用户状态：demo' WHERE detail LIKE '%user status: demo%';
UPDATE operation_log SET detail = REPLACE(detail, 'FOUND-', '招领-');
UPDATE operation_log SET detail = REPLACE(detail, 'LOST-', '失物-');
UPDATE operation_log SET detail = '更新用户状态：演示用户' WHERE detail = '更新用户状态：demo';
