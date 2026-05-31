SET NAMES utf8mb4;

UPDATE lost_item SET images = CONCAT(SUBSTRING_INDEX(images, '?', 1), '?v=userref2026');
UPDATE found_item SET images = CONCAT(SUBSTRING_INDEX(images, '?', 1), '?v=userref2026');
