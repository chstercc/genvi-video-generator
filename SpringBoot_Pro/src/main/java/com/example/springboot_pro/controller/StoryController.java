package com.example.springboot_pro.controller;

import com.example.springboot_pro.domain.Story;
import com.example.springboot_pro.domain.Storyboard;
import com.example.springboot_pro.service.StoryService;
import com.example.springboot_pro.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    private StoryService storyService;

    @GetMapping
    public ResponseEntity<List<Story>> getAllStories() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        List<Story> stories = storyService.getStoriesByUserId(currentUserId);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Story> getStoryById(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Story story = storyService.findById(id);
        
        // 验证故事是否属于当前用户
        if (!story.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权访问该故事");
        }
        
        return ResponseEntity.ok(story);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Story>> getStoriesByUserId(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // 只允许获取自己的故事
        if (!userId.equals(currentUserId)) {
            throw new AccessDeniedException("只能获取自己的故事");
        }
        List<Story> stories = storyService.getStoriesByUserId(userId);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Story>> searchStories(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // 只允许搜索自己的故事
        if (userId != null && !userId.equals(currentUserId)) {
            throw new AccessDeniedException("只能搜索自己的故事");
        }
        List<Story> stories = storyService.searchStoriesByTitleAndUserId(title, currentUserId);
        return ResponseEntity.ok(stories);
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateStory(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = storyService.generateStory(title);
        return ResponseEntity.ok(Map.of("content", content));
    }

    @PostMapping("/modify")
    public ResponseEntity<Map<String, String>> modifyStory(@RequestBody Map<String, String> request) {
        String summary = request.get("summary");
        String instruction = request.get("instruction");
        String modifiedContent = storyService.modifyStory(summary, instruction);
        return ResponseEntity.ok(Map.of("content", modifiedContent));
    }

    @PostMapping
    public ResponseEntity<Story> saveStory(@RequestBody Story story) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        story.setUserId(currentUserId); // 确保设置正确的用户ID
        Story savedStory = storyService.save(story);
        return ResponseEntity.ok(savedStory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Story> updateStory(@PathVariable Long id, @RequestBody Story story) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Story existingStory = storyService.findById(id);
        
        // 验证故事是否属于当前用户
        if (!existingStory.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权修改该故事");
        }
        
        story.setUserId(currentUserId); // 确保用户ID不被修改
        Story updatedStory = storyService.updateStory(id, story);
        return ResponseEntity.ok(updatedStory);
    }

    @PostMapping("/{id}/generate-storyboard")
    public ResponseEntity<List<Storyboard>> generateStoryboard(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Story story = storyService.findById(id);
        
        // 验证故事是否属于当前用户
        if (!story.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权为该故事生成分镜头脚本");
        }
        
        List<Storyboard> storyboards = storyService.generateStoryboard(id);
        return ResponseEntity.ok(storyboards);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(@PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Story story = storyService.findById(id);
        
        // 验证故事是否属于当前用户
        if (!story.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权删除该故事");
        }
        
        storyService.deleteStory(id);
        return ResponseEntity.ok().build();
    }
} 