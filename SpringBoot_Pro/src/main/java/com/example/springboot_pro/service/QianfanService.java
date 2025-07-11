package com.example.springboot_pro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.util.*;
import java.io.IOException;

@Service
public class QianfanService {
    
    private static final Logger logger = LoggerFactory.getLogger(QianfanService.class);
    
    // 这里需要配置您的千帆API访问令牌
    @Value("${qianfan.api.token:bce-v3/ALTAK-*********/614fb**********}")
    private String apiToken;
    
    // 图片尺寸配置 - 16:9比例
    @Value("${qianfan.image.width:1024}")
    private int imageWidth;
    
    @Value("${qianfan.image.height:576}")
    private int imageHeight;
    
    private static final String QIANFAN_API_URL = "https://qianfan.gz.baidubce.com/v2/chat/completions";
    private static final String QIANFAN_IMAGE_API_URL = "https://qianfan.baidubce.com/v2/images/generations";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public QianfanService() {
        this.restTemplate = new RestTemplate();
        // 设置自定义错误处理器，不要在4xx和5xx错误时抛出异常
        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false; // 不将任何状态码视为错误，让我们自己处理
            }
            
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // 不处理错误，让我们自己在代码中处理
            }
        });
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 根据分镜头脚本生成文生图和图生视频提示词
     */
    public Map<String, String> generatePrompts(String script) {
        try {
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ernie-3.5-8k");
            requestBody.put("stream", false);
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // System消息 - 定义AI助手的角色
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的视频制作助手，擅长根据分镜头脚本生成高质量的文生图提示词和图生视频提示词。你的回答应该准确、专业、富有创意。");
            messages.add(systemMessage);
            
            // User消息 - 用户的具体需求
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", buildPromptGenerationRequest(script));
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(QIANFAN_API_URL, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseQianfanResponse(response.getBody());
            } else {
                logger.error("千帆API调用失败，状态码: {}", response.getStatusCode());
                return getDefaultPrompts(script);
            }
            
        } catch (Exception e) {
            logger.error("调用千帆API时发生错误: {}", e.getMessage(), e);
            return getDefaultPrompts(script);
        }
    }
    
    /**
     * 构建提示词生成请求内容
     */
    private String buildPromptGenerationRequest(String script) {
        return String.format(
            "请根据以下分镜头脚本，生成相应的文生图提示词和图生视频提示词：\n\n" +
            "分镜头脚本：\n%s\n\n" +
            "要求：\n" +
            "1. 文生图提示词：用丰富生动的中文描述画面，详细描述场景环境、人物形象、光线效果、色彩搭配、构图风格、氛围营造等视觉元素，3-5句话，力求画面感强烈\n" +
            "2. 图生视频提示词：用流畅自然的中文描述镜头运动、画面变化、节奏感、视觉效果等动态元素，3-5句话，突出动感和电影感\n" +
            "3. 描述要具体形象，避免抽象概念，多用形容词和细节描述\n" +
            "4. 可以包含艺术风格、拍摄技法、视觉特效等专业元素\n" +
            "5. 语言要优美流畅，富有表现力\n" +
            "6. 请直接输出提示词内容，不要包含格式符号\n\n" +
            "请按照以下格式回答：\n" +
            "文生图提示词：[丰富详细的中文描述]\n" +
            "图生视频提示词：[生动具体的动态描述]",
            script
        );
    }
    
    /**
     * 解析千帆API返回结果
     */
    private Map<String, String> parseQianfanResponse(Map<String, Object> responseBody) {
        Map<String, String> result = new HashMap<>();
        
        try {
            // 解析千帆API返回的响应结构
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                String content = (String) message.get("content");
                
                // 解析AI生成的内容，提取文生图和图生视频提示词
                result = extractPromptsFromContent(content);
            }
            
            if (result.isEmpty()) {
                result.put("imagePrompt", "生成失败，请重试");
                result.put("videoPrompt", "生成失败，请重试");
            }
            
        } catch (Exception e) {
            logger.error("解析千帆API响应时发生错误: {}", e.getMessage(), e);
            result.put("imagePrompt", "解析失败，请重试");
            result.put("videoPrompt", "解析失败，请重试");
        }
        
        return result;
    }
    
    /**
     * 从AI生成的内容中提取提示词
     */
    private Map<String, String> extractPromptsFromContent(String content) {
        Map<String, String> result = new HashMap<>();
        
        try {
            // 使用正则表达式或字符串处理提取提示词
            String imagePrompt = "";
            String videoPrompt = "";
            
            // 查找文生图提示词
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.contains("文生图提示词")) {
                    imagePrompt = line.replaceAll(".*文生图提示词[：:]\\s*", "").trim();
                } else if (line.contains("图生视频提示词")) {
                    videoPrompt = line.replaceAll(".*图生视频提示词[：:]\\s*", "").trim();
                }
            }
            
            // 如果没有找到明确的分隔，尝试按段落分割
            if (imagePrompt.isEmpty() || videoPrompt.isEmpty()) {
                String[] paragraphs = content.split("\n\n");
                if (paragraphs.length >= 2) {
                    imagePrompt = paragraphs[0].replaceAll(".*[：:]\\s*", "").trim();
                    videoPrompt = paragraphs[1].replaceAll(".*[：:]\\s*", "").trim();
                }
            }
            
            // 清理中括号和其他格式符号
            imagePrompt = cleanPromptText(imagePrompt);
            videoPrompt = cleanPromptText(videoPrompt);
            
            // 自动在文生图提示词后面追加画幅规格
            if (!imagePrompt.isEmpty() && !imagePrompt.contains("画幅为16：9")) {
                String originalPrompt = imagePrompt;
                imagePrompt = imagePrompt + "，画幅为16：9";
                logger.info("自动追加画幅规格: {} -> {}", originalPrompt, imagePrompt);
            }
            
            result.put("imagePrompt", imagePrompt.isEmpty() ? "电影级画面质感，温暖柔和的自然光线洒在画面中央，高清细腻的视觉效果，色彩层次丰富，构图平衡优雅，画幅为16：9" : imagePrompt);
            result.put("videoPrompt", videoPrompt.isEmpty() ? "镜头以平稳流畅的节奏缓慢移动，光影变化自然过渡，画面层次感逐渐展现，营造沉浸式观影体验" : videoPrompt);
            
        } catch (Exception e) {
            logger.error("提取提示词时发生错误: {}", e.getMessage(), e);
            result.put("imagePrompt", "电影级画面质感，温暖柔和的自然光线洒在画面中央，高清细腻的视觉效果，色彩层次丰富，构图平衡优雅，画幅为16：9");
            result.put("videoPrompt", "镜头以平稳流畅的节奏缓慢移动，光影变化自然过渡，画面层次感逐渐展现，营造沉浸式观影体验");
        }
        
        return result;
    }
    
    /**
     * 清理提示词文本，移除中括号等格式符号
     */
    private String cleanPromptText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 移除中括号及其内容（如果是格式提示）
        text = text.replaceAll("\\[.*?\\]", "");
        
        // 移除开头和结尾的引号
        text = text.replaceAll("^[\"'']|[\"'']$", "");
        
        // 移除多余的空格
        text = text.replaceAll("\\s+", " ").trim();
        
        // 如果文本以常见的格式词开头，尝试移除
        text = text.replaceAll("^(提示词内容|prompt|content)[：:]?\\s*", "");
        
        return text.trim();
    }
    
    /**
     * 生成默认提示词（当API调用失败时使用）
     */
    private Map<String, String> getDefaultPrompts(String script) {
        Map<String, String> result = new HashMap<>();
        
        // 基于脚本关键词生成丰富的默认提示词
        String basicImagePrompt = "电影级画面质感，明亮清澈的自然光线，高清锐利的视觉细节，色彩饱和度适中，构图层次分明，整体氛围宁静和谐，画幅为16：9";
        String basicVideoPrompt = "镜头运动平稳流畅，焦点变化自然过渡，画面节奏舒缓有致，光影效果层次丰富，营造优雅的观影体验";
        
        if (script.contains("室内") || script.contains("房间")) {
            basicImagePrompt = "温馨舒适的室内环境，柔和温暖的灯光洒满空间，现代简约的装饰风格，色调以暖色系为主，细节纹理清晰可见，营造居家温馨氛围，画幅为16：9";
            basicVideoPrompt = "室内镜头缓慢而稳定地移动，光线在墙面和家具上形成柔美的投影，焦点在不同物体间平滑切换，展现空间的层次感和温馨感";
        } else if (script.contains("室外") || script.contains("街道") || script.contains("城市")) {
            basicImagePrompt = "繁华都市街景，自然日光与建筑光影交相辉映，现代建筑线条分明，色彩对比鲜明，远近景深层次丰富，充满城市生活气息，画幅为16：9";
            basicVideoPrompt = "户外镜头水平平移扫过城市景观，建筑物和街道在画面中流动变化，光线随着镜头移动产生动态效果，展现都市的繁华与活力";
        }
        
        result.put("imagePrompt", basicImagePrompt);
        result.put("videoPrompt", basicVideoPrompt);
        
        return result;
    }
    
    /**
     * 调用千帆API生成图像
     */
    public String generateImage(String prompt) {
        logger.info("开始调用千帆API生成图像，提示词: {}", prompt);
        
        try {
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "irag-1.0");
            requestBody.put("prompt", prompt);
            
            // 设置图片尺寸为16:9比例
            // 方案1: 分别设置宽高 (推荐1024x576)
            requestBody.put("width", imageWidth);
            requestBody.put("height", imageHeight);
            
            // 如果上述参数不生效，可尝试以下备选方案：
            // 方案2: 使用size参数
            // requestBody.put("size", "1024x576");
            // requestBody.put("aspect_ratio", "16:9");
            
            // 方案3: 使用image_size对象
            // Map<String, Integer> imageSize = new HashMap<>();
            // imageSize.put("width", 1024);
            // imageSize.put("height", 576);
            // requestBody.put("image_size", imageSize);
            
            try {
                logger.info("请求体: {}", objectMapper.writeValueAsString(requestBody));
            } catch (JsonProcessingException e) {
                logger.warn("无法序列化请求体为JSON: {}", e.getMessage());
            }
            logger.info("API Token: {}", apiToken);
            logger.info("请求URL: {}", QIANFAN_IMAGE_API_URL);
            logger.info("图片尺寸设置: {}x{} (16:9比例)", imageWidth, imageHeight);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            logger.info("请求头 Authorization: Bearer {}", apiToken);
            logger.info("请求头 Content-Type: {}", MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            logger.info("发送POST请求到千帆API...");
            ResponseEntity<Map> response = restTemplate.postForEntity(QIANFAN_IMAGE_API_URL, request, Map.class);
            
            logger.info("收到响应，状态码: {}", response.getStatusCode());
            try {
                logger.info("响应体: {}", objectMapper.writeValueAsString(response.getBody()));
            } catch (JsonProcessingException e) {
                logger.warn("无法序列化响应体为JSON: {}", e.getMessage());
                logger.info("响应体(toString): {}", response.getBody());
            }
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseImageGenerationResponse(response.getBody());
            } else {
                logger.error("千帆图像生成API调用失败，状态码: {}, 响应体: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("图像生成失败，API返回状态码: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("调用千帆图像生成API时发生错误: {}", e.getMessage(), e);
            if (e.getCause() != null) {
                logger.error("根本原因: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("图像生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析千帆图像生成API返回结果
     */
    private String parseImageGenerationResponse(Map<String, Object> responseBody) {
        logger.info("开始解析千帆API响应...");
        
        try {
            if (responseBody == null) {
                throw new RuntimeException("API响应体为空");
            }
            
            logger.info("响应体包含的键: {}", responseBody.keySet());
            
            // 检查是否包含错误信息
            if (responseBody.containsKey("error")) {
                Object error = responseBody.get("error");
                logger.error("API返回错误: {}", error);
                throw new RuntimeException("千帆API返回错误: " + error.toString());
            }
            
            // 解析千帆API返回的响应结构
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            if (data != null && !data.isEmpty()) {
                logger.info("找到data数组，包含 {} 个元素", data.size());
                
                Map<String, Object> firstImage = data.get(0);
                logger.info("第一个图像对象包含的键: {}", firstImage.keySet());
                
                String imageUrl = (String) firstImage.get("url");
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    logger.info("图像生成成功，URL: {}", imageUrl);
                    return imageUrl;
                } else {
                    logger.error("API返回的图像URL为空，第一个图像对象: {}", firstImage);
                    throw new RuntimeException("API返回的图像URL为空");
                }
            } else {
                logger.error("API返回的data数组为空或不存在，完整响应: {}", responseBody);
                throw new RuntimeException("API返回的数据为空");
            }
            
        } catch (Exception e) {
            logger.error("解析千帆图像生成API响应时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("解析图像生成结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据故事内容生成分镜头脚本
     */
    public List<Map<String, Object>> generateStoryboard(String storyContent) {
        logger.info("开始根据故事内容生成分镜头脚本");
        
        try {
            // 构建分镜头脚本生成请求
            String prompt = buildStoryboardGenerationRequest(storyContent);
            
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ernie-4.0-8k");
            
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            requestBody.put("messages", messages);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(QIANFAN_API_URL, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseStoryboardResponse(response.getBody());
            } else {
                logger.error("千帆API调用失败，状态码: {}", response.getStatusCode());
                return getDefaultStoryboard();
            }
            
        } catch (Exception e) {
            logger.error("调用千帆API生成分镜头脚本时发生错误: {}", e.getMessage(), e);
            return getDefaultStoryboard();
        }
    }
    
    /**
     * 构建分镜头脚本生成请求内容
     */
    private String buildStoryboardGenerationRequest(String storyContent) {
        return String.format(
            "请根据以下故事梗概，生成详细的分镜头脚本：\n\n" +
            "故事梗概：\n%s\n\n" +
            "要求：\n" +
            "1. 生成3-6个场景的分镜头脚本\n" +
            "2. 每个场景包含场景序号和详细的分镜头描述\n" +
            "3. 分镜头描述要具体生动，包含镜头运动、画面构成、环境氛围等细节\n" +
            "4. 适合用于视频制作，具有电影感和视觉冲击力\n" +
            "5. 场景之间要有逻辑连贯性，能够完整讲述故事\n" +
            "6. 每个场景的描述控制在50-100字左右\n\n" +
            "请严格按照以下格式输出，每个场景占一行：\n" +
            "场景1：[详细的分镜头描述]\n" +
            "场景2：[详细的分镜头描述]\n" +
            "场景3：[详细的分镜头描述]\n" +
            "...\n\n" +
            "注意：只输出场景内容，不要包含其他说明文字。",
            storyContent
        );
    }
    
    /**
     * 解析千帆API返回的分镜头脚本结果
     */
    private List<Map<String, Object>> parseStoryboardResponse(Map<String, Object> responseBody) {
        List<Map<String, Object>> storyboards = new ArrayList<>();
        
        try {
            // 解析千帆API返回的响应结构
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                String content = (String) message.get("content");
                
                // 解析生成的分镜头脚本内容
                storyboards = parseStoryboardContent(content);
            }
            
            if (storyboards.isEmpty()) {
                logger.warn("AI生成的分镜头脚本为空，使用默认脚本");
                storyboards = getDefaultStoryboard();
            }
            
        } catch (Exception e) {
            logger.error("解析千帆API分镜头脚本响应时发生错误: {}", e.getMessage(), e);
            storyboards = getDefaultStoryboard();
        }
        
        return storyboards;
    }
    
    /**
     * 从AI生成的内容中解析分镜头脚本
     */
    private List<Map<String, Object>> parseStoryboardContent(String content) {
        List<Map<String, Object>> storyboards = new ArrayList<>();
        
        try {
            String[] lines = content.split("\n");
            int sceneNumber = 1;
            
            for (String line : lines) {
                line = line.trim();
                
                // 匹配场景格式：场景X：内容
                if (line.matches("^场景\\d+[：:].*")) {
                    String script = line.replaceAll("^场景\\d+[：:]\\s*", "").trim();
                    
                    if (!script.isEmpty()) {
                        Map<String, Object> storyboard = new HashMap<>();
                        storyboard.put("scene", sceneNumber);
                        storyboard.put("script", script);
                        storyboard.put("imagePrompt", "请输入文生图提示词...");
                        storyboard.put("videoPrompt", "请输入图生视频提示词...");
                        
                        storyboards.add(storyboard);
                        sceneNumber++;
                    }
                }
            }
            
            // 如果没有解析到标准格式，尝试按行解析
            if (storyboards.isEmpty() && lines.length > 0) {
                sceneNumber = 1;
                for (String line : lines) {
                    line = line.trim();
                    
                    // 跳过空行和无意义的行
                    if (!line.isEmpty() && !line.startsWith("请") && !line.startsWith("注意") && 
                        line.length() > 10) {
                        
                        // 清理可能的序号或格式字符
                        String script = line.replaceAll("^\\d+[.、：:]\\s*", "")
                                           .replaceAll("^[场景]+\\d*[：:]\\s*", "")
                                           .trim();
                        
                        if (!script.isEmpty()) {
                            Map<String, Object> storyboard = new HashMap<>();
                            storyboard.put("scene", sceneNumber);
                            storyboard.put("script", script);
                            storyboard.put("imagePrompt", "请输入文生图提示词...");
                            storyboard.put("videoPrompt", "请输入图生视频提示词...");
                            
                            storyboards.add(storyboard);
                            sceneNumber++;
                            
                            // 限制最多6个场景
                            if (sceneNumber > 6) break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("解析分镜头脚本内容时发生错误: {}", e.getMessage(), e);
        }
        
        return storyboards;
    }
    
    /**
     * 生成默认分镜头脚本（当AI生成失败时使用）
     */
    private List<Map<String, Object>> getDefaultStoryboard() {
        List<Map<String, Object>> defaultStoryboards = new ArrayList<>();
        
        // 默认场景1
        Map<String, Object> scene1 = new HashMap<>();
        scene1.put("scene", 1);
        scene1.put("script", "开场：建立故事背景和主要环境。镜头从全景开始，展现故事发生的场所，营造氛围感。");
        scene1.put("imagePrompt", "请输入文生图提示词...");
        scene1.put("videoPrompt", "请输入图生视频提示词...");
        defaultStoryboards.add(scene1);
        
        // 默认场景2
        Map<String, Object> scene2 = new HashMap<>();
        scene2.put("scene", 2);
        scene2.put("script", "人物登场：主角出现，通过特写或中景镜头展现人物特征和状态，推进故事情节。");
        scene2.put("imagePrompt", "请输入文生图提示词...");
        scene2.put("videoPrompt", "请输入图生视频提示词...");
        defaultStoryboards.add(scene2);
        
        // 默认场景3
        Map<String, Object> scene3 = new HashMap<>();
        scene3.put("scene", 3);
        scene3.put("script", "结尾：故事达到高潮或结论。镜头回到远景，展现故事的完整性和深度。");
        scene3.put("imagePrompt", "请输入文生图提示词...");
        scene3.put("videoPrompt", "请输入图生视频提示词...");
        defaultStoryboards.add(scene3);
        
        return defaultStoryboards;
    }

    /**
     * 根据故事标题生成初始故事梗概
     */
    public String generateStoryFromTitle(String title) {
        logger.info("开始根据故事标题生成故事梗概: {}", title);
        
        try {
            String prompt = buildStoryGenerationRequest(title);
            
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ernie-3.5-8k");
            requestBody.put("stream", false);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(QIANFAN_API_URL, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseSimpleTextResponse(response.getBody());
            } else {
                logger.error("千帆API调用失败，状态码: {}", response.getStatusCode());
                return getDefaultStoryContent(title);
            }
            
        } catch (Exception e) {
            logger.error("调用千帆API生成故事时发生错误: {}", e.getMessage(), e);
            return getDefaultStoryContent(title);
        }
    }
    
    /**
     * 根据修改要求修改故事梗概
     */
    public String modifyStory(String currentStory, String instruction) {
        logger.info("开始修改故事梗概，修改要求: {}", instruction);
        
        try {
            String prompt = buildStoryModificationRequest(currentStory, instruction);
            
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ernie-3.5-8k");
            requestBody.put("stream", false);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(QIANFAN_API_URL, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseSimpleTextResponse(response.getBody());
            } else {
                logger.error("千帆API调用失败，状态码: {}", response.getStatusCode());
                return currentStory + " (已根据要求进行调整: " + instruction + ")";
            }
            
        } catch (Exception e) {
            logger.error("调用千帆API修改故事时发生错误: {}", e.getMessage(), e);
            return currentStory + " (已根据要求进行调整: " + instruction + ")";
        }
    }
    
    /**
     * 构建故事生成请求内容
     */
    private String buildStoryGenerationRequest(String title) {
        return String.format(
            "请根据以下故事标题，生成一个简洁而完整的故事梗概（约200字）：\n" +
            "故事标题：%s\n\n" +
            "要求：\n" +
            "- 故事长度适中（约200字）\n" +
            "- 情节紧凑，有明确的起承转合\n" +
            "- 内容要有画面感，适合视觉呈现\n" +
            "- 语言生动，富有感染力\n" +
            "- 直接输出故事梗概，不需要其他说明\n",
            title
        );
    }
    
    /**
     * 构建故事修改请求内容
     */
    private String buildStoryModificationRequest(String currentStory, String instruction) {
        return String.format(
            "以下是之前生成的故事梗概：\n%s\n\n" +
            "现在根据这个修改要求：%s\n" +
            "请帮我修改成新的故事梗概（约200字）。\n\n" +
            "要求：\n" +
            "- 保持故事的连贯性和完整性\n" +
            "- 根据修改要求调整相应内容\n" +
            "- 语言要生动流畅\n" +
            "- 直接输出修改后的完整故事梗概\n",
            currentStory, instruction
        );
    }
    
    /**
     * 解析简单文本响应
     */
    private String parseSimpleTextResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                String content = (String) message.get("content");
                return content != null ? content.trim() : "";
            }
        } catch (Exception e) {
            logger.error("解析千帆API简单文本响应时发生错误: {}", e.getMessage(), e);
        }
        return "";
    }
    
    /**
     * 获取默认故事内容
     */
    private String getDefaultStoryContent(String title) {
        return String.format("这是一个关于《%s》的精彩故事。故事讲述了一个充满想象力的冒险，主人公经历了重重挑战，最终获得了成长和收获。整个故事情节跌宕起伏，富有感染力，非常适合改编为视频作品。", title);
    }
} 