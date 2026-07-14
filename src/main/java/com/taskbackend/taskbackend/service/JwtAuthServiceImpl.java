package com.taskbackend.taskbackend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskbackend.taskbackend.dto.request.LoginRequest;
import com.taskbackend.taskbackend.dto.request.RefreshTokenRequest;
import com.taskbackend.taskbackend.dto.response.AccessTokenResponse;
import com.taskbackend.taskbackend.dto.response.LoginResponse;
import com.taskbackend.taskbackend.entity.RefreshToken;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.InvalidCredentialsException;
import com.taskbackend.taskbackend.repository.UserRepository;
import com.taskbackend.taskbackend.security.CurrentUserService;
import com.taskbackend.taskbackend.security.JwtService;

@Service
public class JwtAuthServiceImpl implements JwtAuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserService currentUserService;

    public JwtAuthServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService,
            UserRepository userRepository, RefreshTokenService refreshTokenService,
            CurrentUserService currentUserService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        long expiresInSeconds = jwtService.getAccessTokenExpirationMs() / 1000;

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken.getToken(), TOKEN_TYPE, expiresInSeconds);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateAndGet(request.refreshToken());
        User user = refreshToken.getUser();

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        long expiresInSeconds = jwtService.getAccessTokenExpirationMs() / 1000;

        return new AccessTokenResponse(accessToken, TOKEN_TYPE, expiresInSeconds);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        // Only revokes the refresh token; any access token already issued to this user
        // remains usable until it naturally expires, since access tokens are not blacklisted.
        User currentUser = currentUserService.getCurrentUser();
        refreshTokenService.revokeOwnedByUser(request.refreshToken(), currentUser);
    }
}
