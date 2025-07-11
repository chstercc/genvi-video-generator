package com.example.springboot_pro.ai_agent;

import org.springframework.stereotype.Component;

@Component
public class StoryAgent {
    
    private static final String STORY_TEMPLATE = """
        请根据以下标题，生成一个引人入胜的故事梗概。故事应该包含：
        1. 清晰的故事背景和设定
        2. 鲜明的人物性格
        3. 引人入胜的情节发展
        4. 合理的故事结构
        5. 令人印象深刻的结局
        
        标题：{title}
        
        请生成故事梗概：
        """;
    
    private static final String MODIFY_TEMPLATE = """
        请根据以下修改要求，对故事进行修改。修改时请注意：
        1. 保持故事的连贯性和完整性
        2. 确保修改后的内容符合原有的故事风格
        3. 保留原有故事中的精彩元素
        4. 根据修改要求进行恰当的调整
        
        原故事：
        {summary}
        
        修改要求：
        {instruction}
        
        请生成修改后的故事：
        """;
    
    public String generateInitialStory(String title) {
        // 这里应该调用LangChain或其他AI服务来生成故事
        // 目前使用模拟数据
        String prompt = STORY_TEMPLATE.replace("{title}", title);
        String aiResponse = simulateAIResponse(prompt);
        return simulateAIResponse(prompt);
    }
    
    public String modifyStory(String summary, String instruction) {
        // 这里应该调用LangChain或其他AI服务来修改故事
        // 目前使用模拟数据
        String prompt = MODIFY_TEMPLATE
            .replace("{summary}", summary)
            .replace("{instruction}", instruction);
        return simulateAIResponse(prompt);
    }
    
    private String simulateAIResponse(String prompt) {
        // 模拟AI响应
        // 在实际实现中，这里应该调用真实的AI服务
        return "这是一个示例故事响应。在实际实现中，这里将返回AI生成的内容。\n" +
               "故事将包含完整的背景、人物、情节和结局。";
    }
} 