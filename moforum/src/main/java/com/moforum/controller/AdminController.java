package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || !"admin".equals(principal.username())) {
            return Map.of("success", false, "message", "无权限");
        }

        Long userCount = jdbc.queryForObject("SELECT COUNT(*) FROM t_user", Long.class);
        Long postCount = jdbc.queryForObject("SELECT COUNT(*) FROM t_post", Long.class);
        Long replyCount = jdbc.queryForObject("SELECT COUNT(*) FROM t_reply", Long.class);
        Long boardCount = jdbc.queryForObject("SELECT COUNT(*) FROM t_board", Long.class);

        List<Map<String, Object>> postsPerBoard = jdbc.queryForList(
                "SELECT b.name, COUNT(p.id) AS count FROM t_board b LEFT JOIN t_post p ON p.board_id = b.id GROUP BY b.id ORDER BY count DESC");

        List<Map<String, Object>> dailyPosts = jdbc.queryForList(
                "SELECT DATE(create_time) AS date, COUNT(*) AS count FROM t_post WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY DATE(create_time) ORDER BY date");

        List<Map<String, Object>> topUsers = jdbc.queryForList(
                "SELECT u.username, COUNT(p.id) AS count FROM t_user u JOIN t_post p ON p.user_id = u.id GROUP BY u.id ORDER BY count DESC LIMIT 10");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("userCount", userCount);
        result.put("postCount", postCount);
        result.put("replyCount", replyCount);
        result.put("boardCount", boardCount);
        result.put("postsPerBoard", postsPerBoard);
        result.put("dailyPosts", dailyPosts);
        result.put("topUsers", topUsers);
        return result;
    }
}
