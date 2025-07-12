package com.example.springboot_pro.service;

import com.example.springboot_pro.dao.StoryboardRepository;
import com.example.springboot_pro.domain.Storyboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class StoryboardServiceImpl implements StoryboardService {
    
    @Autowired
    private StoryboardRepository storyboardRepository;
    
    @Autowired
    private QianfanService qianfanService;
    
    @Autowired
    private SimpleImageService simpleImageService;
    
    @Override
    public List<Storyboard> getStoryboardsByStoryId(Long storyId) {
        return storyboardRepository.findByStoryIdOrderBySceneAsc(storyId);
    }
    
    @Override
    public List<Storyboard> generateStoryboard(Long storyId) {
        // 删除现有的分镜头脚本
        storyboardRepository.deleteByStoryId(storyId);
        
        // 生成新的分镜头脚本
        List<Storyboard> storyboards = createSampleStoryboards(storyId);
        
        // 保存到数据库
        return storyboardRepository.saveAll(storyboards);
    }
    
    @Override
    public Storyboard saveStoryboard(Storyboard storyboard) {
        return storyboardRepository.save(storyboard);
    }
    
    @Override
    public Storyboard updateStoryboard(Long id, Storyboard updatedStoryboard) {
        return storyboardRepository.findById(id)
                .map(storyboard -> {
                    storyboard.setScript(updatedStoryboard.getScript());
                    storyboard.setImagePrompt(updatedStoryboard.getImagePrompt());
                    storyboard.setVideoPrompt(updatedStoryboard.getVideoPrompt());
                    storyboard.setConceptImage(updatedStoryboard.getConceptImage());
                    // 更新视频相关字段（如果提供的话）
                    if (updatedStoryboard.getGeneratedVideo() != null) {
                        storyboard.setGeneratedVideo(updatedStoryboard.getGeneratedVideo());
                    }
                    if (updatedStoryboard.getVideoStatus() != null) {
                        storyboard.setVideoStatus(updatedStoryboard.getVideoStatus());
                    }
                    if (updatedStoryboard.getVideoGeneratedAt() != null) {
                        storyboard.setVideoGeneratedAt(updatedStoryboard.getVideoGeneratedAt());
                    }
                    // 更新音效相关字段（如果提供的话）
                    if (updatedStoryboard.getAudioVideo() != null) {
                        storyboard.setAudioVideo(updatedStoryboard.getAudioVideo());
                    }
                    if (updatedStoryboard.getAudioStatus() != null) {
                        storyboard.setAudioStatus(updatedStoryboard.getAudioStatus());
                    }
                    if (updatedStoryboard.getAudioGeneratedAt() != null) {
                        storyboard.setAudioGeneratedAt(updatedStoryboard.getAudioGeneratedAt());
                    }
                    return storyboardRepository.save(storyboard);
                }).orElseGet(() -> {
                    updatedStoryboard.setId(id);
                    return storyboardRepository.save(updatedStoryboard);
                });
    }
    
    @Override
    public void deleteStoryboard(Long id) {
        storyboardRepository.deleteById(id);
    }
    
    @Override
    public Storyboard updateVideoInfo(Long id, String videoUrl, String status) {
        return storyboardRepository.findById(id)
                .map(storyboard -> {
                    storyboard.setGeneratedVideo(videoUrl);
                    storyboard.setVideoStatus(status);
                    if ("completed".equals(status)) {
                        storyboard.setVideoGeneratedAt(java.time.LocalDateTime.now());
                    }
                    return storyboardRepository.save(storyboard);
                }).orElseThrow(() -> new RuntimeException("Storyboard not found with id: " + id));
    }
    
    @Override
    public Storyboard updateAudioInfo(Long id, String audioVideoUrl, String status) {
        return storyboardRepository.findById(id)
                .map(storyboard -> {
                    storyboard.setAudioVideo(audioVideoUrl);
                    storyboard.setAudioStatus(status);
                    if ("completed".equals(status)) {
                        storyboard.setAudioGeneratedAt(java.time.LocalDateTime.now());
                    }
                    return storyboardRepository.save(storyboard);
                }).orElseThrow(() -> new RuntimeException("Storyboard not found with id: " + id));
    }
    
    @Override
    public Storyboard generateAIPrompts(Long storyboardId) {
        return storyboardRepository.findById(storyboardId)
                .map(storyboard -> {
                    if (storyboard.getScript() == null || storyboard.getScript().trim().isEmpty()) {
                        throw new RuntimeException("分镜头脚本不能为空，请先编写脚本内容");
                    }
                    
                    // 调用千帆API生成提示词
                    java.util.Map<String, String> prompts = qianfanService.generatePrompts(storyboard.getScript());
                    
                    // 更新分镜头脚本的提示词
                    storyboard.setImagePrompt(prompts.get("imagePrompt"));
                    storyboard.setVideoPrompt(prompts.get("videoPrompt"));
                    
                    return storyboardRepository.save(storyboard);
                }).orElseThrow(() -> new RuntimeException("Storyboard not found with id: " + storyboardId));
    }
    
    @Override
    public Storyboard generateConceptImage(Long storyboardId) {
        return storyboardRepository.findById(storyboardId)
                .map(storyboard -> {
                    if (storyboard.getImagePrompt() == null || storyboard.getImagePrompt().trim().isEmpty()) {
                        throw new RuntimeException("文生图提示词不能为空，请先生成或编辑提示词");
                    }
                    
                    // 调用千帆API生成图像（获取网络URL）
                    String networkImageUrl = qianfanService.generateImage(storyboard.getImagePrompt());
                    
                    // 下载图片到本地并获取简单的访问URL
                    String localImageUrl = simpleImageService.downloadAndSaveImage(
                        networkImageUrl, 
                        storyboard.getStoryId(), 
                        storyboard.getScene()
                    );
                    
                    // 同时存储本地URL和网络URL
                    storyboard.setConceptImage(localImageUrl);      // 本地URL，用于前端显示
                    storyboard.setNetworkImageUrl(networkImageUrl); // 网络URL，用于外部API调用
                    
                    return storyboardRepository.save(storyboard);
                }).orElseThrow(() -> new RuntimeException("Storyboard not found with id: " + storyboardId));
    }
    
    @Override
    public List<Storyboard> reorderStoryboards(Long storyId) {
        // 获取该故事的所有分镜头脚本，按场景编号排序
        List<Storyboard> storyboards = storyboardRepository.findByStoryIdOrderBySceneAsc(storyId);
        
        // 重新分配场景编号（从1开始连续编号）
        for (int i = 0; i < storyboards.size(); i++) {
            storyboards.get(i).setScene(i + 1);
        }
        
        // 保存更新后的数据
        return storyboardRepository.saveAll(storyboards);
    }
    
    /**
     * 创建示例分镜头脚本数据
     */
    private List<Storyboard> createSampleStoryboards(Long storyId) {
        List<Storyboard> storyboards = new ArrayList<>();
        
        Storyboard scene1 = new Storyboard(storyId, 1, 
            "开场：主人公走在夜晚的街道上，路灯昏暗，营造神秘氛围。镜头从远景慢慢推近，展现主人公的背影。",
            "深邃的夜晚城市街道，昏黄温暖的路灯光线在湿润的柏油路面上形成斑驳光影，远处一个神秘身影缓缓前行，剪影轮廓清晰。整体色调以深蓝和暖黄为主，营造出电影noir风格的神秘氛围，细节层次丰富，画幅为16：9",
            "镜头从宽阔的远景开始，以缓慢而稳定的节奏向前推进，逐渐从远景过渡到中景。人物步伐节奏感明显，每一步都在路灯光圈中留下短暂的身影，光影在画面中动态变化，营造出悬疑电影的紧张感和节奏感。");
        storyboards.add(scene1);
        
        Storyboard scene2 = new Storyboard(storyId, 2, 
            "转身特写：主人公突然停下脚步，缓缓转身，露出坚定的表情。镜头切换到正面特写，强调眼神的决心。",
            "人物正面特写镜头，坚毅深邃的眼神占据画面中心，戏剧性的侧面补光在脸部形成明暗分明的光影效果，突出面部轮廓和表情细节。背景采用浅景深虚化处理，色彩以冷暖对比为主，整体质感电影化，画幅为16：9",
            "镜头以流畅的切换方式从背影转向正面特写，焦点跟随人物转身动作进行拉焦变化，从背景虚化逐渐过渡到面部清晰。光线在转身过程中产生动态变化，强调眼神中的坚定与决心，营造戏剧张力。");
        storyboards.add(scene2);
        
        Storyboard scene3 = new Storyboard(storyId, 3, 
            "环境远景：镜头拉远，展现整个城市的夜景，高楼林立，霓虹闪烁，主人公在画面中显得渺小但坚定。",
            "壮观的城市夜景全景，摩天大楼林立如森，五彩斑斓的霓虹灯光在建筑外墙上闪烁变幻，街道纵横交错形成几何图案。画面中央的渺小身影与宏大的城市形成强烈对比，色彩层次丰富，光影效果震撼，展现现代都市的繁华与个体的渺小，画幅为16：9",
            "镜头以宏伟的航拍视角缓慢向后拉远，逐渐展现城市的全貌和规模。霓虹灯光在夜空中闪烁变化，交通灯流动形成光轨，建筑群在画面中呈现出层次递进的视觉效果，营造出史诗般的都市夜景场面。");
        storyboards.add(scene3);
        
        return storyboards;
    }
} 