package com.taskbackend.taskbackend.service;

import java.util.List;

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.TaskResponse;

public interface TaskService {

    List<TaskResponse> getAllTasks();

    TaskResponse getTaskById(Long id);

    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse updateTask(Long id, UpdateTaskRequest request);

    TaskResponse completeTask(Long id);

    void deleteTask(Long id);
}
