package com.example.springboot_pro.service;

import com.example.springboot_pro.domain.Storyboard;
import java.util.List;

public interface StoryboardService {
    
    /**
     * 获取故事的分镜头脚本
     */
    List<Storyboard> getStoryboardsByStoryId(Long storyId);
    
    /**
     * 生成分镜头脚本
     */
    List<Storyboard> generateStoryboard(Long storyId);
    
    /**
     * 保存分镜头脚本
     */
    Storyboard saveStoryboard(Storyboard storyboard);
    
    /**
     * 更新分镜头脚本
     */
    Storyboard updateStoryboard(Long id, Storyboard updatedStoryboard);
    
    /**
     * 删除分镜头脚本
     */
    void deleteStoryboard(Long id);
    
    /**
     * 更新视频信息
     */
    Storyboard updateVideoInfo(Long id, String videoUrl, String status);
    
    /**
     * 更新音效信息
     */
    Storyboard updateAudioInfo(Long id, String audioVideoUrl, String status);
    
    /**
     * 使用千帆AI生成文生图和图生视频提示词
     */
    Storyboard generateAIPrompts(Long storyboardId);
    
    /**
     * 使用千帆AI生成概念图
     */
    Storyboard generateConceptImage(Long storyboardId);
    
    /**
     * 重新排序故事的所有场景编号
     */
    List<Storyboard> reorderStoryboards(Long storyId);
} 