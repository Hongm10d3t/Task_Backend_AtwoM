package com.taskbackend.taskbackend.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
