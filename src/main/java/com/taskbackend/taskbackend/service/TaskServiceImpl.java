package com.taskbackend.taskbackend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.taskbackend.taskbackend.entity.Task;
import com.taskbackend.taskbackend.repository.TaskRepository;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + id));
    }

    @Override
    public Task createTask(Task task) {
        task.setId(null);
        task.setCompleted(false);
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long id, Task task) {
        Task existingTask = getTaskById(id);
        existingTask.setTitle(task.getTitle());
        existingTask.setDescription(task.getDescription());
        existingTask.setCompleted(task.isCompleted());
        return taskRepository.save(existingTask);
    }

    @Override
    public Task completeTask(Long id) {
        Task existingTask = getTaskById(id);
        existingTask.setCompleted(true);
        return taskRepository.save(existingTask);
    }

    @Override
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new IllegalArgumentException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }
}
