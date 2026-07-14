package com.taskbackend.taskbackend.dto.response;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
