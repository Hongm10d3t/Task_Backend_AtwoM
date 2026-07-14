package com.taskbackend.taskbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.request.RefreshTokenRequest;
import com.taskbackend.taskbackend.dto.request.RegisterRequest;
import com.taskbackend.taskbackend.dto.response.AccessTokenResponse;
import com.taskbackend.taskbackend.dto.response.LoginResponse;
import com.taskbackend.taskbackend.dto.response.UserResponse;
import com.taskbackend.taskbackend.exception.InvalidCredentialsException;
import com.taskbackend.taskbackend.exception.InvalidRefreshTokenException;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.exception.UsernameAlreadyExistsException;
import com.taskbackend.taskbackend.security.JwtAuthenticationFilter;
import com.taskbackend.taskbackend.service.AuthService;
import com.taskbackend.taskbackend.service.JwtAuthService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthService jwtAuthService;

    @Test
    void register_validRequest_returnsCreatedWithoutPassword() throws Exception {
        RegisterRequest input = new RegisterRequest("newuser", "secret1");
        UserResponse created = new UserResponse(1L, "newuser", "USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void register_blankUsername_returnsBadRequest() throws Exception {
        RegisterRequest input = new RegisterRequest("", "secret1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordTooShort_returnsBadRequest() throws Exception {
        RegisterRequest input = new RegisterRequest("newuser", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_usernameAlreadyExists_returnsConflict() throws Exception {
        RegisterRequest input = new RegisterRequest("existinguser", "secret1");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UsernameAlreadyExistsException("existinguser"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists: existinguser"));
    }

    @Test
    void login_validCredentials_returnsOkWithAccessAndRefreshToken() throws Exception {
        LoginRequest input = new LoginRequest("existinguser", "secret1");
        LoginResponse tokenResponse = new LoginResponse("dummy.jwt.token", "dummy.refresh.token", "Bearer", 900L);
        when(jwtAuthService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("dummy.jwt.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("dummy.refresh.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest input = new LoginRequest("existinguser", "wrongpassword");
        when(jwtAuthService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_blankUsername_returnsBadRequest() throws Exception {
        LoginRequest input = new LoginRequest("", "secret1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validRefreshToken_returnsOkWithNewAccessToken() throws Exception {
        RefreshTokenRequest input = new RefreshTokenRequest("valid-refresh-token");
        AccessTokenResponse response = new AccessTokenResponse("new.jwt.token", "Bearer", 900L);
        when(jwtAuthService.refreshAccessToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void refresh_invalidRefreshToken_returnsUnauthorized() throws Exception {
        RefreshTokenRequest input = new RefreshTokenRequest("garbage-token");
        when(jwtAuthService.refreshAccessToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void refresh_blankToken_returnsBadRequest() throws Exception {
        RefreshTokenRequest input = new RefreshTokenRequest("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_validRequest_returnsOk() throws Exception {
        RefreshTokenRequest input = new RefreshTokenRequest("some-refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(jwtAuthService).logout(input);
    }

    @Test
    void logout_blankToken_returnsBadRequest() throws Exception {
        RefreshTokenRequest input = new RefreshTokenRequest("");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_whenNotAuthenticated_returnsUnauthorized() throws Exception {
        RefreshTokenRequest input = new RefreshTokenRequest("some-refresh-token");
        doThrow(new UnauthorizedException()).when(jwtAuthService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
