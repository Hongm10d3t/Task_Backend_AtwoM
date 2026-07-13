package com.taskbackend.taskbackend.mapper;

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.TaskResponse;
import com.taskbackend.taskbackend.entity.Task;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static Task toEntity(CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        return task;
    }

    public static void applyUpdate(Task task, UpdateTaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCompleted(request.completed());
    }

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.isCompleted());
    }
}
