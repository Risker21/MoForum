package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.entity.Post;
import com.moforum.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping("/create")
    public Map<String, Object> createPost(@RequestBody Post post) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            post.setUserId(((UserPrincipal) auth.getPrincipal()).userId());
        }
        return postService.createPost(post);
    }

    @PostMapping("/delete")
    public Map<String, Object> deletePost(@RequestBody Map<String, Long> body) {
        Long postId = body.get("postId");
        if (postId == null) {
            return Map.of("success", false, "message", "参数不完整");
        }
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        long userId = 0;
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            userId = ((UserPrincipal) auth.getPrincipal()).userId();
        }
        return postService.deletePost(postId, userId);
    }

    @GetMapping("/list")
    public Map<String, Object> getPostList(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (userId != null) {
            return postService.pageByUser(userId, page, pageSize);
        }
        return postService.pageByBoard(boardId != null ? boardId : 0, page, pageSize);
    }

    @GetMapping("/latest")
    public List<Post> latest(@RequestParam(defaultValue = "8") int limit) {
        return postService.latest(limit);
    }

    @GetMapping("/detail")
    public ResponseEntity<Post> detail(@RequestParam Long id) {
        Post p = postService.getDetail(id);
        return p == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(p);
    }
}
