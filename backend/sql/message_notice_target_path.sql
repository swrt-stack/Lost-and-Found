ALTER TABLE message_notice
    ADD COLUMN target_path VARCHAR(255) NULL AFTER content;
