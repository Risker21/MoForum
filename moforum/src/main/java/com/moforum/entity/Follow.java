package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Follow {
    private Long id;
    private Long followerId;
    private Long followedId;
    private LocalDateTime createTime;
}
