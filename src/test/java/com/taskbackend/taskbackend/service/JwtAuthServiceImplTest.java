package com.taskbackend.taskbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.response.LoginResponse;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.InvalidCredentialsException;
import com.taskbackend.taskbackend.repository.UserRepository;
import com.taskbackend.taskbackend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class JwtAuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtAuthServiceImpl jwtAuthService;

    @Test
    void login_whenCredentialsValid_returnsAccessTokenFromJwtService() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded-secret1");
        user.setRole("USER");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("alice", "USER")).thenReturn("dummy.jwt.token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);

        LoginResponse result = jwtAuthService.login(new LoginRequest("alice", "secret1"));

        assertThat(result).isEqualTo(new LoginResponse("dummy.jwt.token", "Bearer", 900L));
    }

    @Test
    void login_whenAuthenticationManagerRejectsCredentials_throwsInvalidCredentialsException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> jwtAuthService.login(new LoginRequest("alice", "wrongpassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
