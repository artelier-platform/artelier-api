
package com.artelier.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            "artelier-test-secret-key-must-be-at-least-32-characters-long";

    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(
                jwtUtil,
                "expirationMs",
                EXPIRATION_MS
        );
    }

    @Test
    void generateToken_ShouldGenerateValidToken() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_USER";

        // Act
        String token = jwtUtil.generateToken(username, role);

        // Assert
        assertThat(token)
                .isNotNull()
                .isNotBlank();

        assertThat(jwtUtil.validateToken(token))
                .isTrue();
    }

    @Test
    void generateToken_ShouldGenerateTokenWithCorrectUsername() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_ADMIN";

        // Act
        String token = jwtUtil.generateToken(username, role);
        String extractedUsername = jwtUtil.extractUsername(token);

        // Assert
        assertThat(extractedUsername)
                .isEqualTo(username);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Arrange
        String username = "admin@artelier.com";
        String token = jwtUtil.generateToken(username, "ROLE_ADMIN");

        // Act
        String result = jwtUtil.extractUsername(token);

        // Assert
        assertThat(result)
                .isEqualTo(username);
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsValid() {
        // Arrange
        String token = jwtUtil.generateToken(
                "testuser",
                "ROLE_USER"
        );

        // Act
        boolean result = jwtUtil.validateToken(token);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        // Arrange
        String token = "invalid.jwt.token";

        // Act
        boolean result = jwtUtil.validateToken(token);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsEmpty() {
        // Arrange
        String token = "";

        // Act
        boolean result = jwtUtil.validateToken(token);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsSignedWithDifferentSecret() {
        // Arrange
        JwtUtil anotherJwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                anotherJwtUtil,
                "secret",
                "another-test-secret-key-must-be-at-least-32-characters"
        );

        ReflectionTestUtils.setField(
                anotherJwtUtil,
                "expirationMs",
                EXPIRATION_MS
        );

        String token = anotherJwtUtil.generateToken(
                "testuser",
                "ROLE_USER"
        );

        // Act
        boolean result = jwtUtil.validateToken(token);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsExpired() {
        // Arrange
        JwtUtil expiredJwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                expiredJwtUtil,
                "secret",
                SECRET
        );

        ReflectionTestUtils.setField(
                expiredJwtUtil,
                "expirationMs",
                -1000L
        );

        String expiredToken = expiredJwtUtil.generateToken(
                "testuser",
                "ROLE_USER"
        );

        // Act
        boolean result = jwtUtil.validateToken(expiredToken);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void getExpirationMs_ShouldReturnConfiguredExpiration() {
        // Act
        long result = jwtUtil.getExpirationMs();

        // Assert
        assertThat(result)
                .isEqualTo(EXPIRATION_MS);
    }
}
