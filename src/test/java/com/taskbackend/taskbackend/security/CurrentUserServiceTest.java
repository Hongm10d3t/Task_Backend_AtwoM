package com.taskbackend.taskbackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_whenAuthenticated_returnsUserFromRepository() {
        User user = new User();
        user.setId(42L);
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        User result = currentUserService.getCurrentUser();

        assertThat(result.getId()).isEqualTo(42L);
    }

    @Test
    void getCurrentUserId_whenAuthenticated_returnsUserId() {
        User user = new User();
        user.setId(42L);
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(currentUserService.getCurrentUserId()).isEqualTo(42L);
    }

    @Test
    void getCurrentUser_whenNoAuthentication_throwsUnauthorizedException() {
        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_whenAnonymousAuthentication_throwsUnauthorizedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_whenAuthenticatedUsernameNotInRepository_throwsUnauthorizedException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }
}
