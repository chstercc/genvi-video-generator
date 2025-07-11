package com.example.springboot_pro.dao;

import com.example.springboot_pro.domain.VideoTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoTaskRepository extends JpaRepository<VideoTask, Long> {
    
    // 根据任务ID查找
    Optional<VideoTask> findByTaskId(String taskId);
    
    // 根据故事板ID查找任务
    List<VideoTask> findByStoryboardIdOrderByCreatedAtDesc(Long storyboardId);
    
    // 根据用户ID查找所有任务
    List<VideoTask> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 根据状态查找任务
    List<VideoTask> findByStatusOrderByCreatedAtDesc(String status);
    
    // 查找用户的特定状态任务
    List<VideoTask> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    
    // 查找需要轮询的任务（状态为submitted或generating）
    @Query("SELECT vt FROM VideoTask vt WHERE vt.status IN ('submitted', 'generating') ORDER BY vt.createdAt DESC")
    List<VideoTask> findPendingTasks();
    
    // 查找超时的任务（创建时间超过指定时间且仍在处理中）
    @Query("SELECT vt FROM VideoTask vt WHERE vt.status IN ('submitted', 'generating') AND vt.createdAt < :timeoutBefore")
    List<VideoTask> findTimeoutTasks(@Param("timeoutBefore") LocalDateTime timeoutBefore);
    
    // 统计用户任务数量
    Long countByUserId(Long userId);
    
    // 统计用户成功完成的任务数量
    Long countByUserIdAndStatus(Long userId, String status);
    
    // 检查故事板是否已有进行中的视频生成任务
    @Query("SELECT COUNT(vt) > 0 FROM VideoTask vt WHERE vt.storyboardId = :storyboardId AND vt.status IN ('submitting', 'submitted', 'generating')")
    boolean hasActiveTaskForStoryboard(@Param("storyboardId") Long storyboardId);
} 