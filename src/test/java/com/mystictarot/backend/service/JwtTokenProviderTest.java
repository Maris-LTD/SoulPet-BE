package com.mystictarot.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtTokenProvider service
 * Following Given-When-Then pattern
 * 
 * Career Transition Note: 
 * Trong Unity, bạn thường test logic bằng Play Mode Tests hoặc Unit Tests với NUnit.
 * Ở đây, chúng ta test JWT token generation/validation - tương tự như test việc tạo và verify
 * một "session key" trong game multiplayer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "testSecretKeyForJwtTokenProviderUnitTesting123456789";
    private static final Long TEST_EXPIRATION = 86400000L; // 24 hours
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", TEST_EXPIRATION);
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
    }

    @Test
    @DisplayName("Should generate valid JWT token with userId and email")
    void shouldGenerateValidToken_WithUserIdAndEmail() {
        // Given
        UUID userId = testUserId;
        String email = testEmail;

        // When
        String token = jwtTokenProvider.generateToken(userId, email);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract userId from valid token")
    void shouldExtractUserId_FromValidToken() {
        // Given
        UUID expectedUserId = testUserId;
        String token = jwtTokenProvider.generateToken(expectedUserId, testEmail);

        // When
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("Should extract email from valid token")
    void shouldExtractEmail_FromValidToken() {
        // Given
        String expectedEmail = testEmail;
        String token = jwtTokenProvider.generateToken(testUserId, expectedEmail);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(extractedEmail).isEqualTo(expectedEmail);
    }

    @Test
    @DisplayName("Should extract expiration date from valid token")
    void shouldExtractExpirationDate_FromValidToken() {
        // Given
        String token = jwtTokenProvider.generateToken(testUserId, testEmail);
        Date now = new Date();
        Date expectedExpiration = new Date(now.getTime() + TEST_EXPIRATION);

        // When
        Date extractedExpiration = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertThat(extractedExpiration).isNotNull();
        assertThat(extractedExpiration.getTime()).isBetween(
                expectedExpiration.getTime() - 1000L, 
                expectedExpiration.getTime() + 1000L); // Allow 1 second tolerance
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidate_ValidToken() {
        // Given
        String token = jwtTokenProvider.generateToken(testUserId, testEmail);

        // When
        Boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate expired token")
    void shouldInvalidate_ExpiredToken() throws InterruptedException {
        // Given
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 100L); // 100ms expiration
        String token = jwtTokenProvider.generateToken(testUserId, testEmail);
        Thread.sleep(200); // Wait for token to expire

        // When
        Boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate malformed token")
    void shouldInvalidate_MalformedToken() {
        // Given
        String malformedToken = "invalid.token.string";

        // When
        Boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate empty token")
    void shouldInvalidate_EmptyToken() {
        // Given
        String emptyToken = "";

        // When
        Boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate null token")
    void shouldInvalidate_NullToken() {
        // Given
        String nullToken = null;

        // When
        Boolean isValid = jwtTokenProvider.validateToken(nullToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate token with wrong secret")
    void shouldInvalidate_TokenWithWrongSecret() {
        // Given
        String token = jwtTokenProvider.generateToken(testUserId, testEmail);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "differentSecretKey123456789");

        // When
        Boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void shouldGenerateDifferentTokens_ForSameUserAtDifferentTimes() throws InterruptedException {
        // Given
        UUID userId = testUserId;
        String email = testEmail;

        // When
        String token1 = jwtTokenProvider.generateToken(userId, email);
        Thread.sleep(1000); // Wait 1 second
        String token2 = jwtTokenProvider.generateToken(userId, email);

        // Then
        assertThat(token1).isNotEqualTo(token2); // Different issuedAt times
        assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(jwtTokenProvider.getUserIdFromToken(token2));
        assertThat(jwtTokenProvider.getEmailFromToken(token1)).isEqualTo(jwtTokenProvider.getEmailFromToken(token2));
    }

    @Test
    @DisplayName("Should extract claims correctly from token")
    void shouldExtractClaims_CorrectlyFromToken() {
        // Given
        UUID userId = testUserId;
        String email = testEmail;
        String token = jwtTokenProvider.generateToken(userId, email);

        // When
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(expiration).isAfter(new Date());
    }
}
