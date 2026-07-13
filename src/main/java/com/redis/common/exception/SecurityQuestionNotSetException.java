package com.redis.common.exception;

public class SecurityQuestionNotSetException extends RuntimeException {
    public SecurityQuestionNotSetException(String email) {
        super("Security question has not been set for user: " + email);
    }
}
