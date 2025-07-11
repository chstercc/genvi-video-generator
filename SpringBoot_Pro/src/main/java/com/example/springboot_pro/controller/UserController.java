package com.example.springboot_pro.controller;

import com.example.springboot_pro.dto.AuthResponse;
import com.example.springboot_pro.dto.UserLoginDto;
import com.example.springboot_pro.dto.UserRegistrationDto;
import com.example.springboot_pro.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    
    @Resource
    private UserService userService;
    
    /**
     * 用户注册
     * @param registrationDto 注册信息
     * @return 认证响应
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        AuthResponse response = userService.register(registrationDto);
        
        // 根据响应内容判断是否成功
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登录
     * @param loginDto 登录信息
     * @return 认证响应
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody UserLoginDto loginDto) {
        AuthResponse response = userService.login(loginDto);
        
        // 根据响应内容判断是否成功
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }
    
    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    @GetMapping("/check-email/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }
} 