package com.example.springboot_pro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SimpleImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleImageService.class);
    
    @Value("${image.storage.path:D:/SpringBoot_Pro/pics}")
    private String imageStoragePath;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    /**
     * 下载图片并保存到本地，返回简单的访问URL
     */
    public String downloadAndSaveImage(String imageUrl, Long storyId, int sceneNumber) {
        try {
            logger.info("开始下载图片: {}", imageUrl);
            
            // 创建目录
            String storyDir = "story_" + storyId;
            Path directoryPath = Paths.get(imageStoragePath, storyDir);
            Files.createDirectories(directoryPath);
            
            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "scene_" + sceneNumber + "_" + timestamp + ".jpg";
            
            // 下载图片
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                Path filePath = directoryPath.resolve(filename);
                Files.copy(inputStream, filePath);
                logger.info("图片保存成功: {}", filePath);
                
                // 返回简单的访问URL
                String accessUrl = "http://localhost:" + serverPort + "/api/simple-images/story_" + storyId + "/" + filename;
                logger.info("生成访问URL: {}", accessUrl);
                return accessUrl;
            }
            
        } catch (Exception e) {
            logger.error("下载图片失败: {}", e.getMessage(), e);
            throw new RuntimeException("下载图片失败: " + e.getMessage());
        }
    }
} 