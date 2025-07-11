package com.example.springboot_pro.service;

import com.example.springboot_pro.dao.UserRepository;
import com.example.springboot_pro.domain.User;
import com.example.springboot_pro.dto.AuthResponse;
import com.example.springboot_pro.dto.UserLoginDto;
import com.example.springboot_pro.dto.UserRegistrationDto;
import com.example.springboot_pro.util.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    
    @Resource
    private UserRepository userRepository;
    
    @Resource
    @Lazy
    private PasswordEncoder passwordEncoder;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Autowired(required = false)
    @Lazy
    private AuthenticationManager authenticationManager;
    
    @Override
    public AuthResponse register(UserRegistrationDto registrationDto) {
        try {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(registrationDto.getUsername())) {
                return new AuthResponse("用户名已存在");
            }
            
            // 检查邮箱是否已存在
            if (userRepository.existsByEmail(registrationDto.getEmail())) {
                return new AuthResponse("邮箱已存在");
            }
            
            // 创建新用户
            User user = new User();
            user.setUsername(registrationDto.getUsername());
            user.setEmail(registrationDto.getEmail());
            user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
            user.setRole("USER");
            
            // 保存用户
            User savedUser = userRepository.save(user);
            
            // 生成JWT token
            String token = jwtUtil.generateToken(savedUser);
            
            return new AuthResponse(token, savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
            
        } catch (Exception e) {
            return new AuthResponse("注册失败：" + e.getMessage());
        }
    }
    
    @Override
    public AuthResponse login(UserLoginDto loginDto) {
        try {
            // 验证用户凭证
            if (authenticationManager != null) {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDto.getUsername(),
                                loginDto.getPassword()
                        )
                );
                
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = (User) userDetails;
                
                // 生成JWT token
                String token = jwtUtil.generateToken(userDetails);
                
                return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
            } else {
                // 如果AuthenticationManager未配置，手动验证
                Optional<User> userOptional = userRepository.findByUsername(loginDto.getUsername());
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    if (passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
                        String token = jwtUtil.generateToken(user);
                        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
                    }
                }
                return new AuthResponse("用户名或密码错误");
            }
            
        } catch (AuthenticationException e) {
            return new AuthResponse("用户名或密码错误");
        } catch (Exception e) {
            return new AuthResponse("登录失败：" + e.getMessage());
        }
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // UserDetailsService 接口实现
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            throw new UsernameNotFoundException("用户未找到: " + username);
        }
    }
} 