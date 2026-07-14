package com.taskbackend.taskbackend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.ApiResponse;
import com.taskbackend.taskbackend.dto.response.TaskResponse;
import com.taskbackend.taskbackend.security.SessionUserResolver;
import com.taskbackend.taskbackend.service.TaskService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final SessionUserResolver sessionUserResolver;

    public TaskController(TaskService taskService, SessionUserResolver sessionUserResolver) {
        this.taskService = taskService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks(HttpServletRequest request) {
        Long userId = sessionUserResolver.requireUserId(request);
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllTasks(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = sessionUserResolver.requireUserId(request);
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskById(id, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request,
            HttpServletRequest servletRequest) {
        Long userId = sessionUserResolver.requireUserId(servletRequest);
        TaskResponse created = taskService.createTask(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(@PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request, HttpServletRequest servletRequest) {
        Long userId = sessionUserResolver.requireUserId(servletRequest);
        TaskResponse updated = taskService.updateTask(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", updated));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable Long id, HttpServletRequest request) {
        Long userId = sessionUserResolver.requireUserId(request);
        TaskResponse completed = taskService.completeTask(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Task marked as completed", completed));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        Long userId = sessionUserResolver.requireUserId(request);
        taskService.deleteTask(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }
}
