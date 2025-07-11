package com.example.springboot_pro.dto;

import jakarta.validation.constraints.NotBlank;

public class UserLoginDto {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    // 构造函数
    public UserLoginDto() {}
    
    public UserLoginDto(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getter 和 Setter 方法
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String toString() {
        return "UserLoginDto{" +
                "username='" + username + '\'' +
                '}';
    }
} 