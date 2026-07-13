package com.taskbackend.taskbackend.service;

import java.util.List;

import com.taskbackend.taskbackend.entity.Task;

public interface TaskService {

    List<Task> getAllTasks();

    Task getTaskById(Long id);

    Task createTask(Task task);

    Task updateTask(Long id, Task task);

    Task completeTask(Long id);

    void deleteTask(Long id);
}
