package com.example.springboot_pro.controller;

import com.example.springboot_pro.domain.Storyboard;
import com.example.springboot_pro.service.StoryboardService;
import com.example.springboot_pro.service.QianfanService;
import com.example.springboot_pro.util.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/storyboards")
public class StoryboardController {
    
    @Resource
    private StoryboardService storyboardService;
    
    @Resource
    private QianfanService qianfanService;
    
    /**
     * 获取故事的分镜头脚本
     */
    @GetMapping("/story/{storyId}")
    public List<Storyboard> getStoryboardsByStoryId(@PathVariable Long storyId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            return storyboardService.getStoryboardsByStoryId(storyId);
        }
        return Collections.emptyList();
    }
    
    /**
     * 生成分镜头脚本
     */
    @PostMapping("/generate/{storyId}")
    public List<Storyboard> generateStoryboard(@PathVariable Long storyId) {
        return storyboardService.generateStoryboard(storyId);
    }
    
    /**
     * 创建或更新分镜头脚本
     */
    @PostMapping
    public Storyboard createStoryboard(@RequestBody Storyboard storyboard) {
        return storyboardService.saveStoryboard(storyboard);
    }
    
    /**
     * 更新分镜头脚本
     */
    @PutMapping("/{id}")
    public Storyboard updateStoryboard(@PathVariable Long id, @RequestBody Storyboard updatedStoryboard) {
        return storyboardService.updateStoryboard(id, updatedStoryboard);
    }
    
    /**
     * 删除分镜头脚本
     */
    @DeleteMapping("/{id}")
    public void deleteStoryboard(@PathVariable Long id) {
        storyboardService.deleteStoryboard(id);
    }
    
    /**
     * 更新视频信息
     */
    @PutMapping("/{id}/video")
    public Storyboard updateVideoInfo(@PathVariable Long id, @RequestBody VideoUpdateRequest request) {
        return storyboardService.updateVideoInfo(id, request.getVideoUrl(), request.getStatus());
    }
    
    /**
     * 使用千帆AI生成文生图和图生视频提示词
     */
    @PostMapping("/{id}/generate-ai-prompts")
    public Storyboard generateAIPrompts(@PathVariable Long id) {
        try {
            return storyboardService.generateAIPrompts(id);
        } catch (Exception e) {
            throw new RuntimeException("生成AI提示词失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用千帆AI生成概念图
     */
    @PostMapping("/{id}/generate-concept-image")
    public Storyboard generateConceptImage(@PathVariable Long id) {
        try {
            return storyboardService.generateConceptImage(id);
        } catch (Exception e) {
            throw new RuntimeException("生成概念图失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新排序故事的所有场景编号
     */
    @PutMapping("/story/{storyId}/reorder")
    public List<Storyboard> reorderStoryboards(@PathVariable Long storyId) {
        return storyboardService.reorderStoryboards(storyId);
    }
    
    /**
     * 测试千帆图像生成API
     */
    @PostMapping("/test-image-generation")
    public Map<String, Object> testImageGeneration(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String prompt = request.getOrDefault("prompt", "画一只小狗");
            String imageUrl = qianfanService.generateImage(prompt);
            result.put("success", true);
            result.put("imageUrl", imageUrl);
            result.put("prompt", prompt);
            result.put("imageSize", "1024x576 (16:9比例)");
            result.put("message", "图片生成成功，使用16:9比例");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * 视频更新请求的内部类
     */
    public static class VideoUpdateRequest {
        private String videoUrl;
        private String status;
        
        public String getVideoUrl() {
            return videoUrl;
        }
        
        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
} 