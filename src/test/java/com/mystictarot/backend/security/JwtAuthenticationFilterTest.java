package com.mystictarot.backend.security;

import com.mystictarot.backend.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường test Input System hoặc Event System bằng cách mock các input events.
 * Ở đây, chúng ta mock HttpServletRequest và FilterChain - tương tự như mock một "InputHandler"
 * để test xem filter có xử lý request đúng cách không.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private String validToken;
    private UUID testUserId;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUserId = UUID.randomUUID();
        validToken = "valid.jwt.token";
        testUserDetails = User.builder()
                .username(testUserId.toString())
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Should set authentication when valid token is provided")
    void shouldSetAuthentication_WhenValidTokenIsProvided() throws ServletException, IOException {
        // Given
        String bearerToken = "Bearer " + validToken;
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(userDetailsService.loadUserByUsername(testUserId.toString())).thenReturn(testUserDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(testUserDetails);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when token is missing")
    void shouldNotSetAuthentication_WhenTokenIsMissing() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when Authorization header is empty")
    void shouldNotSetAuthentication_WhenAuthorizationHeaderIsEmpty() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when token is invalid")
    void shouldNotSetAuthentication_WhenTokenIsInvalid() throws ServletException, IOException {
        // Given
        String bearerToken = "Bearer " + validToken;
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(validToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, times(1)).validateToken(validToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when Authorization header does not start with Bearer")
    void shouldNotSetAuthentication_WhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Token " + validToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract token correctly from Bearer header")
    void shouldExtractToken_CorrectlyFromBearerHeader() throws ServletException, IOException {
        // Given
        String token = "extracted.token.here";
        String bearerToken = "Bearer " + token;
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(testUserId);
        when(userDetailsService.loadUserByUsername(testUserId.toString())).thenReturn(testUserDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider, times(1)).validateToken(token);
        verify(tokenProvider, times(1)).getUserIdFromToken(token);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain even when exception occurs")
    void shouldContinueFilterChain_EvenWhenExceptionOccurs() throws ServletException, IOException {
        // Given
        String bearerToken = "Bearer " + validToken;
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(validToken)).thenThrow(new RuntimeException("Token validation error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should not set authentication when user is not found")
    void shouldNotSetAuthentication_WhenUserIsNotFound() throws ServletException, IOException {
        // Given
        String bearerToken = "Bearer " + validToken;
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(userDetailsService.loadUserByUsername(testUserId.toString()))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle token with spaces correctly")
    void shouldHandleToken_WithSpacesCorrectly() throws ServletException, IOException {
        // Given
        String token = "token with spaces";
        String bearerToken = "Bearer " + token;
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(testUserId);
        when(userDetailsService.loadUserByUsername(testUserId.toString())).thenReturn(testUserDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider, times(1)).validateToken(token);
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
