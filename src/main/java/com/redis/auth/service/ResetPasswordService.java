package com.redis.auth.service;

import com.redis.auth.dto.request.ResetPasswordRequest;

public interface ResetPasswordService {
    void resetPassword(ResetPasswordRequest request);
}
