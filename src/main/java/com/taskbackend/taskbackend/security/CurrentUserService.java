package com.taskbackend.taskbackend.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.UnauthorizedException;
import com.taskbackend.taskbackend.repository.UserRepository;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException();
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(UnauthorizedException::new);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
