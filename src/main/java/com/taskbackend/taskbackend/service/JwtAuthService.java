package com.taskbackend.taskbackend.service;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.request.RefreshTokenRequest;
import com.taskbackend.taskbackend.dto.response.AccessTokenResponse;
import com.taskbackend.taskbackend.dto.response.LoginResponse;

public interface JwtAuthService {

    LoginResponse login(LoginRequest request);

    AccessTokenResponse refreshAccessToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
