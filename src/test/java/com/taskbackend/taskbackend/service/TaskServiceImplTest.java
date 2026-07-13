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

import com.taskbackend.taskbackend.entity.Task;
import com.taskbackend.taskbackend.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void getAllTasks_returnsAllTasksFromRepository() {
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(2L);
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

        List<Task> result = taskService.getAllTasks();

        assertThat(result).containsExactly(task1, task2);
    }

    @Test
    void getTaskById_whenFound_returnsTask() {
        Task task = new Task();
        task.setId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);

        assertThat(result).isSameAs(task);
    }

    @Test
    void getTaskById_whenNotFound_throwsIllegalArgumentException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTask_ignoresClientProvidedIdAndDefaultsCompletedToFalse() {
        Task input = new Task();
        input.setId(123L);
        input.setTitle("Buy milk");
        input.setCompleted(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.createTask(input);

        assertThat(result.getId()).isNull();
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getTitle()).isEqualTo("Buy milk");
        verify(taskRepository).save(input);
    }

    @Test
    void updateTask_keepsIdAndOnlyUpdatesTitleDescriptionCompleted() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setTitle("Old title");
        existing.setDescription("Old description");
        existing.setCompleted(false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task changes = new Task();
        changes.setId(999L);
        changes.setTitle("New title");
        changes.setDescription("New description");
        changes.setCompleted(true);

        Task result = taskService.updateTask(1L, changes);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void completeTask_setsCompletedTrue() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setCompleted(false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.completeTask(1L);

        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void deleteTask_whenExists_deletesTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTask_whenNotFound_throwsIllegalArgumentExceptionAndDoesNotDelete() {
        when(taskRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).deleteById(anyLong());
    }
}
