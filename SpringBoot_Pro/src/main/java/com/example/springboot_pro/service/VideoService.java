package com.example.springboot_pro.service;

import com.example.springboot_pro.domain.Video;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VideoService {
    
    // 保存视频作品
    Video saveVideo(Video video);
    
    // 根据ID查找视频
    Optional<Video> findById(Long id);
    
    // 获取当前用户的所有视频作品
    List<Video> getCurrentUserVideos();
    
    // 根据标题搜索当前用户的视频作品
    List<Video> searchCurrentUserVideosByTitle(String title);
    
    // 获取当前用户的视频统计信息
    Map<String, Object> getCurrentUserVideoStats();
    
    // 根据故事ID创建视频作品
    Video createVideoFromStory(Long storyId, String finalVideoUrl);
    
    // 更新视频状态
    Video updateVideoStatus(Long videoId, String status);
    
    // 删除视频作品
    boolean deleteVideo(Long videoId);
    
    // 检查视频是否属于当前用户
    boolean isVideoOwnedByCurrentUser(Long videoId);
} 