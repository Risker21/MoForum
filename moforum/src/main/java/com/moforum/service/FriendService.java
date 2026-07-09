package com.moforum.service;

import com.moforum.entity.Friend;
import com.moforum.entity.FriendRequest;
import com.moforum.mapper.FriendMapper;
import com.moforum.mapper.FriendRequestMapper;
import com.moforum.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FriendService {

    private final FriendRequestMapper friendRequestMapper;
    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    public FriendService(FriendRequestMapper friendRequestMapper, FriendMapper friendMapper, UserMapper userMapper) {
        this.friendRequestMapper = friendRequestMapper;
        this.friendMapper = friendMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Map<String, Object> sendRequest(Long fromId, Long toId) {
        if (fromId.equals(toId)) {
            return Map.of("success", false, "message", "不能添加自己为好友");
        }
        if (friendMapper.checkFriendship(fromId, toId) > 0) {
            return Map.of("success", false, "message", "已经是好友了");
        }
        FriendRequest existing = friendRequestMapper.selectByUsers(fromId, toId);
        if (existing != null) {
            if (existing.getStatus() == 0) return Map.of("success", false, "message", "已发送过申请");
            if (existing.getStatus() == 1) return Map.of("success", false, "message", "已经是好友了");
            existing.setStatus(0);
            friendRequestMapper.updateStatus(existing.getId(), 0);
            return Map.of("success", true, "message", "好友申请已发送");
        }
        FriendRequest req = new FriendRequest();
        req.setFromId(fromId);
        req.setToId(toId);
        friendRequestMapper.insert(req);
        return Map.of("success", true, "message", "好友申请已发送");
    }

    @Transactional
    public Map<String, Object> respond(Long requestId, Long userId, boolean accept) {
        FriendRequest req = friendRequestMapper.selectById(requestId);
        if (req == null) return Map.of("success", false, "message", "申请不存在");
        if (!req.getToId().equals(userId)) return Map.of("success", false, "message", "无权操作");
        if (req.getStatus() != 0) return Map.of("success", false, "message", "申请已处理");

        if (accept) {
            friendRequestMapper.updateStatus(requestId, 1);
            Friend f = new Friend();
            long id1 = Math.min(req.getFromId(), req.getToId());
            long id2 = Math.max(req.getFromId(), req.getToId());
            f.setUserId1(id1);
            f.setUserId2(id2);
            friendMapper.insert(f);
            return Map.of("success", true, "message", "已同意好友申请");
        } else {
            friendRequestMapper.updateStatus(requestId, 2);
            return Map.of("success", true, "message", "已拒绝好友申请");
        }
    }

    public Map<String, Object> list(Long userId) {
        List<Long> friendIds = friendMapper.selectFriendIds(userId);
        List<Map<String, Object>> friends = new ArrayList<>();
        for (Long fid : friendIds) {
            com.moforum.entity.User u = userMapper.selectById(fid);
            if (u != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", u.getId());
                item.put("username", u.getUsername());
                item.put("userNo", u.getUserNo());
                item.put("avatarUrl", u.getAvatarUrl());
                friends.add(item);
            }
        }
        return Map.of("success", true, "list", friends);
    }

    public Map<String, Object> receivedRequests(Long userId) {
        List<FriendRequest> list = friendRequestMapper.selectReceived(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRequest req : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", req.getId());
            item.put("fromId", req.getFromId());
            item.put("createTime", req.getCreateTime());
            com.moforum.entity.User u = userMapper.selectById(req.getFromId());
            item.put("username", u != null ? u.getUsername() : "未知");
            result.add(item);
        }
        return Map.of("success", true, "list", result);
    }

    public Map<String, Object> sentRequests(Long userId) {
        List<FriendRequest> list = friendRequestMapper.selectSent(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRequest req : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", req.getId());
            item.put("toId", req.getToId());
            item.put("status", req.getStatus());
            item.put("createTime", req.getCreateTime());
            com.moforum.entity.User u = userMapper.selectById(req.getToId());
            item.put("username", u != null ? u.getUsername() : "未知");
            result.add(item);
        }
        return Map.of("success", true, "list", result);
    }

    public Map<String, Object> status(Long userId1, Long userId2) {
        if (friendMapper.checkFriendship(userId1, userId2) > 0) {
            return Map.of("status", "FRIENDS");
        }
        FriendRequest req = friendRequestMapper.selectByUsers(userId1, userId2);
        if (req != null && req.getStatus() == 0) return Map.of("status", "PENDING");
        req = friendRequestMapper.selectByUsers(userId2, userId1);
        if (req != null && req.getStatus() == 0) return Map.of("status", "PENDING_RECEIVED");
        return Map.of("status", "NONE");
    }

    public int countPendingReceived(Long userId) {
        return friendRequestMapper.countPendingReceived(userId);
    }
}
