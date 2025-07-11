-- 数据库字段类型迁移脚本
-- 将concept_image和generated_video字段从VARCHAR改为TEXT类型，以支持长URL存储

-- 修改concept_image字段类型为TEXT
ALTER TABLE storyboard 
MODIFY COLUMN concept_image TEXT COMMENT '概念图URL，支持长URL存储';

-- 修改generated_video字段类型为TEXT  
ALTER TABLE storyboard 
MODIFY COLUMN generated_video TEXT COMMENT '生成的视频URL，支持长URL存储';

-- 验证修改结果
-- SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'storyboard' 
-- AND COLUMN_NAME IN ('concept_image', 'generated_video');

-- 创建video_tasks表来跟踪山火API图生视频任务
CREATE TABLE IF NOT EXISTS video_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    storyboard_id BIGINT NOT NULL COMMENT '关联的故事板ID',
    task_id VARCHAR(255) NOT NULL UNIQUE COMMENT '山火API返回的任务ID',
    image_url TEXT NOT NULL COMMENT '概念图URL',
    prompt TEXT COMMENT '图生视频提示词',
    aspect_ratio VARCHAR(10) COMMENT '视频比例',
    status VARCHAR(20) NOT NULL DEFAULT 'submitting' COMMENT '任务状态：submitting,submitted,generating,completed,failed',
    video_url TEXT COMMENT '生成的视频URL',
    error_message TEXT COMMENT '错误信息',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    completed_at DATETIME COMMENT '任务完成时间',
    INDEX idx_storyboard_id (storyboard_id),
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频生成任务表'; 

-- 添加音效相关字段到storyboard表
ALTER TABLE storyboard 
ADD COLUMN IF NOT EXISTS audio_video TEXT COMMENT '带音效的视频URL';

ALTER TABLE storyboard 
ADD COLUMN IF NOT EXISTS audio_generated_at DATETIME COMMENT '音效生成时间';

ALTER TABLE storyboard 
ADD COLUMN IF NOT EXISTS audio_status VARCHAR(20) COMMENT '音效状态：generating,completed,failed'; 