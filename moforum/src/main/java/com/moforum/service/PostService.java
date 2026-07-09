package com.moforum.service;

import com.moforum.entity.Post;
import com.moforum.mapper.BoardMapper;
import com.moforum.mapper.PostMapper;
import com.moforum.mapper.ReplyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作者:Momo同学
 * 日期: 2026/4/12 22:02
 */
@Service
public class PostService {
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private BoardMapper boardMapper;
    @Autowired
    private ReplyMapper replyMapper;
    @Autowired
    private BoardService boardService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPost(Post post) {
        if (post.getBoardId() == null) {
            return Map.of("success", false, "message", "发布失败：请选择贴吧");
        }
        if (boardMapper.selectById(post.getBoardId()) == null) {
            return Map.of("success", false, "message", "发布失败：贴吧不存在");
        }
        if (post.getUserId() == null) {
            return Map.of("success", false, "message", "发布失败：未指定用户");
        }
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            return Map.of("success", false, "message", "发布失败：标题不能为空");
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            return Map.of("success", false, "message", "发布失败：正文不能为空");
        }
        post.setViewCount(0);
        post.setReplyCount(0);
        postMapper.insert(post);
        boardMapper.incrementPostCount(post.getBoardId());
        boardService.evictBoardCache();
        return Map.of("success", true, "message", "发布成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deletePost(long postId, long operatorUserId) {
        Post p = postMapper.selectBasicById(postId);
        if (p == null) {
            return Map.of("success", false, "message", "删除失败：帖子不存在");
        }
        if (p.getUserId() == null || !p.getUserId().equals(operatorUserId)) {
            return Map.of("success", false, "message", "删除失败：只能删除自己发布的帖子");
        }
        replyMapper.deleteByPostId(postId);
        postMapper.deleteById(postId);
        if (p.getBoardId() != null) {
            boardMapper.decrementPostCount(p.getBoardId());
        }
        boardService.evictBoardCache();
        return Map.of("success", true, "message", "删除成功");
    }

    public Map<String, Object> pageByBoard(long boardId, int page, int pageSize) {
        int p = Math.max(page, 1);
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (p - 1) * size;
        List<Post> list = postMapper.selectPageByBoard(boardId, offset, size);
        int total = postMapper.countByBoard(boardId);
        Map<String, Object> res = new HashMap<>();
        res.put("list", list);
        res.put("total", total);
        res.put("page", p);
        res.put("pageSize", size);
        return res;
    }

    public List<Post> latest(int limit) {
        int n = Math.min(Math.max(limit, 1), 50);
        return postMapper.selectLatest(n);
    }

    public Map<String, Object> pageByUser(Long userId, int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(50, pageSize));
        int offset = (page - 1) * pageSize;
        List<Post> list = postMapper.selectByUserId(userId, offset, pageSize);
        int total = postMapper.countByUserId(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    public Post getDetail(Long id) {
        if (id == null) {
            return null;
        }
        Post p = postMapper.selectByIdWithAuthor(id);
        if (p == null) {
            return null;
        }
        postMapper.incrementViewCount(id);
        if (p.getViewCount() != null) {
            p.setViewCount(p.getViewCount() + 1);
        } else {
            p.setViewCount(1);
        }
        return p;
    }
}
