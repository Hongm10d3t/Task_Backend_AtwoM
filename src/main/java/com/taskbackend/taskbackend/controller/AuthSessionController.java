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
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/session")
public class AuthSessionController {

    private static final String SESSION_USER_ID_ATTRIBUTE = "USER_ID";

    private final AuthService authService;

    public AuthSessionController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        UserResponse user = authService.login(request);

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(SESSION_USER_ID_ATTRIBUTE, user.id());
        System.out.println(">>>>>> hahah" + session.getAttribute(SESSION_USER_ID_ATTRIBUTE));

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
        HttpSession session = servletRequest.getSession(false);
        Long userId = session != null ? (Long) session.getAttribute(SESSION_USER_ID_ATTRIBUTE) : null;

        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser(userId)));
    }
}
