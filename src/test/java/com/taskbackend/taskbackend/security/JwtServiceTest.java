package com.taskbackend.taskbackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "unit-test-only-jwt-secret-key-must-be-at-least-32-bytes-long";
    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000L;

    private final JwtService jwtService = new JwtService(SECRET, ONE_DAY_MS);

    @Test
    void generateAccessToken_returnsWellFormedJwt() {
        String token = jwtService.generateAccessToken("alice", "USER");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_returnsSubjectUsedAtGeneration() {
        String token = jwtService.generateAccessToken("alice", "USER");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractExpiration_isApproximatelyNowPlusConfiguredLifetime() {
        String token = jwtService.generateAccessToken("alice", "USER");

        Date expiration = jwtService.extractExpiration(token);
        long expectedExpiration = System.currentTimeMillis() + ONE_DAY_MS;

        assertThat(expiration.getTime()).isCloseTo(expectedExpiration, org.assertj.core.data.Offset.offset(5000L));
    }

    @Test
    void isTokenExpired_forFreshToken_returnsFalse() {
        String token = jwtService.generateAccessToken("alice", "USER");

        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_forExpiredToken_returnsTrue() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000L);
        String expiredToken = shortLivedJwtService.generateAccessToken("alice", "USER");

        assertThat(shortLivedJwtService.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    void isTokenValid_forFreshTokenAndMatchingUsername_returnsTrue() {
        String token = jwtService.generateAccessToken("alice", "USER");

        assertThat(jwtService.isTokenValid(token, "alice")).isTrue();
    }

    @Test
    void isTokenValid_whenUsernameDoesNotMatch_returnsFalse() {
        String token = jwtService.generateAccessToken("alice", "USER");

        assertThat(jwtService.isTokenValid(token, "bob")).isFalse();
    }

    @Test
    void isTokenValid_whenTokenExpired_returnsFalse() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000L);
        String expiredToken = shortLivedJwtService.generateAccessToken("alice", "USER");

        assertThat(shortLivedJwtService.isTokenValid(expiredToken, "alice")).isFalse();
    }

    @Test
    void isTokenValid_whenTokenSignedWithDifferentSecret_returnsFalse() {
        JwtService otherJwtService = new JwtService("a-completely-different-jwt-secret-key-32-bytes-min", ONE_DAY_MS);
        String tokenFromOtherIssuer = otherJwtService.generateAccessToken("alice", "USER");

        assertThat(jwtService.isTokenValid(tokenFromOtherIssuer, "alice")).isFalse();
    }

    @Test
    void isTokenValid_whenTokenMalformed_returnsFalse() {
        assertThat(jwtService.isTokenValid("not-a-real-token", "alice")).isFalse();
    }

    @Test
    void extractUsername_whenTokenSignedWithDifferentSecret_throwsJwtException() {
        JwtService otherJwtService = new JwtService("a-completely-different-jwt-secret-key-32-bytes-min", ONE_DAY_MS);
        String tokenFromOtherIssuer = otherJwtService.generateAccessToken("alice", "USER");

        assertThatThrownBy(() -> jwtService.extractUsername(tokenFromOtherIssuer))
                .isInstanceOf(JwtException.class);
    }
}
