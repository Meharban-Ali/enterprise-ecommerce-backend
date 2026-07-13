package com.redis.user.service;

import com.redis.common.dto.ProfileUpdateRequest;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.auth.dto.response.RegisterResponse;
import com.redis.user.dto.response.UserResponse;

public interface UserService {

    // Registers a new user with validation, BCrypt password hashing, and default user role assignment
    RegisterResponse registerUser(RegisterRequest request);

    // Updates profile details (username, email) with validation and duplication checks
    UserResponse updateProfile(Long userId, ProfileUpdateRequest request);

    // Retrieves user details by email
    UserResponse getUserByEmail(String email);
}
