-- MoForum 完整建库脚本（从零初始化）
-- 用法：mysql -u root -p < init.sql
-- 注意：用户、帖子、回复等示例数据由后端 DataInitializer 在首次启动时自动生成

CREATE DATABASE IF NOT EXISTS `moforum` DEFAULT CHARACTER SET utf8mb4;
USE `moforum`;

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(256) NOT NULL,
  `user_no` bigint DEFAULT NULL UNIQUE COMMENT 'Mo号，对外唯一',
  `bio` varchar(256) DEFAULT NULL COMMENT '个人签名',
  `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像 OSS 链接',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 板块表
CREATE TABLE IF NOT EXISTS `t_board` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '吧名',
  `description` varchar(512) DEFAULT NULL COMMENT '简介',
  `avatar` varchar(32) DEFAULT NULL COMMENT '展示用图标/emoji',
  `sort_order` int NOT NULL DEFAULT 0,
  `post_count` int NOT NULL DEFAULT 0 COMMENT '帖子数（冗余，发帖时递增）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贴吧板块';

-- 帖子表
CREATE TABLE IF NOT EXISTS `t_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `board_id` bigint NOT NULL DEFAULT 1 COMMENT '所属吧',
  `title` varchar(256) NOT NULL,
  `content` text NOT NULL,
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览',
  `reply_count` int NOT NULL DEFAULT 0 COMMENT '回复数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_board_ct` (`board_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子';

-- 回复表
CREATE TABLE IF NOT EXISTS `t_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子回复（楼层）';

-- 关注表
CREATE TABLE IF NOT EXISTS `t_follow` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `follower_id` BIGINT NOT NULL,
    `followed_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_follow` (`follower_id`, `followed_id`),
    FOREIGN KEY (`follower_id`) REFERENCES `t_user`(`id`),
    FOREIGN KEY (`followed_id`) REFERENCES `t_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注';

-- 好友申请表
CREATE TABLE IF NOT EXISTS `t_friend_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `from_id` BIGINT NOT NULL,
    `to_id` BIGINT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=PENDING 1=ACCEPTED 2=REJECTED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_friend_request` (`from_id`, `to_id`),
    FOREIGN KEY (`from_id`) REFERENCES `t_user`(`id`),
    FOREIGN KEY (`to_id`) REFERENCES `t_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请';

-- 好友关系表
CREATE TABLE IF NOT EXISTS `t_friend` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_1` BIGINT NOT NULL,
    `user_id_2` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_friend` (`user_id_1`, `user_id_2`),
    FOREIGN KEY (`user_id_1`) REFERENCES `t_user`(`id`),
    FOREIGN KEY (`user_id_2`) REFERENCES `t_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系';

-- 私信表
CREATE TABLE IF NOT EXISTS `t_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `from_id` BIGINT NOT NULL,
    `to_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `read` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conv` (`from_id`, `to_id`, `create_time`),
    INDEX `idx_unread` (`to_id`, `read`),
    FOREIGN KEY (`from_id`) REFERENCES `t_user`(`id`),
    FOREIGN KEY (`to_id`) REFERENCES `t_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信';

-- 板块示例数据
INSERT INTO `t_board` (`id`, `name`, `description`, `avatar`, `sort_order`) VALUES
  (1, '综合吧', '闲聊、求助、灌水 无所不谈', '🗨️', 10),
  (2, '游戏吧', '攻略分享、开黑组队、游戏吃瓜', '🎮', 20),
  (3, '学习吧', '笔记交流、考试经验、技术讨论', '📚', 30),
  (4, '生活吧', '美食探店、旅行攻略、日常分享', '☕', 40)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
