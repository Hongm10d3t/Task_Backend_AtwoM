package com.taskbackend.taskbackend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.taskbackend.taskbackend.dto.request.RegisterRequest;
import com.taskbackend.taskbackend.dto.response.UserResponse;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.UsernameAlreadyExistsException;
import com.taskbackend.taskbackend.mapper.UserMapper;
import com.taskbackend.taskbackend.repository.UserRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(DEFAULT_ROLE);

        return UserMapper.toResponse(userRepository.save(user));
    }
}
