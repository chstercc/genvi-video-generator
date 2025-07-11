package com.example.springboot_pro.domain;

import jakarta.persistence.*;
import com.example.springboot_pro.util.SecurityUtil;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_tasks")
public class VideoTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "storyboard_id", nullable = false)
    private Long storyboardId;
    
    @Column(name = "task_id", nullable = false, unique = true)
    private String taskId; // 山火API返回的任务ID
    
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl; // 概念图URL
    
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt; // 图生视频提示词
    
    @Column(name = "aspect_ratio")
    private String aspectRatio; // 视频比例
    
    @Column(name = "status", nullable = false)
    private String status; // 任务状态: submitting, submitted, generating, completed, failed
    
    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl; // 生成的视频URL
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 错误信息
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 任务完成时间
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        // 获取当前用户信息
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            userId = currentUserId;
        }
        
        if (status == null) {
            status = "submitting";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        if ("completed".equals(status) || "failed".equals(status)) {
            if (completedAt == null) {
                completedAt = LocalDateTime.now();
            }
        }
    }
    
    // 构造函数
    public VideoTask() {}
    
    public VideoTask(Long storyboardId, String imageUrl, String prompt, String aspectRatio) {
        this.storyboardId = storyboardId;
        this.imageUrl = imageUrl;
        this.prompt = prompt;
        this.aspectRatio = aspectRatio;
        this.status = "submitting";
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getStoryboardId() {
        return storyboardId;
    }
    
    public void setStoryboardId(Long storyboardId) {
        this.storyboardId = storyboardId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getAspectRatio() {
        return aspectRatio;
    }
    
    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return "VideoTask{" +
                "id=" + id +
                ", storyboardId=" + storyboardId +
                ", taskId='" + taskId + '\'' +
                ", status='" + status + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
} 