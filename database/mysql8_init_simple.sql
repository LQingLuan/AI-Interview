-- MySQL数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `smart_interview_db`

USE `interview_system`;

-- 1. 面试记录主表
CREATE TABLE IF NOT EXISTS `interview_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `interview_id` varchar(64) NOT NULL COMMENT '面试记录唯一标识',
  `user_id` varchar(64) NOT NULL DEFAULT 'anonymous' COMMENT '用户ID',
  `career_direction` varchar(100) NOT NULL COMMENT '职业方向',
  `difficulty_level` int NOT NULL COMMENT '难度等级',
  `question_text` text NOT NULL COMMENT '面试问题',
  `answer_text` text COMMENT '用户回答',
  `answer_duration` int DEFAULT NULL COMMENT '回答时长(秒)',
  `overall_score` varchar(20) DEFAULT NULL COMMENT '总体评分',
  `overall_feedback` text COMMENT '总体反馈',
  `improvement_suggestions` json DEFAULT NULL COMMENT '改进建议(JSON格式)',
  `status` enum('PENDING','ANSWERED','FEEDBACK_GENERATED','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `ai_model_used` varchar(50) DEFAULT 'spark' COMMENT '使用的AI模型',
  `language` varchar(10) DEFAULT 'zh' COMMENT '语言',
  `interview_mode` varchar(20) DEFAULT 'normal' COMMENT '面试模式',
  `reference_answer` text NOT NULL COMMENT '参考答案',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_id` (`interview_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_career_direction` (`career_direction`),
  KEY `idx_difficulty_level` (`difficulty_level`),
  KEY `idx_question_text` (`question_text`(255)),
  KEY `idx_answer_text` (`answer_text`(255)),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_completed_at` (`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试记录主表';

-- 2. 面试维度评分表
CREATE TABLE IF NOT EXISTS `interview_record_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `interview_record_id` bigint unsigned NOT NULL COMMENT '面试记录ID',
  `interview_id` varchar(64) NOT NULL COMMENT '面试记录唯一标识',
  `dimension_name` varchar(50) NOT NULL COMMENT '维度名称',
  `score` decimal(5,2) DEFAULT NULL COMMENT '维度得分',
  `evaluation` text COMMENT '维度评价',
  `weight` decimal(3,2) DEFAULT '1.00' COMMENT '权重',
  `order_num` int DEFAULT NULL COMMENT '排序序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_interview_record_id` (`interview_record_id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_dimension_name` (`dimension_name`),
  KEY `idx_score` (`score`),
  CONSTRAINT `fk_interview_record_detail_record` FOREIGN KEY (`interview_record_id`) REFERENCES `interview_record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试维度评分表';


-- 3. 用户表（用于登录和用户管理）
CREATE TABLE IF NOT EXISTS `Smart_Interview_User` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户唯一标识，与interview_record.user_id对应',
  `username` varchar(50) NOT NULL COMMENT '用户名/账号',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希值',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `user_type` enum('REGULAR','VIP','ADMIN','TEST') NOT NULL DEFAULT 'REGULAR' COMMENT '用户类型',
  `status` enum('ACTIVE','INACTIVE','LOCKED','DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
  `last_login_at` datetime(3) DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
  `failed_login_attempts` int NOT NULL DEFAULT '0' COMMENT '连续登录失败次数',
  `account_locked_until` datetime(3) DEFAULT NULL COMMENT '账户锁定截止时间',
  `password_changed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '密码最后修改时间',
  `email_verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '邮箱是否已验证',
  `phone_verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '手机号是否已验证',
  `preferences` json DEFAULT NULL COMMENT '用户偏好设置(JSON格式)',
  `career_direction` varchar(100) DEFAULT NULL COMMENT '默认职业方向',
  `difficulty_level` int DEFAULT '3' COMMENT '默认难度等级',
  `interview_mode` varchar(20) DEFAULT 'normal' COMMENT '默认面试模式',
  `language` varchar(10) DEFAULT 'zh' COMMENT '默认语言',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_status` (`status`),
  KEY `idx_last_login_at` (`last_login_at`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_career_direction` (`career_direction`),
  KEY `idx_real_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
