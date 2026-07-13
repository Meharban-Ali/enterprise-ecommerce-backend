package com.redis.auth.service;

import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.ForgotPasswordResetRequest;
import com.redis.auth.dto.request.ForgotPasswordVerifyRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.auth.dto.response.ForgotPasswordResponse;

public interface ForgotPasswordService {

    ForgotPasswordResponse retrieveSecurityQuestion(ForgotPasswordRequest request);

    ApiResponse<String> verifySecurityAnswer(ForgotPasswordVerifyRequest request);

    ApiResponse<Void> resetForgotPassword(ForgotPasswordResetRequest request);
}
