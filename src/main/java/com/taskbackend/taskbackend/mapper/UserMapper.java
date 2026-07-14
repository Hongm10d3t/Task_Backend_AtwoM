package com.taskbackend.taskbackend.mapper;

import com.taskbackend.taskbackend.dto.response.UserResponse;
import com.taskbackend.taskbackend.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
