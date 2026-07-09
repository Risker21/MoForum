package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Reply {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    /** 联表查询：作者昵称 */
    private String authorName;
}
