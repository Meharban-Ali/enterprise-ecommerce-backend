package com.redis.user.service;

import com.redis.user.entity.User;

public interface UserSessionService {
    void loginSession(User user);
    void logoutSession(String email);
    void updateSessionActivity(User user);
}
