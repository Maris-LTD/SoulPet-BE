package com.mystictarot.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SecurityUtils
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường test static utility methods bằng cách test với các input khác nhau.
 * Ở đây, chúng ta test SecurityUtils bằng cách mock SecurityContext - tương tự như mock
 * một static service trong Unity để test game logic.
 */
@DisplayName("Security Utils Tests")
class SecurityUtilsTest {

    private SecurityContext securityContext;
    private Authentication authentication;
    private UUID testUserId;
    private String testUserIdString;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserIdString = testUserId.toString();
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should extract user ID successfully from authenticated user")
    void shouldExtractUserId_SuccessfullyFromAuthenticatedUser() {
        // Given
        UserDetails userDetails = mock(UserDetails.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(testUserIdString);

        // When
        UUID result = SecurityUtils.getCurrentUserId();

        // Then
        assertThat(result).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when authentication is null")
    void shouldThrowAuthenticationCredentialsNotFoundException_WhenAuthenticationIsNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> SecurityUtils.getCurrentUserId())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("User is not authenticated");
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not authenticated")
    void shouldThrowAuthenticationCredentialsNotFoundException_WhenUserIsNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> SecurityUtils.getCurrentUserId())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("User is not authenticated");
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when principal is not UserDetails")
    void shouldThrowAuthenticationCredentialsNotFoundException_WhenPrincipalIsNotUserDetails() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not a UserDetails");

        // When & Then
        assertThatThrownBy(() -> SecurityUtils.getCurrentUserId())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("Unable to extract user ID");
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user ID format is invalid")
    void shouldThrowAuthenticationCredentialsNotFoundException_WhenUserIdFormatIsInvalid() {
        // Given
        UserDetails userDetails = mock(UserDetails.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("invalid-uuid-format");

        // When & Then
        assertThatThrownBy(() -> SecurityUtils.getCurrentUserId())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("Invalid user ID format");
    }
}
