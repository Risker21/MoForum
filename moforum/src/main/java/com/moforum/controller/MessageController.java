package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.service.MessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> body) {
        Long toUserId = Long.valueOf(body.get("toUserId").toString());
        String content = body.get("content").toString();
        return messageService.send(principal.userId(), toUserId, content);
    }

    @GetMapping("/conversations")
    public Map<String, Object> conversations(@AuthenticationPrincipal UserPrincipal principal) {
        return messageService.conversations(principal.userId());
    }

    @GetMapping("/list")
    public Map<String, Object> list(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestParam Long userId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        return messageService.list(principal.userId(), userId, page, pageSize);
    }

    @PostMapping("/read")
    public Map<String, Object> read(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Long> body) {
        return messageService.markRead(principal.userId(), body.get("userId"));
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", messageService.countUnread(principal.userId()));
    }
}
