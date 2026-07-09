package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long fromId;
    private Long toId;
    private String content;
    private Integer read;
    private LocalDateTime createTime;
}
