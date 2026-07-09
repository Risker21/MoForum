package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequest {
    private Long id;
    private Long fromId;
    private Long toId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
