-- IR消息表结构和示例数据
-- 用于AGEIPort分布式导出客户端演示

CREATE TABLE IF NOT EXISTS `ir_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` varchar(64) NOT NULL COMMENT '消息ID',
  `sender_id` varchar(32) NOT NULL COMMENT '发送方ID',
  `receiver_id` varchar(32) NOT NULL COMMENT '接收方ID',
  `message_type` varchar(16) NOT NULL COMMENT '消息类型',
  `content` text COMMENT '消息内容',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '消息状态',
  `priority` int(11) NOT NULL DEFAULT '0' COMMENT '优先级',
  `retry_count` int(11) NOT NULL DEFAULT '0' COMMENT '重试次数',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scheduled_time` datetime DEFAULT NULL COMMENT '计划发送时间',
  `sent_time` datetime DEFAULT NULL COMMENT '实际发送时间',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `attachment_count` int(11) NOT NULL DEFAULT '0' COMMENT '附件数量',
  `has_attachments` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有附件',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IR消息表';

-- 插入示例数据
INSERT INTO `ir_message` (`message_id`, `sender_id`, `receiver_id`, `message_type`, `content`, `status`, `priority`, `attachment_count`, `has_attachments`) VALUES
('MSG_001', 'USER_001', 'USER_002', 'TEXT', '这是一条测试消息', 'SENT', 1, 2, 1),
('MSG_002', 'USER_002', 'USER_003', 'IMAGE', '图片消息内容', 'PENDING', 2, 1, 1),
('MSG_003', 'USER_003', 'USER_001', 'FILE', '文件消息内容', 'FAILED', 3, 5, 1),
('MSG_004', 'USER_001', 'USER_004', 'TEXT', '普通文本消息', 'SENT', 1, 0, 0),
('MSG_005', 'USER_004', 'USER_002', 'VIDEO', '视频消息内容', 'PROCESSING', 2, 3, 1);

-- 生成大量测试数据的存储过程（可选）
-- 注意：这将生成10万条记录，用于测试大数据量导出
DELIMITER $$
CREATE PROCEDURE `generate_test_data`(IN record_count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE message_types VARCHAR(100) DEFAULT 'TEXT,IMAGE,FILE,VIDEO,AUDIO';
    DECLARE statuses VARCHAR(100) DEFAULT 'SENT,PENDING,FAILED,PROCESSING';
    
    WHILE i <= record_count DO
        INSERT INTO `ir_message` (
            `message_id`, 
            `sender_id`, 
            `receiver_id`, 
            `message_type`, 
            `content`, 
            `status`, 
            `priority`, 
            `attachment_count`, 
            `has_attachments`
        ) VALUES (
            CONCAT('MSG_', LPAD(i, 8, '0')),
            CONCAT('USER_', LPAD(FLOOR(1 + RAND() * 1000), 4, '0')),
            CONCAT('USER_', LPAD(FLOOR(1 + RAND() * 1000), 4, '0')),
            SUBSTRING_INDEX(SUBSTRING_INDEX(message_types, ',', FLOOR(1 + RAND() * 5)), ',', -1),
            CONCAT('测试消息内容 ', i),
            SUBSTRING_INDEX(SUBSTRING_INDEX(statuses, ',', FLOOR(1 + RAND() * 4)), ',', -1),
            FLOOR(1 + RAND() * 3),
            FLOOR(RAND() * 6),
            IF(FLOOR(RAND() * 6) > 0, 1, 0)
        );
        SET i = i + 1;
        
        -- 每1000条记录提交一次，避免事务过大
        IF i % 1000 = 0 THEN
            COMMIT;
        END IF;
    END WHILE;
    COMMIT;
END$$
DELIMITER ;

-- 使用示例：生成10万条测试数据
-- CALL generate_test_data(100000);

-- 查询统计信息
-- SELECT 
--     COUNT(*) as total_records,
--     COUNT(CASE WHEN has_attachments = 1 THEN 1 END) as records_with_attachments,
--     SUM(attachment_count) as total_attachments
-- FROM ir_message;
