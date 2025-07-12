package com.example.springboot_pro.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/simple-images")
@CrossOrigin(origins = "*")
public class SimpleImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleImageController.class);
    
    @Value("${image.storage.path:D:/SpringBoot_Pro/pics}")
    private String imageStoragePath;
    

    
    /**
     * 获取具体图片 - 使用路径参数，支持story_ID/filename格式
     */
    @GetMapping("/story_{storyId}/{filename}")
    public ResponseEntity<Resource> getImageByStoryAndFile(@PathVariable String storyId, @PathVariable String filename) {
        String path = "story_" + storyId + "/" + filename;
        return getImageByPath(path);
    }
    
    /**
     * 通用的图片获取方法
     */
    private ResponseEntity<Resource> getImageByPath(String path) {
        try {
            logger.info("请求图片: {}", path);
            
            // 获取完整路径
            String fullPath = Paths.get(imageStoragePath, path).toString();
            File file = new File(fullPath);
            
            logger.info("完整路径: {}", fullPath);
            logger.info("文件是否存在: {}", file.exists());
            
            if (!file.exists()) {
                logger.warn("图片文件不存在: {}", fullPath);
                return ResponseEntity.notFound().build();
            }
            
            // 创建资源
            Resource resource = new FileSystemResource(file);
            
            // 确定媒体类型
            MediaType mediaType = getMediaType(file.getName());
            
            logger.info("返回图片: {} ({})", fullPath, mediaType);
            
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("获取图片失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    

    
    private MediaType getMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "webp":
                return MediaType.parseMediaType("image/webp");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
} 