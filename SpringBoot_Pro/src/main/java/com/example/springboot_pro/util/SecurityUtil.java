package com.example.springboot_pro.util;

import com.example.springboot_pro.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    
    /**
     * 获取当前登录用户
     * @return 当前用户，如果未登录则返回null
     */
    public static User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    return (User) principal;
                }
            }
        } catch (Exception e) {
            // 如果获取失败，返回null
        }
        return null;
    }
    
    /**
     * 获取当前登录用户的ID
     * @return 当前用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 获取当前登录用户的用户名
     * @return 当前用户名，如果未登录则返回null
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // 如果获取失败，返回null
        }
        return null;
    }
    
    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated() && 
                   !"anonymousUser".equals(authentication.getName());
        } catch (Exception e) {
            return false;
        }
    }
} 