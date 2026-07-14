package com.taskbackend.taskbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.request.RegisterRequest;
import com.taskbackend.taskbackend.dto.response.UserResponse;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.InvalidCredentialsException;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.exception.UsernameAlreadyExistsException;
import com.taskbackend.taskbackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_whenUsernameNotTaken_savesEncodedPasswordWithDefaultRole() {
        RegisterRequest request = new RegisterRequest("newuser", "secret1");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded-secret1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse result = authService.register(request);

        assertThat(result).isEqualTo(new UserResponse(1L, "newuser", "USER"));
        verify(userRepository).save(argThatSavedUserHasEncodedPassword());
    }

    @Test
    void register_whenUsernameAlreadyExists_throwsUsernameAlreadyExistsExceptionAndDoesNotSave() {
        RegisterRequest request = new RegisterRequest("existinguser", "secret1");
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("existinguser");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_whenCredentialsValid_returnsUserResponse() {
        User existing = userWithId(1L, "existinguser", "encoded-secret1", "USER");
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret1", "encoded-secret1")).thenReturn(true);

        UserResponse result = authService.login(new LoginRequest("existinguser", "secret1"));

        assertThat(result).isEqualTo(new UserResponse(1L, "existinguser", "USER"));
    }

    @Test
    void login_whenUsernameNotFound_throwsInvalidCredentialsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "secret1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsInvalidCredentialsException() {
        User existing = userWithId(1L, "existinguser", "encoded-secret1", "USER");
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrongpassword", "encoded-secret1")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("existinguser", "wrongpassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getCurrentUser_whenUserExists_returnsUserResponse() {
        User existing = userWithId(1L, "existinguser", "encoded-secret1", "USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserResponse result = authService.getCurrentUser(1L);

        assertThat(result).isEqualTo(new UserResponse(1L, "existinguser", "USER"));
    }

    @Test
    void getCurrentUser_whenUserMissing_throwsUnauthorizedException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    private User userWithId(Long id, String username, String password, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }

    private User argThatSavedUserHasEncodedPassword() {
        return org.mockito.ArgumentMatchers.argThat(user ->
                user.getUsername().equals("newuser")
                        && user.getPassword().equals("encoded-secret1")
                        && user.getRole().equals("USER"));
    }
}
