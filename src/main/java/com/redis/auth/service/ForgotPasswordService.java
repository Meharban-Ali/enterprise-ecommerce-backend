package com.redis.auth.service;

import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.common.dto.ApiResponse;

public interface ForgotPasswordService {

    ApiResponse<Void> requestPasswordReset(ForgotPasswordRequest request);

    ApiResponse<Void> resetPassword(ResetPasswordRequest request);
}
