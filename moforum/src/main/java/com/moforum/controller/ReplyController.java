package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.entity.Reply;
import com.moforum.service.ReplyService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/reply")
public class ReplyController {

    @Autowired
    private ReplyService replyService;

    @GetMapping("/list")
    public List<Reply> list(@RequestParam Long postId) {
        return replyService.listByPost(postId);
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Reply reply) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            reply.setUserId(((UserPrincipal) auth.getPrincipal()).userId());
        }
        return replyService.create(reply);
    }
}
