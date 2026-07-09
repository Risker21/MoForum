package com.moforum.service;

import com.moforum.entity.Message;
import com.moforum.mapper.FriendMapper;
import com.moforum.mapper.MessageMapper;
import com.moforum.mapper.UserMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MessageService {

    private final MessageMapper messageMapper;
    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageMapper messageMapper, FriendMapper friendMapper,
                          UserMapper userMapper, SimpMessagingTemplate messagingTemplate) {
        this.messageMapper = messageMapper;
        this.friendMapper = friendMapper;
        this.userMapper = userMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Map<String, Object> send(Long fromId, Long toId, String content) {
        if (content == null || content.isBlank()) {
            return Map.of("success", false, "message", "消息不能为空");
        }
        if (friendMapper.checkFriendship(fromId, toId) == 0 && !fromId.equals(toId)) {
            return Map.of("success", false, "message", "仅好友之间可发送消息");
        }
        Message msg = new Message();
        msg.setFromId(fromId);
        msg.setToId(toId);
        msg.setContent(content);
        messageMapper.insert(msg);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", msg.getId());
        payload.put("fromId", fromId);
        payload.put("content", content);
        payload.put("createTime", msg.getCreateTime());
        try {
            messagingTemplate.convertAndSendToUser(toId.toString(), "/queue/messages", payload);
        } catch (Exception ignored) {}

        return Map.of("success", true, "message", "发送成功", "id", msg.getId());
    }

    public Map<String, Object> conversations(Long userId) {
        List<Message> latestMessages = messageMapper.selectConversations(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> added = new HashSet<>();
        for (Message msg : latestMessages) {
            Long otherId = msg.getFromId().equals(userId) ? msg.getToId() : msg.getFromId();
            if (added.contains(otherId)) continue;
            added.add(otherId);
            com.moforum.entity.User u = userMapper.selectById(otherId);
            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("userId", otherId);
            conv.put("username", u != null ? u.getUsername() : "未知");
            conv.put("avatarUrl", u != null ? u.getAvatarUrl() : null);
            conv.put("lastContent", msg.getContent());
            conv.put("lastTime", msg.getCreateTime());
            conv.put("unread", msg.getToId().equals(userId) && msg.getRead() == 0 ? 1 : 0);
            result.add(conv);
        }
        return Map.of("success", true, "list", result);
    }

    public Map<String, Object> list(Long userId, Long friendId, int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(50, pageSize));
        int offset = (page - 1) * pageSize;
        List<Message> messages = messageMapper.selectConversation(userId, friendId, offset, pageSize);
        int total = messageMapper.countConversation(userId, friendId);
        return Map.of("success", true, "list", messages, "total", total, "page", page, "pageSize", pageSize);
    }

    @Transactional
    public Map<String, Object> markRead(Long toUserId, Long fromUserId) {
        messageMapper.markRead(toUserId, fromUserId);
        return Map.of("success", true);
    }

    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId);
    }
}
