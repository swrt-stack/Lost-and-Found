UPDATE announcement SET title = '失物招领处开放时间', content = '校园失物招领处工作日开放时间为 09:00—18:00，请在此时间段内办理相关业务。', updated_at = NOW() WHERE title = 'Lost and Found Desk Hours';
UPDATE announcement SET title = '认领须知', content = '提交认领申请时请携带有效证件，或提供足以核实身份的物品细节信息。', updated_at = NOW() WHERE title = 'Claim Reminder';
UPDATE announcement SET title = '系统升级通知', content = '本周已完成审核与消息通知模块升级，相关功能已同步上线。', updated_at = NOW() WHERE title = 'System Upgrade';
UPDATE announcement SET title = '平台使用提示', content = '欢迎使用校园失物招领平台，发布与认领信息请及时关注消息中心通知。', updated_at = NOW() WHERE title = 'Cache Smoke Announcement';
