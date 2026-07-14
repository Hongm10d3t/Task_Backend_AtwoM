package com.taskbackend.taskbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskbackend.taskbackend.entity.RefreshToken;
import com.taskbackend.taskbackend.entity.User;
import com.taskbackend.taskbackend.exception.InvalidRefreshTokenException;
import com.taskbackend.taskbackend.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, SEVEN_DAYS_MS);
    }

    private RefreshToken tokenWith(String token, User user, Instant expiresAt, boolean revoked) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(revoked);
        return refreshToken;
    }

    @Test
    void createRefreshToken_savesTokenWithFutureExpiryAndAssociatedUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        assertThat(result.getExpiresAt()).isCloseTo(Instant.now().plusMillis(SEVEN_DAYS_MS),
                org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void createRefreshToken_generatesDifferentTokensOnEachCall() {
        User user = new User();
        user.setId(1L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken first = refreshTokenService.createRefreshToken(user);
        RefreshToken second = refreshTokenService.createRefreshToken(user);

        assertThat(first.getToken()).isNotEqualTo(second.getToken());
    }

    @Test
    void validateAndGet_whenTokenValidAndNotExpiredNorRevoked_returnsToken() {
        User user = new User();
        user.setId(1L);
        RefreshToken token = tokenWith("valid-token", user, Instant.now().plusSeconds(3600), false);
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validateAndGet("valid-token");

        assertThat(result).isEqualTo(token);
    }

    @Test
    void validateAndGet_whenTokenNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndGet("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndGet_whenTokenRevoked_throwsInvalidRefreshTokenException() {
        User user = new User();
        user.setId(1L);
        RefreshToken token = tokenWith("revoked-token", user, Instant.now().plusSeconds(3600), true);
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndGet("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndGet_whenTokenExpired_throwsInvalidRefreshTokenException() {
        User user = new User();
        user.setId(1L);
        RefreshToken token = tokenWith("expired-token", user, Instant.now().minusSeconds(1), false);
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndGet("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeOwnedByUser_whenTokenOwnedByUser_marksRevokedAndSaves() {
        User user = new User();
        user.setId(1L);
        RefreshToken token = tokenWith("owned-token", user, Instant.now().plusSeconds(3600), false);
        when(refreshTokenRepository.findByToken("owned-token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeOwnedByUser("owned-token", user);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeOwnedByUser_whenTokenAlreadyRevoked_isIdempotentAndDoesNotThrow() {
        User user = new User();
        user.setId(1L);
        RefreshToken token = tokenWith("already-revoked-token", user, Instant.now().plusSeconds(3600), true);
        when(refreshTokenRepository.findByToken("already-revoked-token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeOwnedByUser("already-revoked-token", user);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeOwnedByUser_whenTokenBelongsToAnotherUser_doesNothingAndDoesNotThrow() {
        User owner = new User();
        owner.setId(1L);
        User attacker = new User();
        attacker.setId(2L);
        RefreshToken token = tokenWith("someone-elses-token", owner, Instant.now().plusSeconds(3600), false);
        when(refreshTokenRepository.findByToken("someone-elses-token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeOwnedByUser("someone-elses-token", attacker);

        assertThat(token.isRevoked()).isFalse();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeOwnedByUser_whenTokenNotFound_doesNothingAndDoesNotThrow() {
        User user = new User();
        user.setId(1L);
        when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        refreshTokenService.revokeOwnedByUser("missing-token", user);

        verify(refreshTokenRepository, never()).save(any());
    }
}
