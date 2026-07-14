package com.taskbackend.taskbackend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskbackend.taskbackend.entity.RefreshToken;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.InvalidRefreshTokenException;
import com.taskbackend.taskbackend.repository.RefreshTokenRepository;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateSecureToken());
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeOwnedByUser(String token, User user) {
        // Idempotent by design: a missing token, a token owned by someone else, or an
        // already-revoked token are all silently no-ops so repeated/invalid logout calls
        // never fail with an error - they just don't change anything.
        refreshTokenRepository.findByToken(token)
                .filter(refreshToken -> refreshToken.getUser().getId().equals(user.getId()))
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
