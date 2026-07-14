package com.taskbackend.taskbackend.dto.response;

public record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {
}
