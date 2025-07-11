package com.example.springboot_pro.service;

import com.example.springboot_pro.domain.Story;
import com.example.springboot_pro.domain.Storyboard;
import com.example.springboot_pro.dao.StoryRepository;
import com.example.springboot_pro.dao.StoryboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.springboot_pro.ai_agent.StoryAgent;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class StoryServiceImpl implements StoryService {
    
    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private StoryboardRepository storyboardRepository;
    
    @Autowired
    private QianfanService qianfanService;
    
    private final StoryAgent storyAgent = new StoryAgent();

    // 基本CRUD操作
    @Override
    public Story save(Story story) {
        return storyRepository.save(story);
    }

    @Override
    public Story findById(Long id) {
        return storyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Story not found with id: " + id));
    }

    @Override
    public List<Story> getAllStories() {
        return storyRepository.findAll();
    }

    @Override
    public Story updateStory(Long id, Story updatedStory) {
        return storyRepository.findById(id)
                .map(story -> {
                    story.setTitle(updatedStory.getTitle());
                    story.setContent(updatedStory.getContent());
                    story.setUserId(updatedStory.getUserId());
                    return storyRepository.save(story);
                })
                .orElseGet(() -> {
                    updatedStory.setId(id);
                    return storyRepository.save(updatedStory);
                });
    }

    @Override
    public void deleteStory(Long id) {
        storyRepository.deleteById(id);
    }

    // 搜索相关
    @Override
    public List<Story> searchStories(String title) {
        return storyRepository.findByTitleContaining(title);
    }

    @Override
    public List<Story> getStoriesByUserId(Long userId) {
        return storyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Story> searchStoriesByTitleAndUserId(String title, Long userId) {
        return storyRepository.findByTitleContainingAndUserIdOrderByCreatedAtDesc(title, userId);
    }

    // AI生成相关
    @Override
    public String generateStory(String title) {
        return storyAgent.generateInitialStory(title);
    }

    @Override
    public String modifyStory(String summary, String instruction) {
        return storyAgent.modifyStory(summary, instruction);
    }

    @Override
    public List<Storyboard> generateStoryboard(Long storyId) {
        // 获取故事信息
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("故事不存在，ID: " + storyId));
        
        // 删除该故事下的现有分镜头脚本
        List<Storyboard> existingStoryboards = storyboardRepository.findByStoryIdOrderBySceneAsc(storyId);
        if (!existingStoryboards.isEmpty()) {
            storyboardRepository.deleteAll(existingStoryboards);
        }
        
        // 使用QianfanService生成分镜头脚本
        List<Map<String, Object>> storyboardData = qianfanService.generateStoryboard(story.getContent());
        
        // 转换为Storyboard实体并保存
        List<Storyboard> storyboards = new ArrayList<>();
        for (Map<String, Object> data : storyboardData) {
            Storyboard storyboard = new Storyboard();
            storyboard.setStoryId(storyId);
            storyboard.setScene((Integer) data.get("scene"));
            storyboard.setScript((String) data.get("script"));
            storyboard.setImagePrompt((String) data.get("imagePrompt"));
            storyboard.setVideoPrompt((String) data.get("videoPrompt"));
            
            storyboards.add(storyboardRepository.save(storyboard));
        }
        
        return storyboards;
    }
} 