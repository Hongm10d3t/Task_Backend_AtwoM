package com.taskbackend.taskbackend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

import com.taskbackend.taskbackend.dto.request.CreateTaskRequest;
import com.taskbackend.taskbackend.dto.request.UpdateTaskRequest;
import com.taskbackend.taskbackend.dto.response.TaskResponse;
import com.taskbackend.taskbackend.exception.TaskNotFoundException;
import com.taskbackend.taskbackend.service.TaskService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void getAllTasks_returnsOkWithTaskListWrappedInApiResponse() throws Exception {
        TaskResponse task = new TaskResponse(1L, "Buy milk", null, false);
        when(taskService.getAllTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Buy milk"));
    }

    @Test
    void getTaskById_returnsOkWithTaskWrappedInApiResponse() throws Exception {
        TaskResponse task = new TaskResponse(1L, "Buy milk", null, false);
        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/tasks/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Buy milk"));
    }

    @Test
    void getTaskById_whenNotFound_returnsNotFoundWrappedInApiResponse() throws Exception {
        when(taskService.getTaskById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found with id: 99"));
    }

    @Test
    void createTask_returnsCreatedWrappedInApiResponse() throws Exception {
        CreateTaskRequest input = new CreateTaskRequest("Buy milk", "2 cartons");
        TaskResponse saved = new TaskResponse(1L, "Buy milk", "2 cartons", false);
        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.completed").value(false));
    }

    @Test
    void createTask_missingTitle_returnsBadRequest() throws Exception {
        String body = "{\"description\":\"2 cartons\"}";

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTask_blankTitle_returnsBadRequestWithFieldErrorMessage() throws Exception {
        CreateTaskRequest input = new CreateTaskRequest("", "2 cartons");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.title").value("Title must not be blank"));

        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTask_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTask_whitespaceOnlyTitle_returnsBadRequest() throws Exception {
        CreateTaskRequest input = new CreateTaskRequest("   ", "2 cartons");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTask_titleTooLong_returnsBadRequest() throws Exception {
        CreateTaskRequest input = new CreateTaskRequest("a".repeat(101), "2 cartons");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTask_validRequest_isAccepted() throws Exception {
        CreateTaskRequest input = new CreateTaskRequest("Buy milk", "2 cartons");
        TaskResponse saved = new TaskResponse(1L, "Buy milk", "2 cartons", false);
        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated());

        verify(taskService, times(1)).createTask(any(CreateTaskRequest.class));
    }

    @Test
    void updateTask_blankTitle_returnsBadRequest() throws Exception {
        UpdateTaskRequest input = new UpdateTaskRequest("", "whole wheat", true);

        mockMvc.perform(put("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(any(), any());
    }

    @Test
    void updateTask_returnsOkWrappedInApiResponse() throws Exception {
        UpdateTaskRequest input = new UpdateTaskRequest("Buy bread", "whole wheat", true);
        TaskResponse updated = new TaskResponse(1L, "Buy bread", "whole wheat", true);
        when(taskService.updateTask(eq(1L), any(UpdateTaskRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Buy bread"));
    }

    @Test
    void completeTask_returnsOkWrappedInApiResponse() throws Exception {
        TaskResponse completed = new TaskResponse(1L, "Buy milk", null, true);
        when(taskService.completeTask(1L)).thenReturn(completed);

        mockMvc.perform(patch("/api/tasks/{id}/complete", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    void deleteTask_returnsOkWrappedInApiResponse() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(taskService, times(1)).deleteTask(1L);
    }
}
