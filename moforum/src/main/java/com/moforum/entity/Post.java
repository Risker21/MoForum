package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者:Momo同学
 * 日期: 2026/4/12 18:43
 */
@Data
public class Post {
    private Long id;
    private Long userId;
    private Long boardId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer replyCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 联表查询：楼主昵称 */
    private String authorName;
    /** 联表查询：楼主头像 */
    private String avatarUrl;
    /** 联表查询：吧名 */
    private String boardName;
}
