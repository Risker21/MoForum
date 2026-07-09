package com.moforum.config;

import java.security.Principal;

public class UserPrincipal implements Principal {
    private final Long userId;
    private final String username;

    public UserPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long userId() { return userId; }
    public String username() { return username; }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
