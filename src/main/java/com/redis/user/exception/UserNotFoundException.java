package com.redis.user.exception;

import com.redis.user.entity.User;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
