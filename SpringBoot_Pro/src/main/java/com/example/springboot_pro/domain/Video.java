package com.example.springboot_pro.domain;

import jakarta.persistence.*;
import com.example.springboot_pro.util.SecurityUtil;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "story_id", nullable = false)
    private Long storyId;
    
    @Column(name = "video_url", nullable = false)
    private String videoUrl;
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @Column(name = "duration")
    private Integer duration; // 视频时长，单位：秒
    
    @Column(name = "scene_count")
    private Integer sceneCount; // 场景数量
    
    @Column(name = "file_size")
    private Long fileSize; // 文件大小，单位：字节
    
    @Column(name = "video_format")
    private String videoFormat; // 视频格式，如mp4, avi等
    
    @Column(name = "resolution")
    private String resolution; // 分辨率，如1920x1080
    
    @Column(name = "status")
    private String status; // 状态：processing, completed, failed
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "author_name")
    private String authorName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        // 获取当前用户信息
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentUsername = SecurityUtil.getCurrentUsername();
        
        if (currentUserId != null) {
            userId = currentUserId;
            authorName = currentUsername;
        } else {
            authorName = "匿名用户";
        }
        
        if (status == null) {
            status = "processing";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 构造函数
    public Video() {}
    
    public Video(String title, String description, Long storyId, String videoUrl) {
        this.title = title;
        this.description = description;
        this.storyId = storyId;
        this.videoUrl = videoUrl;
        this.status = "completed";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getSceneCount() {
        return sceneCount;
    }

    public void setSceneCount(Integer sceneCount) {
        this.sceneCount = sceneCount;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
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

    @Override
    public String toString() {
        return "Video{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", storyId=" + storyId +
                ", videoUrl='" + videoUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", duration=" + duration +
                ", sceneCount=" + sceneCount +
                ", fileSize=" + fileSize +
                ", videoFormat='" + videoFormat + '\'' +
                ", resolution='" + resolution + '\'' +
                ", status='" + status + '\'' +
                ", userId=" + userId +
                ", authorName='" + authorName + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 