package com.example.springboot_pro.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VolcEngineVideoService {
    
    private static final Logger logger = LoggerFactory.getLogger(VolcEngineVideoService.class);
    
    private static final BitSet URLENCODER = new BitSet(256);
    private static final String CONST_ENCODE = "0123456789ABCDEF";
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    
    // 配置信息
    @Value("${volcengine.access-key-id:}")
    private String accessKeyId;
    
    @Value("${volcengine.secret-access-key:}")
    private String secretAccessKey;
    
    @Value("${volcengine.endpoint:visual.volcengineapi.com}")
    private String endpoint;
    
    @Value("${volcengine.region:cn-north-1}")
    private String region;
    
    @Value("${volcengine.service:cv}")
    private String service;
    
    @Value("${volcengine.schema:https}")
    private String schema;
    
    private static final String REQ_KEY = "jimeng_vgfm_i2v_l20";
    private static final String SUBMIT_ACTION = "CVSync2AsyncSubmitTask";
    private static final String QUERY_ACTION = "CVSync2AsyncGetResult";
    private static final String VERSION = "2022-08-31";
    
    // 本地视频存储目录
    private static final String LOCAL_VIDEO_DIR = "D:/SpringBoot_Pro/videos";
    private static final String VIDEO_URL_PREFIX = "http://localhost:8080/api/video/files/";
    
    static {
        int i;
        for (i = 97; i <= 122; ++i) {
            URLENCODER.set(i);
        }
        for (i = 65; i <= 90; ++i) {
            URLENCODER.set(i);
        }
        for (i = 48; i <= 57; ++i) {
            URLENCODER.set(i);
        }
        URLENCODER.set('-');
        URLENCODER.set('_');
        URLENCODER.set('.');
        URLENCODER.set('~');
    }
    
    /**
     * 提交图生视频任务
     */
    public VideoTaskResponse submitVideoTask(String imageUrl, String prompt, String aspectRatio) {
        try {
            validateConfig();
            
            // 构建请求参数
            JSONObject req = new JSONObject();
            req.put("req_key", REQ_KEY);
            
            ArrayList<String> imageUrls = new ArrayList<>();
            imageUrls.add(imageUrl);
            req.put("image_urls", imageUrls);
            
            if (prompt != null && !prompt.trim().isEmpty()) {
                req.put("prompt", prompt.trim());
            }
            
            if (aspectRatio != null && !aspectRatio.trim().isEmpty()) {
                req.put("aspect_ratio", aspectRatio);
            }
            
            // 调用API
            String response = doRequest("POST", new HashMap<>(), req.toJSONString().getBytes(), 
                                      new Date(), SUBMIT_ACTION, VERSION);
            
            logger.info("山火API提交任务响应: {}", response);
            
            // 解析响应
            JSONObject responseObj = JSONObject.parseObject(response);
            VideoTaskResponse result = new VideoTaskResponse();
            
            if (responseObj.getInteger("code") == 10000) {
                JSONObject data = responseObj.getJSONObject("data");
                result.setSuccess(true);
                result.setTaskId(data.getString("task_id"));
                result.setMessage("任务提交成功");
            } else {
                result.setSuccess(false);
                result.setMessage(responseObj.getString("message"));
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("提交视频生成任务失败", e);
            VideoTaskResponse result = new VideoTaskResponse();
            result.setSuccess(false);
            result.setMessage("提交任务失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 查询视频生成任务结果
     */
    public VideoTaskResponse queryVideoTask(String taskId) {
        try {
            validateConfig();
            
            // 构建请求参数
            JSONObject req = new JSONObject();
            req.put("req_key", REQ_KEY);
            req.put("task_id", taskId);
            
            // 调用API
            String response = doRequest("POST", new HashMap<>(), req.toJSONString().getBytes(), 
                                      new Date(), QUERY_ACTION, VERSION);
            
            logger.info("山火API查询任务响应: {}", response);
            
            // 解析响应
            JSONObject responseObj = JSONObject.parseObject(response);
            VideoTaskResponse result = new VideoTaskResponse();
            
            if (responseObj.getInteger("code") == 10000) {
                JSONObject data = responseObj.getJSONObject("data");
                result.setSuccess(true);
                result.setTaskId(taskId);
                
                String status = data.getString("status");
                result.setStatus(status);
                
                if ("done".equals(status)) {
                    result.setVideoUrl(data.getString("video_url"));
                    result.setMessage("视频生成完成");
                } else {
                    result.setMessage("视频生成中，请稍后查询");
                }
            } else {
                result.setSuccess(false);
                result.setMessage(responseObj.getString("message"));
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("查询视频生成任务失败", e);
            VideoTaskResponse result = new VideoTaskResponse();
            result.setSuccess(false);
            result.setMessage("查询任务失败: " + e.getMessage());
            return result;
        }
    }
    
    private void validateConfig() throws Exception {
        if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
            throw new Exception("山火引擎AccessKeyID未配置");
        }
        if (secretAccessKey == null || secretAccessKey.trim().isEmpty()) {
            throw new Exception("山火引擎SecretAccessKey未配置");
        }
    }
    
    private String doRequest(String method, Map<String, String> queryList, byte[] body,
                           Date date, String action, String version) throws Exception {
        if (body == null) {
            body = new byte[0];
        }
        
        String path = "/";
        String xContentSha256 = hashSHA256(body);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String xDate = sdf.format(date);
        String shortXDate = xDate.substring(0, 8);
        String contentType = "application/json";
        String signHeader = "host;x-date;x-content-sha256;content-type";

        SortedMap<String, String> realQueryList = new TreeMap<>(queryList);
        realQueryList.put("Action", action);
        realQueryList.put("Version", version);
        StringBuilder querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key)).append("=").append(signStringEncoder(realQueryList.get(key))).append("&");
        }
        querySB.deleteCharAt(querySB.length() - 1);

        String canonicalStringBuilder = method + "\n" + path + "\n" + querySB + "\n" +
                "host:" + endpoint + "\n" +
                "x-date:" + xDate + "\n" +
                "x-content-sha256:" + xContentSha256 + "\n" +
                "content-type:" + contentType + "\n" +
                "\n" +
                signHeader + "\n" +
                xContentSha256;

        String hashcanonicalString = hashSHA256(canonicalStringBuilder.getBytes());
        String credentialScope = shortXDate + "/" + region + "/" + service + "/request";
        String signString = "HMAC-SHA256" + "\n" + xDate + "\n" + credentialScope + "\n" + hashcanonicalString;

        byte[] signKey = genSigningSecretKeyV4(secretAccessKey, shortXDate, region, service);
        String signature = Hex.encodeHexString(hmacSHA256(signKey, signString));

        URL url = new URL(schema + "://" + endpoint + path + "?" + querySB);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Host", endpoint);
        conn.setRequestProperty("X-Date", xDate);
        conn.setRequestProperty("X-Content-Sha256", xContentSha256);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", "HMAC-SHA256" +
                " Credential=" + accessKeyId + "/" + credentialScope +
                ", SignedHeaders=" + signHeader +
                ", Signature=" + signature);
        
        if (!Objects.equals(conn.getRequestMethod(), "GET")) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body);
            os.flush();
            os.close();
        }
        conn.connect();

        int responseCode = conn.getResponseCode();

        InputStream is;
        if (responseCode == 200) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }
        
        byte[] responseBytes = is.readAllBytes();
        String responseBody = new String(responseBytes);
        is.close();

        if (responseCode != 200) {
            throw new Exception("HTTP请求失败，状态码: " + responseCode + ", 响应: " + responseBody);
        }

        return responseBody;
    }
    
    private String signStringEncoder(String source) {
        if (source == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder(source.length());
        ByteBuffer bb = UTF_8.encode(source);
        while (bb.hasRemaining()) {
            int b = bb.get() & 255;
            if (URLENCODER.get(b)) {
                buf.append((char) b);
            } else if (b == 32) {
                buf.append("%20");
            } else {
                buf.append("%");
                char hex1 = CONST_ENCODE.charAt(b >> 4);
                char hex2 = CONST_ENCODE.charAt(b & 15);
                buf.append(hex1);
                buf.append(hex2);
            }
        }
        return buf.toString();
    }

    public static String hashSHA256(byte[] content) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Hex.encodeHexString(md.digest(content));
        } catch (Exception e) {
            throw new Exception("Unable to compute hash while signing request: " + e.getMessage(), e);
        }
    }

    public static byte[] hmacSHA256(byte[] key, String content) throws Exception {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(content.getBytes());
        } catch (Exception e) {
            throw new Exception("Unable to calculate a request signature: " + e.getMessage(), e);
        }
    }

    private byte[] genSigningSecretKeyV4(String secretKey, String date, String region, String service) throws Exception {
        byte[] kDate = hmacSHA256((secretKey).getBytes(), date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "request");
    }
    
    /**
     * 下载视频到本地并返回本地URL
     */
    public String downloadVideoToLocal(String videoUrl, String taskId) {
        try {
            // 确保目录存在
            Path videoDir = Paths.get(LOCAL_VIDEO_DIR);
            Files.createDirectories(videoDir);
            
            // 生成本地文件名
            String fileName = "video_" + taskId + "_" + System.currentTimeMillis() + ".mp4";
            Path localFilePath = videoDir.resolve(fileName);
            
            logger.info("开始下载视频: {} -> {}", videoUrl, localFilePath);
            
            // 下载视频文件
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(300000);   // 5分钟读取超时
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(localFilePath.toFile())) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    
                    logger.info("视频下载完成: {} bytes -> {}", totalBytes, localFilePath);
                }
                
                // 返回本地访问URL
                return VIDEO_URL_PREFIX + fileName;
                
            } else {
                throw new Exception("下载视频失败，HTTP状态码: " + responseCode);
            }
            
        } catch (Exception e) {
            logger.error("下载视频到本地失败: " + videoUrl, e);
            // 下载失败时返回原始URL，作为降级方案
            return videoUrl;
        }
    }
    
    /**
     * 视频任务响应类
     */
    public static class VideoTaskResponse {
        private boolean success;
        private String taskId;
        private String status;
        private String videoUrl;
        private String message;
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getVideoUrl() {
            return videoUrl;
        }
        
        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
} 