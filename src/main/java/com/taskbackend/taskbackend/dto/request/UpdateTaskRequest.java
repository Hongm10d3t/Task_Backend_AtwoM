package com.taskbackend.taskbackend.dto.request;

public record UpdateTaskRequest(String title, String description, boolean completed) {
}
