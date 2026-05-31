CREATE TABLE IF NOT EXISTS `item_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `item_id` VARCHAR(64) NOT NULL,
  `sender_user_id` BIGINT NOT NULL,
  `receiver_user_id` BIGINT NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `read_flag` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_chat_item` (`item_id`),
  KEY `idx_item_chat_sender` (`sender_user_id`),
  KEY `idx_item_chat_receiver` (`receiver_user_id`),
  KEY `idx_item_chat_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
