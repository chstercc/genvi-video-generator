package com.example.springboot_pro.domain;

import jakarta.persistence.*;
import com.example.springboot_pro.util.SecurityUtil;
import java.time.LocalDateTime;

@Entity
@Table(name = "storyboard")
public class Storyboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "story_id", nullable = false)
    private Long storyId;
    
    @Column(name = "scene", nullable = false)
    private Integer scene;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String script;
    
    @Column(name = "image_prompt", columnDefinition = "TEXT")
    private String imagePrompt;
    
    @Column(name = "video_prompt", columnDefinition = "TEXT")
    private String videoPrompt;
    
    @Column(name = "concept_image", columnDefinition = "TEXT")
    private String conceptImage;
    
    @Column(name = "generated_video", columnDefinition = "TEXT")
    private String generatedVideo;
    
    @Column(name = "video_generated_at")
    private LocalDateTime videoGeneratedAt;
    
    @Column(name = "video_status")
    private String videoStatus; // 'generating', 'completed', 'failed'
    
    // 音效相关字段
    @Column(name = "audio_video", columnDefinition = "TEXT")
    private String audioVideo; // 带音效的视频URL
    
    @Column(name = "audio_generated_at")
    private LocalDateTime audioGeneratedAt;
    
    @Column(name = "audio_status")
    private String audioStatus; // 'generating', 'completed', 'failed'
    
    @Column(name = "user_id")
    private Long userId;
    
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
        if (currentUserId != null) {
            userId = currentUserId;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 构造函数
    public Storyboard() {}
    
    public Storyboard(Long storyId, Integer scene, String script) {
        this.storyId = storyId;
        this.scene = scene;
        this.script = script;
    }
    
    public Storyboard(Long storyId, Integer scene, String script, String imagePrompt, String videoPrompt) {
        this.storyId = storyId;
        this.scene = scene;
        this.script = script;
        this.imagePrompt = imagePrompt;
        this.videoPrompt = videoPrompt;
        this.videoStatus = "pending"; // 默认状态为待生成
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public Integer getScene() {
        return scene;
    }

    public void setScene(Integer scene) {
        this.scene = scene;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getImagePrompt() {
        return imagePrompt;
    }

    public void setImagePrompt(String imagePrompt) {
        this.imagePrompt = imagePrompt;
    }

    public String getVideoPrompt() {
        return videoPrompt;
    }

    public void setVideoPrompt(String videoPrompt) {
        this.videoPrompt = videoPrompt;
    }

    public String getConceptImage() {
        return conceptImage;
    }

    public void setConceptImage(String conceptImage) {
        this.conceptImage = conceptImage;
    }

    public String getGeneratedVideo() {
        return generatedVideo;
    }

    public void setGeneratedVideo(String generatedVideo) {
        this.generatedVideo = generatedVideo;
    }

    public LocalDateTime getVideoGeneratedAt() {
        return videoGeneratedAt;
    }

    public void setVideoGeneratedAt(LocalDateTime videoGeneratedAt) {
        this.videoGeneratedAt = videoGeneratedAt;
    }

    public String getVideoStatus() {
        return videoStatus;
    }

    public void setVideoStatus(String videoStatus) {
        this.videoStatus = videoStatus;
    }

    public String getAudioVideo() {
        return audioVideo;
    }

    public void setAudioVideo(String audioVideo) {
        this.audioVideo = audioVideo;
    }

    public LocalDateTime getAudioGeneratedAt() {
        return audioGeneratedAt;
    }

    public void setAudioGeneratedAt(LocalDateTime audioGeneratedAt) {
        this.audioGeneratedAt = audioGeneratedAt;
    }

    public String getAudioStatus() {
        return audioStatus;
    }

    public void setAudioStatus(String audioStatus) {
        this.audioStatus = audioStatus;
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

    @Override
    public String toString() {
        return "Storyboard{" +
                "id=" + id +
                ", storyId=" + storyId +
                ", scene=" + scene +
                ", script='" + script + '\'' +
                ", imagePrompt='" + imagePrompt + '\'' +
                ", videoPrompt='" + videoPrompt + '\'' +
                ", conceptImage='" + conceptImage + '\'' +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 