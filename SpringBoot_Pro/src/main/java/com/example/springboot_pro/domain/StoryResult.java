package com.example.springboot_pro.domain;

public class StoryResult {
    private String title;
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public StoryResult() {}

    public StoryResult(String title, String content) {
        this.title = title;
        this.content = content;
    }


}
