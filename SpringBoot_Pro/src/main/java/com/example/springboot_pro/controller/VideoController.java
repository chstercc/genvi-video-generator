package com.example.springboot_pro.controller;

import com.example.springboot_pro.domain.Video;
import com.example.springboot_pro.domain.VideoTask;
import com.example.springboot_pro.service.VideoService;
import com.example.springboot_pro.service.VolcEngineVideoService;
import com.example.springboot_pro.service.StoryboardService;
import com.example.springboot_pro.dao.VideoTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;
    
    @Autowired
    private VolcEngineVideoService volcEngineVideoService;
    
    @Autowired
    private StoryboardService storyboardService;
    
    @Autowired
    private VideoTaskRepository videoTaskRepository;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String OUTPUT_DIR = TEMP_DIR + File.separator + "video_output";
    
    // 持久化视频存储目录
    private static final String PERSISTENT_VIDEO_DIR = "D:" + File.separator + "video_works";
    private static final String VIDEO_ACCESS_URL_BASE = "http://localhost:8080/api/video/works/";

    /**
     * 视频拼接请求参数
     */
    public static class VideoConcatRequest {
        private List<String> videoUrls;
        private String outputName;
        private String backgroundMusic; // 背景音乐文件名

        public List<String> getVideoUrls() {
            return videoUrls;
        }

        public void setVideoUrls(List<String> videoUrls) {
            this.videoUrls = videoUrls;
        }

        public String getOutputName() {
            return outputName;
        }

        public void setOutputName(String outputName) {
            this.outputName = outputName;
        }

        public String getBackgroundMusic() {
            return backgroundMusic;
        }

        public void setBackgroundMusic(String backgroundMusic) {
            this.backgroundMusic = backgroundMusic;
        }
    }

    /**
     * 拼接视频
     */
    @PostMapping("/concat")
    public ResponseEntity<?> concatVideos(@RequestBody VideoConcatRequest request) {
        
        System.out.println("==================== 视频拼接请求开始 ====================");
        
        if (request.getVideoUrls() == null || request.getVideoUrls().isEmpty()) {
            System.err.println("错误: 视频URL列表为空");
            return ResponseEntity.badRequest().body("视频URL列表不能为空");
        }
        
        System.out.println("收到视频拼接请求: " + request.getVideoUrls().size() + " 个视频");
        
        // 验证视频URL
        for (int i = 0; i < request.getVideoUrls().size(); i++) {
            String url = request.getVideoUrls().get(i);
            if (url == null || url.trim().isEmpty()) {
                System.err.println("错误: 第 " + (i + 1) + " 个视频URL为空");
                return ResponseEntity.badRequest().body("第 " + (i + 1) + " 个视频URL为空");
            }
            System.out.println("视频 " + (i + 1) + ": " + url);
        }

        String sessionId = UUID.randomUUID().toString();
        Path tempDir = Paths.get(TEMP_DIR, "video_concat_" + sessionId);
        Path outputDir = Paths.get(OUTPUT_DIR);

        try {
            // 创建临时目录
            Files.createDirectories(tempDir);
            Files.createDirectories(outputDir);

            // 下载视频文件
            List<String> localVideoPaths = new ArrayList<>();
            for (int i = 0; i < request.getVideoUrls().size(); i++) {
                String videoUrl = request.getVideoUrls().get(i);
                String localPath = downloadVideo(videoUrl, tempDir, i);
                if (localPath != null) {
                    localVideoPaths.add(localPath);
                }
            }

            if (localVideoPaths.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("没有成功下载到任何视频文件");
            }

            System.out.println("成功下载 " + localVideoPaths.size() + " 个视频文件");

            // 生成拼接列表文件
            String concatListPath = createConcatList(localVideoPaths, tempDir);

            // 执行FFmpeg拼接（先拼接视频，不处理音频）
            String tempVideoName = "temp_concat_" + sessionId + ".mp4";
            Path tempVideoPath = outputDir.resolve(tempVideoName);
            
            // 第一步：仅拼接视频
            boolean success = executeFFmpegConcat(concatListPath, tempVideoPath.toString());
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("视频拼接失败");
            }
            
            // 第二步：如果有背景音乐，添加音乐
            String outputFileName = request.getOutputName() != null ? 
                request.getOutputName() : ("concat_video_" + sessionId + ".mp4");
            Path outputPath = outputDir.resolve(outputFileName);
            
            if (request.getBackgroundMusic() != null && !request.getBackgroundMusic().trim().isEmpty()) {
                success = addMusicToVideo(tempVideoPath.toString(), request.getBackgroundMusic(), outputPath.toString());
                // 删除临时视频文件
                try {
                    Files.deleteIfExists(tempVideoPath);
                } catch (IOException e) {
                    System.err.println("删除临时视频文件失败: " + e.getMessage());
                }
            } else {
                // 没有音乐，直接重命名临时文件
                try {
                    Files.move(tempVideoPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("重命名视频文件失败: " + e.getMessage());
                    success = false;
                }
            }

            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("视频拼接失败");
            }

            // 检查输出文件是否存在
            if (!Files.exists(outputPath)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("输出文件不存在");
            }

            // 创建持久化存储目录
            Path persistentDir = Paths.get(PERSISTENT_VIDEO_DIR);
            Files.createDirectories(persistentDir);
            
            // 生成唯一的文件名
            String timestamp = String.valueOf(System.currentTimeMillis());
            String persistentFileName = timestamp + "_" + outputFileName;
            Path persistentPath = persistentDir.resolve(persistentFileName);
            
            // 复制文件到持久化目录
            Files.copy(outputPath, persistentPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("视频已保存到持久化目录: " + persistentPath.toString());
            
            // 生成访问URL
            String accessUrl = VIDEO_ACCESS_URL_BASE + persistentFileName;
            
            // 返回包含访问URL的JSON响应，而不是直接返回文件
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "视频拼接成功");
            result.put("videoUrl", accessUrl);
            result.put("fileName", persistentFileName);
            result.put("fileSize", Files.size(persistentPath));
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            return ResponseEntity.ok()
                .headers(headers)
                .body(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("视频拼接过程中发生错误: " + e.getMessage());
        } finally {
            // 清理临时文件
            System.out.println("开始清理临时文件...");
            cleanupTempFiles(tempDir);
            System.out.println("==================== 视频拼接请求结束 ====================");
        }
    }

    /**
     * 下载视频文件或处理本地文件
     */
    private String downloadVideo(String videoUrl, Path tempDir, int index) {
        try {
            System.out.println("正在处理视频 " + (index + 1) + ": " + videoUrl);
            
            // 检查是否为本地视频API
            if (videoUrl.contains("/api/video/local/")) {
                // 提取文件名
                String filename = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
                
                // 本地视频文件路径映射（与getLocalVideo方法保持一致）
                Map<String, String> videoFiles = new HashMap<>();
                videoFiles.put("scene1-1", "D:\\courses\\场景设计\\杭州的未来\\镜头1.1.mp4");
                videoFiles.put("scene1-2", "D:\\courses\\场景设计\\杭州的未来\\镜头1.2.mp4");
                videoFiles.put("scene1-transition", "D:\\courses\\场景设计\\杭州的未来\\镜头1转换.mp4");
                videoFiles.put("scene2-1", "D:\\courses\\场景设计\\杭州的未来\\镜头2.1.mp4");
                videoFiles.put("scene6-1", "D:\\courses\\场景设计\\杭州的未来\\镜头6 (1).mp4");
                videoFiles.put("scene6-2", "D:\\courses\\场景设计\\杭州的未来\\镜头6 (2).mp4");
                videoFiles.put("scene7-1", "D:\\courses\\场景设计\\杭州的未来\\镜头7 (1).mp4");
                videoFiles.put("scene7-2", "D:\\courses\\场景设计\\杭州的未来\\镜头7 (2).mp4");
                videoFiles.put("scene7-3", "D:\\courses\\场景设计\\杭州的未来\\镜头7 (3).mp4");
                
                String localPath = videoFiles.get(filename);
                if (localPath != null) {
                    Path originalFile = Paths.get(localPath);
                    
                    if (Files.exists(originalFile)) {
                        System.out.println("找到本地视频文件: " + originalFile.toString());
                        
                        // 验证是否为有效的视频文件
                        if (isValidVideoFile(originalFile)) {
                            System.out.println("本地视频文件验证成功: " + originalFile.toString());
                            return originalFile.toString();
                        } else {
                            System.err.println("本地文件不是有效的视频文件: " + localPath);
                            return null;
                        }
                    } else {
                        System.err.println("本地视频文件不存在: " + localPath);
                        return null;
                    }
                } else {
                    System.err.println("未找到对应的本地视频文件: " + filename);
                    return null;
                }
            }
            
            // 检查是否为直接的本地文件路径
            if (videoUrl.startsWith("file://") || (videoUrl.contains(":\\") && !videoUrl.startsWith("http"))) {
                // 处理本地文件
                String localPath = videoUrl.startsWith("file://") ? videoUrl.substring(7) : videoUrl;
                Path originalFile = Paths.get(localPath);
                
                if (Files.exists(originalFile)) {
                    System.out.println("找到本地视频文件: " + originalFile.toString());
                    
                    // 验证是否为有效的视频文件
                    if (isValidVideoFile(originalFile)) {
                        System.out.println("本地视频文件验证成功: " + originalFile.toString());
                        return originalFile.toString();
                    } else {
                        System.err.println("本地文件不是有效的视频文件: " + localPath);
                        return null;
                    }
                } else {
                    System.err.println("本地视频文件不存在: " + localPath);
                    return null;
                }
            }
            
            // 处理网络URL
            URL url = new URL(videoUrl);
            String fileName = "video_" + index + ".mp4";
            Path filePath = tempDir.resolve(fileName);

            // 设置连接超时
            java.net.URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(60000); // 60秒读取超时
            
            // 设置User-Agent以避免被某些服务器拒绝
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            try (InputStream in = connection.getInputStream()) {
                long bytesDownloaded = Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("下载了 " + bytesDownloaded + " 字节");
            }

            if (Files.exists(filePath)) {
                long fileSize = Files.size(filePath);
                if (fileSize > 0) {
                    System.out.println("视频下载成功: " + filePath.toString() + " (大小: " + fileSize + " 字节)");
                    
                    // 验证是否为有效的视频文件
                    if (isValidVideoFile(filePath)) {
                        return filePath.toString();
                    } else {
                        System.err.println("下载的文件不是有效的视频文件: " + videoUrl);
                        Files.deleteIfExists(filePath);
                        return null;
                    }
                } else {
                    System.err.println("下载的文件为空: " + videoUrl);
                    Files.deleteIfExists(filePath);
                    return null;
                }
            } else {
                System.err.println("文件下载后不存在: " + videoUrl);
                return null;
            }

        } catch (java.net.MalformedURLException e) {
            System.err.println("无效的URL: " + videoUrl + " - " + e.getMessage());
            return null;
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("下载超时: " + videoUrl + " - " + e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            System.err.println("下载IO错误: " + videoUrl + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("下载视频时发生未知错误: " + videoUrl + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 验证是否为有效的视频文件
     */
    private boolean isValidVideoFile(Path filePath) {
        try {
            // 简单验证：检查文件头部字节
            byte[] header = new byte[12];
            try (InputStream is = Files.newInputStream(filePath)) {
                int bytesRead = is.read(header);
                if (bytesRead < 4) {
                    return false;
                }
            }
            
            // 检查常见视频文件的魔术字节
            // MP4: starts with "ftyp" at offset 4
            if (header.length >= 8) {
                String headerStr = new String(header, 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (headerStr.equals("ftyp")) {
                    return true;
                }
            }
            
            // WebM: starts with 0x1A 0x45 0xDF 0xA3
            if (header[0] == 0x1A && header[1] == 0x45 && header[2] == (byte)0xDF && header[3] == (byte)0xA3) {
                return true;
            }
            
            // AVI: starts with "RIFF" and contains "AVI "
            if (header.length >= 12) {
                String riff = new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
                String avi = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (riff.equals("RIFF") && avi.equals("AVI ")) {
                    return true;
                }
            }
            
            System.out.println("未识别的文件格式，假设为有效视频文件");
            return true; // 对于未识别的格式，假设为有效
            
        } catch (Exception e) {
            System.err.println("验证视频文件时发生错误: " + e.getMessage());
            return true; // 验证失败时假设为有效
        }
    }

    /**
     * 创建FFmpeg拼接列表文件
     */
    private String createConcatList(List<String> videoPaths, Path tempDir) throws IOException {
        Path concatListPath = tempDir.resolve("concat_list.txt");
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(concatListPath, java.nio.charset.StandardCharsets.UTF_8))) {
            for (String videoPath : videoPaths) {
                // 使用绝对路径，Windows下FFmpeg支持反斜杠，但要确保路径正确
                Path path = Paths.get(videoPath).toAbsolutePath().normalize();
                writer.println("file '" + path.toString().replace("'", "\\'") + "'");
                System.out.println("添加到拼接列表: " + path.toString());
            }
        }
        
        System.out.println("拼接列表文件创建成功: " + concatListPath.toString());
        
        // 打印拼接列表内容用于调试
        try {
            List<String> lines = Files.readAllLines(concatListPath, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("拼接列表内容:");
            for (String line : lines) {
                System.out.println("  " + line);
            }
        } catch (IOException e) {
            System.err.println("无法读取拼接列表文件: " + e.getMessage());
        }
        
        return concatListPath.toString();
    }

    /**
     * 执行FFmpeg拼接命令（仅拼接视频）
     */
    private boolean executeFFmpegConcat(String concatListPath, String outputPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-f");
            command.add("concat");
            command.add("-safe");
            command.add("0");
            command.add("-i");
            command.add(concatListPath);
            command.add("-c");
            command.add("copy"); // 直接拷贝，不重新编码
            command.add("-y"); // 覆盖输出文件
            command.add(outputPath);

            System.out.println("执行FFmpeg视频拼接命令: " + String.join(" ", command));
            
            // 检查输入文件是否存在
            if (!Files.exists(Paths.get(concatListPath))) {
                System.err.println("拼接列表文件不存在: " + concatListPath);
                return false;
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            // 设置工作目录
            processBuilder.directory(new File(System.getProperty("java.io.tmpdir")));
            
            Process process = processBuilder.start();

            // 读取输出信息
            StringBuilder ffmpegOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg输出: " + line);
                    ffmpegOutput.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS); // 5分钟超时

            if (!finished) {
                System.err.println("FFmpeg进程超时");
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                System.out.println("FFmpeg拼接成功，输出文件: " + outputPath);
                
                // 验证输出文件
                Path outputFilePath = Paths.get(outputPath);
                if (Files.exists(outputFilePath)) {
                    long fileSize = Files.size(outputFilePath);
                    System.out.println("输出文件大小: " + fileSize + " 字节");
                    return fileSize > 0;
                } else {
                    System.err.println("输出文件不存在: " + outputPath);
                    return false;
                }
            } else {
                System.err.println("FFmpeg拼接失败，退出码: " + exitCode);
                System.err.println("FFmpeg完整输出:");
                System.err.println(ffmpegOutput.toString());
                return false;
            }

        } catch (Exception e) {
            System.err.println("执行FFmpeg命令时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 为视频添加背景音乐
     */
    private boolean addMusicToVideo(String videoPath, String musicFileName, String outputPath) {
        try {
            String musicPath = "D:/SpringBoot_Pro/musics/" + musicFileName;
            
            // 检查音乐文件是否存在
            if (!Files.exists(Paths.get(musicPath))) {
                System.err.println("背景音乐文件不存在: " + musicPath);
                return false;
            }
            
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(videoPath); // 输入视频
            command.add("-i");
            command.add(musicPath); // 输入音乐
            
            // 检查视频是否有音频轨道
            boolean hasVideoAudio = checkVideoHasAudio(videoPath);
            
            if (hasVideoAudio) {
                // 如果视频有音轨，混合原音轨和背景音乐
                command.add("-filter_complex");
                command.add("[0:a][1:a]amix=inputs=2:duration=shortest:weights=0.7 0.4[aout]");
                command.add("-map");
                command.add("0:v"); // 映射视频流
                command.add("-map");
                command.add("[aout]"); // 映射混合后的音频
                System.out.println("视频包含音轨，将混合原音效与背景音乐");
            } else {
                // 如果视频没有音轨，直接使用背景音乐
            command.add("-map");
            command.add("0:v"); // 映射视频流
            command.add("-map");
            command.add("1:a"); // 映射音乐音频流
                System.out.println("视频不包含音轨，直接使用背景音乐");
            }
            
            command.add("-c:v");
            command.add("copy"); // 视频流直接拷贝
            command.add("-c:a");
            command.add("aac"); // 音频重新编码为AAC
            command.add("-shortest"); // 以最短流为准
            command.add("-y"); // 覆盖输出文件
            command.add(outputPath);

            System.out.println("执行FFmpeg添加音乐命令: " + String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(new File(System.getProperty("java.io.tmpdir")));
            
            Process process = processBuilder.start();

            // 读取输出信息
            StringBuilder ffmpegOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg音乐添加输出: " + line);
                    ffmpegOutput.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS); // 5分钟超时

            if (!finished) {
                System.err.println("FFmpeg音乐添加进程超时");
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                System.out.println("FFmpeg音乐添加成功，输出文件: " + outputPath);
                
                // 验证输出文件
                Path outputFilePath = Paths.get(outputPath);
                if (Files.exists(outputFilePath)) {
                    long fileSize = Files.size(outputFilePath);
                    System.out.println("输出文件大小: " + fileSize + " 字节");
                    return fileSize > 0;
                } else {
                    System.err.println("输出文件不存在: " + outputPath);
                    return false;
                }
            } else {
                System.err.println("FFmpeg音乐添加失败，退出码: " + exitCode);
                System.err.println("FFmpeg完整输出:");
                System.err.println(ffmpegOutput.toString());
                return false;
            }

        } catch (Exception e) {
            System.err.println("执行FFmpeg音乐添加命令时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查视频是否包含音频轨道
     */
    private boolean checkVideoHasAudio(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffprobe");
            command.add("-v");
            command.add("quiet");
            command.add("-show_streams");
            command.add("-select_streams");
            command.add("a"); // 只检查音频流
            command.add("-of");
            command.add("csv=p=0");
            command.add(videoPath);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("检查视频音频轨道超时");
                return false;
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString().trim();
            
            // 如果有音频流，ffprobe会返回音频流信息；如果没有，则输出为空
            boolean hasAudio = exitCode == 0 && !outputStr.isEmpty();
            
            System.out.println("音频轨道检查结果 - 文件: " + videoPath + ", 有音频: " + hasAudio);
            if (hasAudio) {
                System.out.println("检测到音频流信息: " + outputStr);
            }
            
            return hasAudio;
            
        } catch (Exception e) {
            System.err.println("检查视频音频轨道时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 出错时默认假设有音频，采用混合模式更安全
            return true;
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a)) // 先删除文件，再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("删除临时文件失败: " + path + " - " + e.getMessage());
                        }
                    });
            }
        } catch (Exception e) {
            System.err.println("清理临时文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 提供持久化视频作品文件访问
     */
    @GetMapping("/works/{filename}")
    public ResponseEntity<FileSystemResource> getPersistentVideo(@PathVariable String filename) {
        try {
            Path videoPath = Paths.get(PERSISTENT_VIDEO_DIR, filename);
            
            if (!Files.exists(videoPath)) {
                System.err.println("持久化视频文件不存在: " + videoPath.toString());
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(videoPath.toFile());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600"); // 1小时缓存
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .body(resource);
                
        } catch (Exception e) {
            System.err.println("获取持久化视频失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 提供本地视频文件访问
     */
    @GetMapping("/local/{filename}")
    public ResponseEntity<FileSystemResource> getLocalVideo(@PathVariable String filename) {
        try {
            // 本地视频文件路径映射
            Map<String, String> videoFiles = new HashMap<>();
            videoFiles.put("scene1-1", "D:\\courses\\场景设计\\杭州的未来\\镜头1.1.mp4");
            videoFiles.put("scene1-2", "D:\\courses\\场景设计\\杭州的未来\\镜头1.2.mp4");
            videoFiles.put("scene1-transition", "D:\\courses\\场景设计\\杭州的未来\\镜头1转换.mp4");
            videoFiles.put("scene2-1", "D:\\courses\\场景设计\\杭州的未来\\镜头2.1.mp4");
            videoFiles.put("scene6-1", "D:\\courses\\场景设计\\杭州的未来\\镜头6 (1).mp4");
            videoFiles.put("scene6-2", "D:\\courses\\场景设计\\杭州的未来\\镜头6 (2).mp4");
            videoFiles.put("scene7-1", "D:\\courses\\场景设计\\杭州的未来\\镜头7 (1).mp4");
            videoFiles.put("scene7-2", "D:\\courses\\场景设计\\杭州的未来\\镜头7 (2).mp4");
            videoFiles.put("scene7-3", "D:\\courses\\场景设计\\杭州的未来\\镜头7 (3).mp4");
            
            String filePath = videoFiles.get(filename);
            if (filePath == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path videoPath = Paths.get(filePath);
            if (!Files.exists(videoPath)) {
                System.err.println("本地视频文件不存在: " + filePath);
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(videoPath.toFile());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + ".mp4\"");
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .body(resource);
                
        } catch (Exception e) {
            System.err.println("获取本地视频失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 测试FFmpeg是否可用
     */
    @GetMapping("/test-ffmpeg")
    public ResponseEntity<?> testFFmpeg() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                
                if (finished && process.exitValue() == 0) {
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "FFmpeg可用",
                        "version", output.toString()
                    ));
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "success", false,
                            "message", "FFmpeg不可用或版本获取失败"
                        ));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "FFmpeg测试失败: " + e.getMessage()
                ));
        }
    }

    // ==================== 用户作品管理API ====================

    /**
     * 获取当前用户的所有视频作品
     */
    @GetMapping("/my-works")
    public ResponseEntity<?> getCurrentUserVideos() {
        try {
            List<Video> videos = videoService.getCurrentUserVideos();
            return ResponseEntity.ok(videos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取用户作品失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户的视频作品统计信息
     */
    @GetMapping("/my-works/stats")
    public ResponseEntity<?> getCurrentUserVideoStats() {
        try {
            Map<String, Object> stats = videoService.getCurrentUserVideoStats();
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 根据标题搜索当前用户的视频作品
     */
    @GetMapping("/my-works/search")
    public ResponseEntity<?> searchCurrentUserVideos(@RequestParam(required = false) String title) {
        try {
            List<Video> videos = videoService.searchCurrentUserVideosByTitle(title);
            return ResponseEntity.ok(videos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "搜索用户作品失败: " + e.getMessage()));
        }
    }

    /**
     * 根据故事ID创建视频作品
     */
    @PostMapping("/create-from-story")
    public ResponseEntity<?> createVideoFromStory(@RequestBody Map<String, Object> request) {
        try {
            Long storyId = Long.valueOf(request.get("storyId").toString());
            String finalVideoUrl = request.get("finalVideoUrl").toString();
            
            Video video = videoService.createVideoFromStory(storyId, finalVideoUrl);
            return ResponseEntity.ok(video);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "创建视频作品失败: " + e.getMessage()));
        }
    }

    /**
     * 更新视频状态
     */
    @PutMapping("/{videoId}/status")
    public ResponseEntity<?> updateVideoStatus(@PathVariable Long videoId, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            Video video = videoService.updateVideoStatus(videoId, status);
            return ResponseEntity.ok(video);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "更新视频状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除视频作品
     */
    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId) {
        try {
            boolean success = videoService.deleteVideo(videoId);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "视频删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "视频删除失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "删除视频失败: " + e.getMessage()));
        }
    }

    /**
     * 获取单个视频详情
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideoById(@PathVariable Long videoId) {
        try {
            var videoOpt = videoService.findById(videoId);
            if (videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Video video = videoOpt.get();
            if (!videoService.isVideoOwnedByCurrentUser(videoId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "无权访问此视频"));
            }
            
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取视频详情失败: " + e.getMessage()));
        }
    }
    
    // ==================== 图生视频相关接口 ====================
    
    /**
     * 图生视频请求参数
     */
    public static class VideoGenerationRequest {
        private Long storyboardId;
        private String imageUrl;
        private String prompt;
        private String aspectRatio;
        
        public Long getStoryboardId() {
            return storyboardId;
        }
        
        public void setStoryboardId(Long storyboardId) {
            this.storyboardId = storyboardId;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
        
        public String getPrompt() {
            return prompt;
        }
        
        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
        
        public String getAspectRatio() {
            return aspectRatio;
        }
        
        public void setAspectRatio(String aspectRatio) {
            this.aspectRatio = aspectRatio;
        }
    }
    
    /**
     * 提交图生视频任务
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateVideo(@RequestBody VideoGenerationRequest request) {
        try {
            // 验证参数
            if (request.getStoryboardId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "故事板ID不能为空"));
            }
            
            if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "图片URL不能为空"));
            }
            
            // 检查是否已有正在进行的任务
            boolean hasActiveTask = videoTaskRepository.hasActiveTaskForStoryboard(request.getStoryboardId());
            if (hasActiveTask) {
                return ResponseEntity.badRequest().body(Map.of("error", "该故事板已有正在进行的视频生成任务"));
            }
            
            // 设置默认值
            String aspectRatio = request.getAspectRatio();
            if (aspectRatio == null || aspectRatio.trim().isEmpty()) {
                aspectRatio = "16:9"; // 默认比例
            }
            
            // 先调用山火API提交任务获取task_id
            VolcEngineVideoService.VideoTaskResponse response = volcEngineVideoService.submitVideoTask(
                request.getImageUrl().trim(),
                request.getPrompt(),
                aspectRatio
            );
            
            if (response.isSuccess()) {
                // API调用成功，创建任务记录
                VideoTask videoTask = new VideoTask(
                    request.getStoryboardId(),
                    request.getImageUrl().trim(),
                    request.getPrompt(),
                    aspectRatio
                );
                
                // 设置从山火API获取的任务ID和状态
                videoTask.setTaskId(response.getTaskId());
                videoTask.setStatus("submitted");
                
                // 保存到数据库
                videoTask = videoTaskRepository.save(videoTask);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "视频生成任务提交成功");
                result.put("taskId", response.getTaskId());
                result.put("videoTaskId", videoTask.getId());
                
                return ResponseEntity.ok(result);
            } else {
                // API调用失败，直接返回错误
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "提交视频生成任务失败: " + response.getMessage()));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "视频生成任务提交失败: " + e.getMessage()));
        }
    }
    
    /**
     * 查询图生视频任务状态
     */
    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<?> getVideoTaskStatus(@PathVariable String taskId) {
        try {
            // 查找本地任务记录
            var videoTaskOpt = videoTaskRepository.findByTaskId(taskId);
            if (videoTaskOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            VideoTask videoTask = videoTaskOpt.get();
            
            // 如果任务已完成或失败，直接返回状态
            if ("completed".equals(videoTask.getStatus()) || "failed".equals(videoTask.getStatus())) {
                Map<String, Object> result = new HashMap<>();
                result.put("taskId", taskId);
                result.put("status", videoTask.getStatus());
                result.put("videoUrl", videoTask.getVideoUrl());
                result.put("errorMessage", videoTask.getErrorMessage());
                result.put("createdAt", videoTask.getCreatedAt());
                result.put("completedAt", videoTask.getCompletedAt());
                
                return ResponseEntity.ok(result);
            }
            
            // 查询山火API获取最新状态
            VolcEngineVideoService.VideoTaskResponse response = volcEngineVideoService.queryVideoTask(taskId);
            
            if (response.isSuccess()) {
                // 更新本地任务状态
                if ("done".equals(response.getStatus())) {
                    // 下载视频到本地
                    String originalVideoUrl = response.getVideoUrl();
                    String localVideoUrl = volcEngineVideoService.downloadVideoToLocal(originalVideoUrl, taskId);
                    
                    videoTask.setStatus("completed");
                    videoTask.setVideoUrl(localVideoUrl);
                } else {
                    videoTask.setStatus("generating");
                }
                videoTaskRepository.save(videoTask);
                
                Map<String, Object> result = new HashMap<>();
                result.put("taskId", taskId);
                result.put("status", videoTask.getStatus());
                result.put("videoUrl", videoTask.getVideoUrl());
                result.put("message", response.getMessage());
                result.put("createdAt", videoTask.getCreatedAt());
                result.put("completedAt", videoTask.getCompletedAt());
                
                return ResponseEntity.ok(result);
            } else {
                // API查询失败，更新为失败状态
                videoTask.setStatus("failed");
                videoTask.setErrorMessage(response.getMessage());
                videoTaskRepository.save(videoTask);
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询任务状态失败: " + response.getMessage()));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "查询任务状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取故事板的视频生成任务列表
     */
    @GetMapping("/tasks/storyboard/{storyboardId}")
    public ResponseEntity<?> getStoryboardVideoTasks(@PathVariable Long storyboardId) {
        try {
            List<VideoTask> videoTasks = videoTaskRepository.findByStoryboardIdOrderByCreatedAtDesc(storyboardId);
            
            return ResponseEntity.ok(videoTasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取任务列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户的所有视频生成任务
     */
    @GetMapping("/tasks/my")
    public ResponseEntity<?> getMyVideoTasks() {
        try {
            // 这里需要获取当前用户ID，简化处理，可以通过SecurityUtil获取
            // 或者暂时返回所有任务
            List<VideoTask> videoTasks = videoTaskRepository.findAll();
            
            return ResponseEntity.ok(videoTasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取我的任务列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 访问本地视频文件
     */
    @GetMapping("/files/{filename}")
    public ResponseEntity<FileSystemResource> getLocalVideoFile(@PathVariable String filename) {
        try {
            // 验证文件名格式，防止路径遍历攻击
            if (!filename.matches("video_[a-zA-Z0-9_]+\\.mp4")) {
                return ResponseEntity.badRequest().build();
            }
            
            Path filePath = Paths.get("D:/SpringBoot_Pro/videos", filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000"); // 缓存1年
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .body(resource);
                
        } catch (Exception e) {
            logger.error("获取本地视频文件失败: " + filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 音效文件复制请求
     */
    public static class AudioFileCopyRequest {
        private String sourceFilePath;
        private Long storyboardId; // 添加故事板ID用于更新数据库
        
        public String getSourceFilePath() {
            return sourceFilePath;
        }
        
        public void setSourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
        }
        
        public Long getStoryboardId() {
            return storyboardId;
        }
        
        public void setStoryboardId(Long storyboardId) {
            this.storyboardId = storyboardId;
        }
    }

    /**
     * 复制音效文件到本地目录并返回访问URL
     */
    @PostMapping("/copy-audio-file")
    public ResponseEntity<?> copyAudioFile(@RequestBody AudioFileCopyRequest request) {
        try {
            String sourceFilePath = request.getSourceFilePath();
            
            if (sourceFilePath == null || sourceFilePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "源文件路径不能为空"));
            }
            
            Path sourcePath = Paths.get(sourceFilePath);
            
            // 验证源文件是否存在
            if (!Files.exists(sourcePath)) {
                return ResponseEntity.badRequest().body(Map.of("error", "源文件不存在: " + sourceFilePath));
            }
            
            // 验证文件是否为视频文件
            String fileName = sourcePath.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".mp4") && !fileName.endsWith(".avi") && !fileName.endsWith(".mov")) {
                return ResponseEntity.badRequest().body(Map.of("error", "只支持视频文件格式 (.mp4, .avi, .mov)"));
            }
            
            // 创建目标目录
            Path targetDirPath = Paths.get("D:/SpringBoot_Pro/videos");
            Files.createDirectories(targetDirPath);
            
            // 生成新的文件名（音效文件专用格式）
            String originalFileName = sourcePath.getFileName().toString();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
            String baseFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
            String timestamp = String.valueOf(System.currentTimeMillis());
            String newFileName = "audio_" + baseFileName + "_" + timestamp + fileExtension;
            
            Path targetPath = targetDirPath.resolve(newFileName);
            
            // 复制文件
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 构建访问URL - 使用新的音效文件接口
            String accessUrl = "http://localhost:8080/api/video/audio-files/" + newFileName;
            
            // 如果提供了故事板ID，更新数据库中的音效信息
            if (request.getStoryboardId() != null) {
                try {
                    storyboardService.updateAudioInfo(request.getStoryboardId(), accessUrl, "completed");
                    logger.info("数据库音效信息更新成功: storyboardId={}, audioUrl={}", request.getStoryboardId(), accessUrl);
                } catch (Exception e) {
                    logger.warn("更新数据库音效信息失败: {}", e.getMessage());
                    // 不影响文件复制的成功返回，只是记录警告
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文件复制成功" + (request.getStoryboardId() != null ? "，数据库已更新" : ""));
            result.put("fileName", newFileName);
            result.put("accessUrl", accessUrl);
            result.put("filePath", targetPath.toString());
            
            logger.info("音效文件复制成功: {} -> {}", sourceFilePath, targetPath);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("复制音效文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "复制文件失败: " + e.getMessage()));
        }
    }

    /**
     * 访问音效视频文件
     */
    @GetMapping("/audio-files/{filename}")
    public ResponseEntity<FileSystemResource> getAudioVideoFile(@PathVariable String filename) {
        try {
            // 验证音效文件名格式，防止路径遍历攻击
            if (!filename.matches("audio_.*\\.(mp4|avi|mov)")) {
                return ResponseEntity.badRequest().build();
            }
            
            Path filePath = Paths.get("D:/SpringBoot_Pro/videos", filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000"); // 缓存1年
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .body(resource);
                
        } catch (Exception e) {
            logger.error("获取音效视频文件失败: " + filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 