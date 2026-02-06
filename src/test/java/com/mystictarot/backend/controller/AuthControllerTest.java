package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.request.LoginRequestDTO;
import com.mystictarot.backend.dto.request.RegisterRequestDTO;
import com.mystictarot.backend.dto.request.SocialLoginRequestDTO;
import com.mystictarot.backend.dto.response.AuthResponseDTO;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthController
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường test UI/Input bằng cách simulate user interactions trong Play Mode.
 * Ở đây, chúng ta test controller logic bằng cách mock service và verify response - tương tự như
 * test một GameManager method mà không cần thực sự chạy game loop.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponseDTO mockAuthResponse;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        mockAuthResponse = AuthResponseDTO.builder()
                .token("jwt.token.here")
                .userId(testUserId)
                .email("test@example.com")
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(0)
                .build();
    }

    @Test
    @DisplayName("Should register user successfully and return 201 CREATED")
    void shouldRegisterUser_SuccessfullyAndReturn201Created() {
        // Given
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .build();

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockAuthResponse);

        // When
        ResponseEntity<AuthResponseDTO> response = authController.register(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should throw when email already exists (GlobalExceptionHandler returns 409)")
    void shouldThrow_WhenEmailAlreadyExists() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .build();

        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }


    @Test
    @DisplayName("Should login user successfully and return 200 OK")
    void shouldLoginUser_SuccessfullyAndReturn200Ok() {
        // Given
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockAuthResponse);

        // When
        ResponseEntity<AuthResponseDTO> response = authController.login(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login credentials are invalid")
    void shouldThrow_WhenLoginCredentialsAreInvalid() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should handle social login successfully and return 200 OK")
    void shouldHandleSocialLogin_SuccessfullyAndReturn200Ok() {
        // Given
        SocialLoginRequestDTO request = SocialLoginRequestDTO.builder()
                .provider("google")
                .accessToken("accessToken123")
                .email("test@example.com")
                .name("Social User")
                .build();

        when(authService.socialLogin(any(SocialLoginRequestDTO.class))).thenReturn(mockAuthResponse);

        // When
        ResponseEntity<AuthResponseDTO> response = authController.socialLogin(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw when social login service throws exception")
    void shouldThrow_WhenSocialLoginServiceThrowsException() {
        SocialLoginRequestDTO request = SocialLoginRequestDTO.builder()
                .provider("google")
                .accessToken("invalidToken")
                .email("test@example.com")
                .build();

        when(authService.socialLogin(any(SocialLoginRequestDTO.class)))
                .thenThrow(new RuntimeException("Invalid token"));

        assertThatThrownBy(() -> authController.socialLogin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid token");
    }
}
