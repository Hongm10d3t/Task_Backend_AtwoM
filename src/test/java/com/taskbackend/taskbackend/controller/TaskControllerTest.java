package com.taskbackend.taskbackend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.taskbackend.taskbackend.entity.Task;
import com.taskbackend.taskbackend.service.TaskService;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void getAllTasks_returnsOkWithTaskList() throws Exception {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Buy milk");
        when(taskService.getAllTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Buy milk"));
    }

    @Test
    void getTaskById_returnsOkWithTask() throws Exception {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Buy milk");
        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/tasks/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Buy milk"));
    }

    @Test
    void createTask_returnsCreated() throws Exception {
        Task input = new Task();
        input.setTitle("Buy milk");

        Task saved = new Task();
        saved.setId(1L);
        saved.setTitle("Buy milk");
        saved.setCompleted(false);
        when(taskService.createTask(any(Task.class))).thenReturn(saved);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void updateTask_returnsOk() throws Exception {
        Task input = new Task();
        input.setTitle("Buy bread");

        Task updated = new Task();
        updated.setId(1L);
        updated.setTitle("Buy bread");
        when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(updated);

        mockMvc.perform(put("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy bread"));
    }

    @Test
    void completeTask_returnsOk() throws Exception {
        Task completed = new Task();
        completed.setId(1L);
        completed.setCompleted(true);
        when(taskService.completeTask(1L)).thenReturn(completed);

        mockMvc.perform(patch("/api/tasks/{id}/complete", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void deleteTask_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(1L);
    }
}
