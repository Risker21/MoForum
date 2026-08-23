package com.moforum.service;

import com.moforum.config.JwtUtils;
import com.moforum.entity.User;
import com.moforum.mapper.FollowMapper;
import com.moforum.mapper.FriendMapper;
import com.moforum.mapper.FriendRequestMapper;
import com.moforum.mapper.MessageMapper;
import com.moforum.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 作者:Momo同学
 * 日期: 2026/4/12 22:02
 */
@Service
public class UserService {

    private static final long MO_NO_BASE = 1_000_000_000L;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private FriendRequestMapper friendRequestMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private LocalStorageService localStorageService;

    /**
     * Mo 号 = 10 亿 + 主键，与 QQ 类似为固定长度数字身份，便于记忆与检索
     */
    private static long moNoFromId(long id) {
        return MO_NO_BASE + id;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return Map.of("success", false, "message", "注册失败：用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return Map.of("success", false, "message", "注册失败：密码长度至少 6 位");
        }
        User existingUser = userMapper.selectByUsername(user.getUsername().trim());
        if (existingUser != null) {
            return Map.of("success", false, "message", "注册失败：用户名已占用");
        }
        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        long mo = moNoFromId(user.getId());
        userMapper.updateUserNo(user.getId(), mo);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        return Map.of("success", true, "message", "注册成功", "userNo", mo, "token", token, "userId", user.getId(), "username", user.getUsername(),
                "avatarUrl", "", "bio", "");
    }

    private User findByAccount(String account) {
        if (account == null) {
            return null;
        }
        String t = account.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.matches("\\d{8,12}")) {
            try {
                return userMapper.selectByUserNo(Long.parseLong(t));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return userMapper.selectByUsername(t);
    }

    /**
     * 账号支持：用户名，或 8～12 位 Mo 号
     */
    public Map<String, Object> loginResult(String username, String password) {
        User user = findByAccount(username);
        if (user == null) {
            return Map.of("success", false, "message", "登录失败：账号不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Map.of("success", false, "message", "登录失败：密码错误");
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        return Map.of("success", true, "message", "登录成功", "token", token, "userId", user.getId(), "username", user.getUsername(), "userNo", user.getUserNo(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "", "bio", user.getBio() != null ? user.getBio() : "");
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public User getUserByUserNo(Long userNo) {
        return userMapper.selectByUserNo(userNo);
    }

    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return Map.of("success", false, "message", "新密码长度至少 6 位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Map.of("success", false, "message", "原密码错误");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        return Map.of("success", true, "message", "密码已修改");
    }

    /**
     * 注销账号：删除账号及好友/关注/私信等关系数据；
     * 帖子与回复保留，作者匿名显示（LEFT JOIN 后 authorName 为 null，前端展示“已注销”）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }
        followMapper.deleteByUser(userId);
        friendMapper.deleteByUser(userId);
        friendRequestMapper.deleteByUser(userId);
        messageMapper.deleteByUser(userId);
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            localStorageService.delete(user.getAvatarUrl());
        }
        userMapper.deleteById(userId);
        return Map.of("success", true, "message", "账号已注销");
    }
}
