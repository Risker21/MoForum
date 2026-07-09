package com.moforum.controller;

import com.moforum.config.JwtUtils;
import com.moforum.config.UserPrincipal;
import com.moforum.entity.User;
import com.moforum.mapper.UserMapper;
import com.moforum.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        String bio = body.getOrDefault("bio", "");
        String avatarUrl = body.getOrDefault("avatarUrl", "");
        if (bio.length() > 256) {
            return Map.of("success", false, "message", "签名不能超过 256 字");
        }
        userMapper.updateProfile(principal.userId(), bio, avatarUrl);
        return Map.of("success", true, "message", "已更新");
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return userService.loginResult(body.get("username"), body.get("password"));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtils.parseToken(token);
                String jti = claims.getId();
                if (jti != null) {
                    long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
                    if (ttl > 0) {
                        stringRedisTemplate.opsForValue().set("jwt:blacklist:" + jti, "1", Duration.ofMillis(ttl));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        SecurityContextHolder.clearContext();
        return Map.of("success", true, "message", "已退出登录");
    }

    @GetMapping("/getById")
    public User getUserById(@RequestParam Long id) {
        User u = userService.getUserById(id);
        if (u != null) {
            u.setPassword(null);
        }
        return u;
    }

    @GetMapping("/getByUserNo")
    public ResponseEntity<User> getByUserNo(@RequestParam Long userNo) {
        User u = userService.getUserByUserNo(userNo);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        u.setPassword(null);
        return ResponseEntity.ok(u);
    }
}
