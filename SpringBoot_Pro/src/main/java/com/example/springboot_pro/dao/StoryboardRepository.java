package com.example.springboot_pro.dao;

import com.example.springboot_pro.domain.Storyboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface StoryboardRepository extends JpaRepository<Storyboard, Long> {
    
    /**
     * 根据故事ID查找所有分镜头脚本，按场景编号排序
     */
    List<Storyboard> findByStoryIdOrderBySceneAsc(Long storyId);
    
    /**
     * 根据故事ID和用户ID查找分镜头脚本
     */
    List<Storyboard> findByStoryIdAndUserIdOrderBySceneAsc(Long storyId, Long userId);
    
    /**
     * 根据故事ID删除所有分镜头脚本
     */
    @Modifying
    @Transactional
    void deleteByStoryId(Long storyId);
    
    /**
     * 根据故事ID和场景编号查找分镜头脚本
     */
    Storyboard findByStoryIdAndScene(Long storyId, Integer scene);
    
    /**
     * 检查故事是否存在分镜头脚本
     */
    boolean existsByStoryId(Long storyId);
    
    /**
     * 统计故事的分镜头脚本数量
     */
    @Query("SELECT COUNT(s) FROM Storyboard s WHERE s.storyId = :storyId")
    int countByStoryId(@Param("storyId") Long storyId);
    
    /**
     * 获取故事的最大场景编号
     */
    @Query("SELECT MAX(s.scene) FROM Storyboard s WHERE s.storyId = :storyId")
    Integer findMaxSceneByStoryId(@Param("storyId") Long storyId);
} 