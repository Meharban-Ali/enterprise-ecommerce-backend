package com.redis.common.exception;

public class InvalidSecurityAnswerException extends RuntimeException {
    public InvalidSecurityAnswerException() {
        super("Invalid security answer provided");
    }
}
