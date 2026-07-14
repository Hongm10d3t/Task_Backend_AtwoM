package com.taskbackend.taskbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.request.RefreshTokenRequest;
import com.taskbackend.taskbackend.dto.response.AccessTokenResponse;
import com.taskbackend.taskbackend.dto.response.LoginResponse;
import com.taskbackend.taskbackend.entity.RefreshToken;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.InvalidCredentialsException;
import com.taskbackend.taskbackend.exception.InvalidRefreshTokenException;
import com.taskbackend.taskbackend.repository.UserRepository;
import com.taskbackend.taskbackend.security.CurrentUserService;
import com.taskbackend.taskbackend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class JwtAuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private JwtAuthServiceImpl jwtAuthService;

    private User userWithId(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded-secret1");
        user.setRole(role);
        return user;
    }

    @Test
    void login_whenCredentialsValid_returnsAccessAndRefreshToken() {
        User user = userWithId(1L, "alice", "USER");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("generated-refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("alice", "USER")).thenReturn("dummy.jwt.token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        LoginResponse result = jwtAuthService.login(new LoginRequest("alice", "secret1"));

        assertThat(result).isEqualTo(new LoginResponse("dummy.jwt.token", "generated-refresh-token", "Bearer", 900L));
    }

    @Test
    void login_whenAuthenticationManagerRejectsCredentials_throwsInvalidCredentialsException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> jwtAuthService.login(new LoginRequest("alice", "wrongpassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshAccessToken_whenRefreshTokenValid_returnsNewAccessToken() {
        User user = userWithId(1L, "alice", "USER");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));

        when(refreshTokenService.validateAndGet("valid-refresh-token")).thenReturn(refreshToken);
        when(jwtService.generateAccessToken("alice", "USER")).thenReturn("new.jwt.token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);

        AccessTokenResponse result = jwtAuthService.refreshAccessToken(new RefreshTokenRequest("valid-refresh-token"));

        assertThat(result).isEqualTo(new AccessTokenResponse("new.jwt.token", "Bearer", 900L));
    }

    @Test
    void refreshAccessToken_whenRefreshTokenInvalid_propagatesInvalidRefreshTokenException() {
        when(refreshTokenService.validateAndGet("bad-token")).thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> jwtAuthService.refreshAccessToken(new RefreshTokenRequest("bad-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logout_revokesRefreshTokenOwnedByCurrentUser() {
        User user = userWithId(1L, "alice", "USER");
        when(currentUserService.getCurrentUser()).thenReturn(user);

        jwtAuthService.logout(new RefreshTokenRequest("some-refresh-token"));

        verify(refreshTokenService).revokeOwnedByUser("some-refresh-token", user);
    }
}
