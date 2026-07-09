package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.service.FollowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody Map<String, Long> body) {
        return followService.toggle(principal.userId(), body.get("userId"));
    }

    @GetMapping("/followers")
    public Map<String, Object> followers(@RequestParam Long userId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.userId() : null;
        return followService.followers(userId, currentUserId);
    }

    @GetMapping("/following")
    public Map<String, Object> following(@RequestParam Long userId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        Long currentUserId = principal != null ? principal.userId() : null;
        return followService.following(userId, currentUserId);
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestParam Long userId) {
        if (principal == null) {
            return Map.of("followed", false);
        }
        return followService.status(principal.userId(), userId);
    }

    @GetMapping("/counts")
    public Map<String, Object> counts(@RequestParam Long userId) {
        return followService.counts(userId);
    }
}
