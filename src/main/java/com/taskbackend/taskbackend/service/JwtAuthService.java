package com.taskbackend.taskbackend.service;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.response.LoginResponse;

public interface JwtAuthService {

    LoginResponse login(LoginRequest request);
}
