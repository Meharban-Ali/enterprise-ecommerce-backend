package com.redis.user.exception;

import com.redis.user.entity.User;

public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
}
