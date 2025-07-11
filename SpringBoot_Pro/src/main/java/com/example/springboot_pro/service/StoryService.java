package com.example.springboot_pro.service;

import com.example.springboot_pro.domain.Story;
import com.example.springboot_pro.domain.Storyboard;
import java.util.List;

public interface StoryService {
    // 基本CRUD操作
    Story save(Story story);
    Story findById(Long id);
    List<Story> getAllStories();
    Story updateStory(Long id, Story updatedStory);
    void deleteStory(Long id);
    
    // 搜索相关
    List<Story> searchStories(String title);
    List<Story> getStoriesByUserId(Long userId);
    List<Story> searchStoriesByTitleAndUserId(String title, Long userId);
    
    // AI生成相关
    String generateStory(String title);
    String modifyStory(String summary, String instruction);
    List<Storyboard> generateStoryboard(Long storyId);
} 