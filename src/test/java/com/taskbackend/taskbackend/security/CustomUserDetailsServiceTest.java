package com.taskbackend.taskbackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetailsWithEncodedPasswordAndRole() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded-password");
        user.setRole("USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_whenUserNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
