package com.taskbackend.taskbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.response.UserResponse;
import com.taskbackend.taskbackend.exception.InvalidCredentialsException;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.security.SessionUserResolver;
import com.taskbackend.taskbackend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthSessionController.class)
class AuthSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SessionUserResolver sessionUserResolver;

    @Test
    void login_validCredentials_returnsOkAndStoresOnlyUserIdInSession() throws Exception {
        UserResponse user = new UserResponse(1L, "existinguser", "USER");
        when(authService.login(any(LoginRequest.class))).thenReturn(user);

        MvcResult result = mockMvc.perform(post("/api/auth/session/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("existinguser", "secret1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("existinguser"))
                .andExpect(request().sessionAttribute("USER_ID", 1L))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("USER_ID")).isEqualTo(1L);
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/session/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("existinguser", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void me_withActiveSession_returnsCurrentUser() throws Exception {
        when(sessionUserResolver.requireUserId(any(HttpServletRequest.class))).thenReturn(1L);
        UserResponse user = new UserResponse(1L, "existinguser", "USER");
        when(authService.getCurrentUser(1L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/session/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("existinguser"));
    }

    @Test
    void me_withoutSession_returnsUnauthorized() throws Exception {
        when(sessionUserResolver.requireUserId(any(HttpServletRequest.class))).thenThrow(new UnauthorizedException());

        mockMvc.perform(get("/api/auth/session/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 1L);

        mockMvc.perform(post("/api/auth/session/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(session.isInvalid()).isTrue();
    }
}
