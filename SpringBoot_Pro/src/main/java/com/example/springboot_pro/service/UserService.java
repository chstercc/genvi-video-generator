package com.example.springboot_pro.service;

import com.example.springboot_pro.domain.User;
import com.example.springboot_pro.dto.UserRegistrationDto;
import com.example.springboot_pro.dto.UserLoginDto;
import com.example.springboot_pro.dto.AuthResponse;

public interface UserService {
    
    /**
     * 用户注册
     * @param registrationDto 注册信息
     * @return 认证响应
     */
    AuthResponse register(UserRegistrationDto registrationDto);
    
    /**
     * 用户登录
     * @param loginDto 登录信息
     * @return 认证响应
     */
    AuthResponse login(UserLoginDto loginDto);
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户对象
     */
    User findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);
} 