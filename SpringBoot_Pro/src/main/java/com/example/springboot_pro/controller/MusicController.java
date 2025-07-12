package com.example.springboot_pro.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/music")
@CrossOrigin(origins = "*")
public class MusicController {

    private static final String MUSIC_DIRECTORY = "D:/SpringBoot_Pro/musics";

    /**
     * 获取所有可用的音乐文件列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getMusicList() {
        try {
            File musicDir = new File(MUSIC_DIRECTORY);
            if (!musicDir.exists() || !musicDir.isDirectory()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "音乐目录不存在",
                    "musicList", new ArrayList<>()
                ));
            }

            List<Map<String, Object>> musicList = new ArrayList<>();
            File[] files = musicDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".mp3") || 
                name.toLowerCase().endsWith(".wav") || 
                name.toLowerCase().endsWith(".m4a"));

            if (files != null) {
                for (File file : files) {
                    Map<String, Object> musicInfo = new HashMap<>();
                    musicInfo.put("name", file.getName());
                    musicInfo.put("displayName", getDisplayName(file.getName()));
                    musicInfo.put("size", file.length());
                    musicInfo.put("url", "/api/music/file/" + file.getName());
                    musicList.add(musicInfo);
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "获取音乐列表成功",
                "musicList", musicList
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "获取音乐列表失败: " + e.getMessage(),
                    "musicList", new ArrayList<>()
                ));
        }
    }

    /**
     * 提供音乐文件下载/流式传输
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getMusicFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(MUSIC_DIRECTORY, fileName);
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            // 安全检查 - 确保文件在指定目录内
            String canonicalDir = new File(MUSIC_DIRECTORY).getCanonicalPath();
            String canonicalFile = file.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = getContentType(fileName);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(file.length())
                .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取音乐文件的内容类型
     */
    private String getContentType(String fileName) {
        String extension = fileName.toLowerCase();
        if (extension.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (extension.endsWith(".wav")) {
            return "audio/wav";
        } else if (extension.endsWith(".m4a")) {
            return "audio/mp4";
        }
        return "application/octet-stream";
    }

    /**
     * 获取音乐文件的显示名称
     */
    private String getDisplayName(String fileName) {
        // 移除文件扩展名并美化显示
        String nameWithoutExt = fileName.replaceAll("\\.[^.]+$", "");
        
        // 简单的名称美化
        switch (nameWithoutExt.toLowerCase()) {
            case "music1":
                return "古典背景音乐";
            case "music2":
                return "悠闲背景音乐";
            case "music3":
                return "热烈背景音乐";
            default:
                return nameWithoutExt;
        }
    }
} 