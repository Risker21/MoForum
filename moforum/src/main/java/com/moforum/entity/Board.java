package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Board {
    private Long id;
    private String name;
    private String description;
    private String avatar;
    private Integer sortOrder;
    private Integer postCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
