package com.example.springboot_pro.dao;

import com.example.springboot_pro.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    
    // 根据用户ID查找所有视频作品，按创建时间倒序排列
    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 根据用户ID和状态查找视频作品
    List<Video> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    
    // 根据标题模糊搜索用户的视频作品
    @Query("SELECT v FROM Video v WHERE v.userId = :userId AND v.title LIKE %:title% ORDER BY v.createdAt DESC")
    List<Video> findByUserIdAndTitleContaining(@Param("userId") Long userId, @Param("title") String title);
    
    // 根据故事ID查找视频作品
    List<Video> findByStoryIdOrderByCreatedAtDesc(Long storyId);
    
    // 统计用户的视频作品数量
    Long countByUserId(Long userId);
    
    // 统计用户在指定时间之后创建的视频数量
    @Query("SELECT COUNT(v) FROM Video v WHERE v.userId = :userId AND v.createdAt >= :startDate")
    Long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    // 计算用户所有视频的平均场景数
    @Query("SELECT AVG(v.sceneCount) FROM Video v WHERE v.userId = :userId AND v.sceneCount IS NOT NULL")
    Double getAverageSceneCountByUserId(@Param("userId") Long userId);
    
    // 获取用户视频的总场景数
    @Query("SELECT SUM(v.sceneCount) FROM Video v WHERE v.userId = :userId AND v.sceneCount IS NOT NULL")
    Long getTotalSceneCountByUserId(@Param("userId") Long userId);
} 