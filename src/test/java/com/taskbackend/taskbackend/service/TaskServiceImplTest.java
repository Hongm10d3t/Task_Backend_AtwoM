package com.taskbackend.taskbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.TaskNotFoundException;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.repository.TaskRepository;
import com.taskbackend.taskbackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    private static final Long USER_A_ID = 1L;
    private static final Long USER_B_ID = 2L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

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

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void getAllTasks_returnsOnlyTasksOwnedByGivenUser() {
        Task task1 = taskWithId(1L, "Task 1", "Desc 1", false);
        Task task2 = taskWithId(2L, "Task 2", "Desc 2", true);
        when(taskRepository.findAllByUserId(USER_A_ID)).thenReturn(List.of(task1, task2));

        List<TaskResponse> result = taskService.getAllTasks(USER_A_ID);

        assertThat(result).containsExactly(
                new TaskResponse(1L, "Task 1", "Desc 1", false),
                new TaskResponse(2L, "Task 2", "Desc 2", true));
    }

    @Test
    void getTaskById_whenOwnedByUser_returnsTaskResponse() {
        Task task = taskWithId(1L, "Task 1", "Desc 1", false);
        when(taskRepository.findByIdAndUserId(1L, USER_A_ID)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.getTaskById(1L, USER_A_ID);

        assertThat(result).isEqualTo(new TaskResponse(1L, "Task 1", "Desc 1", false));
    }

    @Test
    void getTaskById_whenNotFound_throwsTaskNotFoundException() {
        when(taskRepository.findByIdAndUserId(99L, USER_A_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L, USER_A_ID))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getTaskById_whenOwnedByAnotherUser_throwsTaskNotFoundException() {
        when(taskRepository.findByIdAndUserId(1L, USER_B_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(1L, USER_B_ID))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void createTask_assignsTaskToCurrentUserAndDefaultsCompletedToFalse() {
        CreateTaskRequest request = new CreateTaskRequest("Buy milk", "2 cartons");
        User owner = userWithId(USER_A_ID);
        when(userRepository.findById(USER_A_ID)).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TaskResponse result = taskService.createTask(request, USER_A_ID);

        assertThat(result).isEqualTo(new TaskResponse(1L, "Buy milk", "2 cartons", false));
        verify(taskRepository).save(argThat(task -> task.getUser() == owner && !task.isCompleted()));
    }

    @Test
    void createTask_whenUserIdInvalid_throwsUnauthorizedExceptionAndDoesNotSave() {
        CreateTaskRequest request = new CreateTaskRequest("Buy milk", "2 cartons");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request, 99L))
                .isInstanceOf(UnauthorizedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_whenOwnedByUser_keepsIdAndOnlyUpdatesTitleDescriptionCompleted() {
        Task existing = taskWithId(1L, "Old title", "Old description", false);
        when(taskRepository.findByIdAndUserId(1L, USER_A_ID)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest("New title", "New description", true);

        TaskResponse result = taskService.updateTask(1L, request, USER_A_ID);

        assertThat(result).isEqualTo(new TaskResponse(1L, "New title", "New description", true));
    }

    @Test
    void updateTask_whenOwnedByAnotherUser_throwsTaskNotFoundExceptionAndDoesNotSave() {
        when(taskRepository.findByIdAndUserId(1L, USER_B_ID)).thenReturn(Optional.empty());

        UpdateTaskRequest request = new UpdateTaskRequest("New title", "New description", true);

        assertThatThrownBy(() -> taskService.updateTask(1L, request, USER_B_ID))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void completeTask_whenOwnedByUser_setsCompletedTrue() {
        Task existing = taskWithId(1L, "Task 1", "Desc 1", false);
        when(taskRepository.findByIdAndUserId(1L, USER_A_ID)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.completeTask(1L, USER_A_ID);

        assertThat(result.completed()).isTrue();
    }

    @Test
    void completeTask_whenOwnedByAnotherUser_throwsTaskNotFoundException() {
        when(taskRepository.findByIdAndUserId(1L, USER_B_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.completeTask(1L, USER_B_ID))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deleteTask_whenOwnedByUser_deletesTask() {
        Task existing = taskWithId(1L, "Task 1", "Desc 1", false);
        when(taskRepository.findByIdAndUserId(1L, USER_A_ID)).thenReturn(Optional.of(existing));

        taskService.deleteTask(1L, USER_A_ID);

        verify(taskRepository, times(1)).delete(existing);
    }

    @Test
    void deleteTask_whenOwnedByAnotherUser_throwsTaskNotFoundExceptionAndDoesNotDelete() {
        when(taskRepository.findByIdAndUserId(1L, USER_B_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(1L, USER_B_ID))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("1");

        verify(taskRepository, never()).delete(any());
    }
}
