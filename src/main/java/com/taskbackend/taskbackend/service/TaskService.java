package com.taskbackend.taskbackend.service;

import java.util.List;

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.TaskResponse;

public interface TaskService {

    List<TaskResponse> getAllTasks(Long userId);

    TaskResponse getTaskById(Long id, Long userId);

    TaskResponse createTask(CreateTaskRequest request, Long userId);

    TaskResponse updateTask(Long id, UpdateTaskRequest request, Long userId);

    TaskResponse completeTask(Long id, Long userId);

    void deleteTask(Long id, Long userId);
}
