package com.taskbackend.taskbackend.dto.response;

public record TaskResponse(Long id, String title, String description, boolean completed) {
}
