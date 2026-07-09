package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.service.FriendService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/friend")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/request")
    public Map<String, Object> request(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody Map<String, Long> body) {
        return friendService.sendRequest(principal.userId(), body.get("userId"));
    }

    @PostMapping("/respond")
    public Map<String, Object> respond(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody Map<String, Object> body) {
        Long requestId = Long.valueOf(body.get("requestId").toString());
        boolean accept = Boolean.parseBoolean(body.get("accept").toString());
        return friendService.respond(requestId, principal.userId(), accept);
    }

    @GetMapping("/list")
    public Map<String, Object> list(@AuthenticationPrincipal UserPrincipal principal) {
        return friendService.list(principal.userId());
    }

    @GetMapping("/requests/received")
    public Map<String, Object> receivedRequests(@AuthenticationPrincipal UserPrincipal principal) {
        return friendService.receivedRequests(principal.userId());
    }

    @GetMapping("/requests/sent")
    public Map<String, Object> sentRequests(@AuthenticationPrincipal UserPrincipal principal) {
        return friendService.sentRequests(principal.userId());
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestParam Long userId) {
        return friendService.status(principal.userId(), userId);
    }

    @GetMapping("/pending-count")
    public Map<String, Object> pendingCount(@AuthenticationPrincipal UserPrincipal principal) {
        int count = friendService.countPendingReceived(principal.userId());
        return Map.of("count", count);
    }
}
