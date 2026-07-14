package com.taskbackend.taskbackend.service;

import com.taskbackend.taskbackend.dto.request.RegisterRequest;
import com.taskbackend.taskbackend.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);
}
