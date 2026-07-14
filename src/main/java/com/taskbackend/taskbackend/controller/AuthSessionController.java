package com.taskbackend.taskbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.response.ApiResponse;
import com.taskbackend.taskbackend.dto.response.UserResponse;
import com.taskbackend.taskbackend.security.SessionUserResolver;
import com.taskbackend.taskbackend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/session")
public class AuthSessionController {

    private final AuthService authService;
    private final SessionUserResolver sessionUserResolver;

    public AuthSessionController(AuthService authService, SessionUserResolver sessionUserResolver) {
        this.authService = authService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        UserResponse user = authService.login(request);

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(SessionUserResolver.SESSION_USER_ID_ATTRIBUTE, user.id());

        return ResponseEntity.ok(ApiResponse.success("Login successful", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(HttpServletRequest servletRequest) {
        Long userId = sessionUserResolver.requireUserId(servletRequest);
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser(userId)));
    }
}
