package com.example.springboot_pro.dao;

import com.example.springboot_pro.domain.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByTitleContaining(String title);
    
    // 根据用户ID查询故事（按创建时间倒序）
    List<Story> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 根据标题和用户ID搜索故事（按创建时间倒序）
    List<Story> findByTitleContainingAndUserIdOrderByCreatedAtDesc(String title, Long userId);
} 