package com.moforum.service;

import com.moforum.entity.Post;
import com.moforum.entity.Reply;
import com.moforum.mapper.PostMapper;
import com.moforum.mapper.ReplyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReplyService {

    @Autowired
    private ReplyMapper replyMapper;
    @Autowired
    private PostMapper postMapper;

    public List<Reply> listByPost(Long postId) {
        return replyMapper.listByPostId(postId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Reply reply) {
        if (reply.getPostId() == null) {
            return Map.of("success", false, "message", "回复失败：缺少帖子 ID");
        }
        if (reply.getUserId() == null) {
            return Map.of("success", false, "message", "回复失败：未指定用户");
        }
        if (reply.getContent() == null || reply.getContent().isBlank()) {
            return Map.of("success", false, "message", "回复失败：内容不能为空");
        }
        Post p = postMapper.selectBasicById(reply.getPostId());
        if (p == null) {
            return Map.of("success", false, "message", "回复失败：帖子不存在");
        }
        replyMapper.insert(reply);
        postMapper.incrementReplyCount(reply.getPostId());
        return Map.of("success", true, "message", "回复成功");
    }
}
