package com.taskbackend.taskbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.TaskResponse;
import com.taskbackend.taskbackend.entity.Task;
import com.taskbackend.taskbackend.exception.TaskNotFoundException;
import com.taskbackend.taskbackend.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task taskWithId(Long id, String title, String description, boolean completed) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setCompleted(completed);
        return task;
    }

    @Test
    void getAllTasks_returnsAllTasksMappedToResponses() {
        Task task1 = taskWithId(1L, "Task 1", "Desc 1", false);
        Task task2 = taskWithId(2L, "Task 2", "Desc 2", true);
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

        List<TaskResponse> result = taskService.getAllTasks();

        assertThat(result).containsExactly(
                new TaskResponse(1L, "Task 1", "Desc 1", false),
                new TaskResponse(2L, "Task 2", "Desc 2", true));
    }

    @Test
    void getTaskById_whenFound_returnsTaskResponse() {
        Task task = taskWithId(1L, "Task 1", "Desc 1", false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.getTaskById(1L);

        assertThat(result).isEqualTo(new TaskResponse(1L, "Task 1", "Desc 1", false));
    }

    @Test
    void getTaskById_whenNotFound_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTask_defaultsCompletedToFalseAndAssignsGeneratedId() {
        CreateTaskRequest request = new CreateTaskRequest("Buy milk", "2 cartons");
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TaskResponse result = taskService.createTask(request);

        assertThat(result).isEqualTo(new TaskResponse(1L, "Buy milk", "2 cartons", false));
    }

    @Test
    void updateTask_keepsIdAndOnlyUpdatesTitleDescriptionCompleted() {
        Task existing = taskWithId(1L, "Old title", "Old description", false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest("New title", "New description", true);

        TaskResponse result = taskService.updateTask(1L, request);

        assertThat(result).isEqualTo(new TaskResponse(1L, "New title", "New description", true));
    }

    @Test
    void completeTask_setsCompletedTrue() {
        Task existing = taskWithId(1L, "Task 1", "Desc 1", false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.completeTask(1L);

        assertThat(result.completed()).isTrue();
    }

    @Test
    void deleteTask_whenExists_deletesTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTask_whenNotFound_throwsTaskNotFoundExceptionAndDoesNotDelete() {
        when(taskRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).deleteById(anyLong());
    }
}
