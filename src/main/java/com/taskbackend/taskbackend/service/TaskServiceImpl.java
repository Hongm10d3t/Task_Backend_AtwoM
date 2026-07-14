package com.taskbackend.taskbackend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.TaskResponse;
import com.taskbackend.taskbackend.entity.Task;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.TaskNotFoundException;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.mapper.TaskMapper;
import com.taskbackend.taskbackend.repository.TaskRepository;
import com.taskbackend.taskbackend.repository.UserRepository;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<TaskResponse> getAllTasks(Long userId) {
        return taskRepository.findAllByUserId(userId).stream()
                .map(TaskMapper::toResponse)
                .toList();
    }

    @Override
    public TaskResponse getTaskById(Long id, Long userId) {
        return TaskMapper.toResponse(findTaskOrThrow(id, userId));
    }

    @Override
    public TaskResponse createTask(CreateTaskRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UnauthorizedException::new);

        Task task = TaskMapper.toEntity(request);
        task.setCompleted(false);
        task.setUser(user);

        return TaskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request, Long userId) {
        Task task = findTaskOrThrow(id, userId);
        TaskMapper.applyUpdate(task, request);
        return TaskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse completeTask(Long id, Long userId) {
        Task task = findTaskOrThrow(id, userId);
        task.setCompleted(true);
        return TaskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public void deleteTask(Long id, Long userId) {
        Task task = findTaskOrThrow(id, userId);
        taskRepository.delete(task);
    }

    private Task findTaskOrThrow(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
