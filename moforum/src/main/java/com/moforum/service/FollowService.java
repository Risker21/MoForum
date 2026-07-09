package com.moforum.service;

import com.moforum.entity.Follow;
import com.moforum.mapper.FollowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FollowService {

    private final FollowMapper followMapper;

    public FollowService(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    @Transactional
    public Map<String, Object> toggle(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            return Map.of("success", false, "message", "不能关注自己");
        }
        Follow existing = followMapper.selectByFollowerAndFollowed(followerId, followedId);
        if (existing != null) {
            followMapper.delete(followerId, followedId);
            return Map.of("success", true, "message", "已取消关注", "followed", false);
        } else {
            Follow f = new Follow();
            f.setFollowerId(followerId);
            f.setFollowedId(followedId);
            followMapper.insert(f);
            return Map.of("success", true, "message", "已关注", "followed", true);
        }
    }

    public Map<String, Object> followers(Long userId, Long currentUserId) {
        List<Follow> list = followMapper.selectFollowers(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Follow f : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", f.getFollowerId());
            item.put("followTime", f.getCreateTime());
            if (currentUserId != null) {
                item.put("isFollowed", followMapper.selectByFollowerAndFollowed(currentUserId, f.getFollowerId()) != null);
            }
            result.add(item);
        }
        return Map.of("success", true, "list", result);
    }

    public Map<String, Object> following(Long userId, Long currentUserId) {
        List<Follow> list = followMapper.selectFollowing(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Follow f : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", f.getFollowedId());
            item.put("followTime", f.getCreateTime());
            if (currentUserId != null) {
                item.put("isFollowed", followMapper.selectByFollowerAndFollowed(currentUserId, f.getFollowedId()) != null);
            }
            result.add(item);
        }
        return Map.of("success", true, "list", result);
    }

    public Map<String, Object> status(Long followerId, Long followedId) {
        Follow f = followMapper.selectByFollowerAndFollowed(followerId, followedId);
        return Map.of("followed", f != null);
    }

    public Map<String, Object> counts(Long userId) {
        int followers = followMapper.countFollowers(userId);
        int following = followMapper.countFollowing(userId);
        return Map.of("followers", followers, "following", following);
    }
}
