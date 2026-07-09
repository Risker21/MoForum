package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Friend {
    private Long id;
    private Long userId1;
    private Long userId2;
    private LocalDateTime createTime;
}
