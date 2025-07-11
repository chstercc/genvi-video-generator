package com.example.springboot_pro.service;

import com.example.springboot_pro.dao.VideoRepository;
import com.example.springboot_pro.dao.StoryRepository;
import com.example.springboot_pro.dao.StoryboardRepository;
import com.example.springboot_pro.domain.Video;
import com.example.springboot_pro.domain.Story;
import com.example.springboot_pro.domain.Storyboard;
import com.example.springboot_pro.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class VideoServiceImpl implements VideoService {
    
    @Autowired
    private VideoRepository videoRepository;
    
    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private StoryboardRepository storyboardRepository;
    
    @Override
    public Video saveVideo(Video video) {
        return videoRepository.save(video);
    }
    
    @Override
    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
    }
    
    @Override
    public List<Video> getCurrentUserVideos() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        return videoRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
    }
    
    @Override
    public List<Video> searchCurrentUserVideosByTitle(String title) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (title == null || title.trim().isEmpty()) {
            return getCurrentUserVideos();
        }
        return videoRepository.findByUserIdAndTitleContaining(currentUserId, title.trim());
    }
    
    @Override
    public Map<String, Object> getCurrentUserVideoStats() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 完成作品数
        Long totalVideos = videoRepository.countByUserId(currentUserId);
        stats.put("totalVideos", totalVideos != null ? totalVideos : 0L);
        
        // 场景总数
        Long totalScenes = videoRepository.getTotalSceneCountByUserId(currentUserId);
        stats.put("totalScenes", totalScenes != null ? totalScenes : 0L);
        
        // 平均场景数
        Double avgScenes = videoRepository.getAverageSceneCountByUserId(currentUserId);
        stats.put("averageScenes", avgScenes != null ? Math.round(avgScenes * 10.0) / 10.0 : 0.0);
        
        // 本周新作品数
        LocalDateTime weekStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(7);
        Long weeklyVideos = videoRepository.countByUserIdAndCreatedAtAfter(currentUserId, weekStart);
        stats.put("weeklyVideos", weeklyVideos != null ? weeklyVideos : 0L);
        
        return stats;
    }
    
    @Override
    public Video createVideoFromStory(Long storyId, String finalVideoUrl) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        // 获取故事信息
        Optional<Story> storyOpt = storyRepository.findById(storyId);
        if (storyOpt.isEmpty()) {
            throw new RuntimeException("故事不存在");
        }
        
        Story story = storyOpt.get();
        if (!story.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权访问此故事");
        }
        
        // 获取分镜头数量
        List<Storyboard> storyboards = storyboardRepository.findByStoryIdOrderBySceneAsc(storyId);
        
        // 创建视频作品
        Video video = new Video();
        video.setTitle(story.getTitle());
        video.setDescription("基于故事《" + story.getTitle() + "》生成的视频作品");
        video.setStoryId(storyId);
        video.setVideoUrl(finalVideoUrl);
        video.setSceneCount(storyboards.size());
        video.setVideoFormat("mp4");
        video.setResolution("1920x1080");
        video.setStatus("completed");
        
        return videoRepository.save(video);
    }
    
    @Override
    public Video updateVideoStatus(Long videoId, String status) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isEmpty()) {
            throw new RuntimeException("视频不存在");
        }
        
        Video video = videoOpt.get();
        if (!isVideoOwnedByCurrentUser(videoId)) {
            throw new RuntimeException("无权修改此视频");
        }
        
        video.setStatus(status);
        return videoRepository.save(video);
    }
    
    @Override
    public boolean deleteVideo(Long videoId) {
        if (!isVideoOwnedByCurrentUser(videoId)) {
            throw new RuntimeException("无权删除此视频");
        }
        
        try {
            videoRepository.deleteById(videoId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isVideoOwnedByCurrentUser(Long videoId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isEmpty()) {
            return false;
        }
        
        return videoOpt.get().getUserId().equals(currentUserId);
    }
} 