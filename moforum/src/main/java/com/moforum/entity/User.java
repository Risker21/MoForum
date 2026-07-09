package com.moforum.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者:Momo同学
 * 日期: 2026/4/12 18:39
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    /** Mo 号：对外唯一，类似 QQ 号，用于登录与查询 */
    private Long userNo;
    private String bio;
    private String avatarUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
