package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.LoginRequestDTO;
import com.mystictarot.backend.dto.request.RegisterRequestDTO;
import com.mystictarot.backend.dto.request.SocialLoginRequestDTO;
import com.mystictarot.backend.dto.response.AuthResponseDTO;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường test game logic bằng cách mock các dependencies như Input System,
 * Scene Manager, hoặc Data Service. Ở đây, chúng ta mock UserRepository và PasswordEncoder
 * để test authentication logic mà không cần thực sự lưu vào database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UUID testUserId;
    private String testEmail;
    private String testPassword;
    private String hashedPassword;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testPassword = "password123";
        hashedPassword = "$2a$10$hashedPassword123456789";
        testToken = "jwt.token.here";

        testUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(hashedPassword)
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(0)
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser_Successfully() {
        // Given
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .name("Test User")
                .build();

        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(testUserId, testEmail)).thenReturn(testToken);

        // When
        AuthResponseDTO response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(testToken);
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo(testEmail);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(response.getExtraCredits()).isEqualTo(0);

        verify(userRepository, times(1)).existsByEmail(testEmail);
        verify(passwordEncoder, times(1)).encode(testPassword);
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtTokenProvider, times(1)).generateToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void shouldThrowException_WhenRegisteringWithExistingEmail() {
        // Given
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .name("Test User")
                .build();

        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, times(1)).existsByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateToken(any(), anyString());
    }

    @Test
    @DisplayName("Should login user with valid credentials")
    void shouldLoginUser_WithValidCredentials() {
        // Given
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, hashedPassword)).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUserId, testEmail)).thenReturn(testToken);

        // When
        AuthResponseDTO response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(testToken);
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo(testEmail);

        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(passwordEncoder, times(1)).matches(testPassword, hashedPassword);
        verify(jwtTokenProvider, times(1)).generateToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when email not found")
    void shouldThrowBadCredentialsException_WhenEmailNotFound() {
        // Given
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any(), anyString());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password is incorrect")
    void shouldThrowBadCredentialsException_WhenPasswordIsIncorrect() {
        // Given
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email(testEmail)
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", hashedPassword)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(passwordEncoder, times(1)).matches("wrongPassword", hashedPassword);
        verify(jwtTokenProvider, never()).generateToken(any(), anyString());
    }

    @Test
    @DisplayName("Should create new user for social login when email not found")
    void shouldCreateNewUser_ForSocialLoginWhenEmailNotFound() {
        // Given
        SocialLoginRequestDTO request = SocialLoginRequestDTO.builder()
                .provider("google")
                .accessToken("accessToken123")
                .email(testEmail)
                .name("Social User")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(testUserId, testEmail)).thenReturn(testToken);

        // When
        AuthResponseDTO response = authService.socialLogin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(testToken);
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo(testEmail);

        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtTokenProvider, times(1)).generateToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should return existing user for social login when email found")
    void shouldReturnExistingUser_ForSocialLoginWhenEmailFound() {
        // Given
        SocialLoginRequestDTO request = SocialLoginRequestDTO.builder()
                .provider("facebook")
                .accessToken("accessToken123")
                .email(testEmail)
                .name("Social User")
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUserId, testEmail)).thenReturn(testToken);

        // When
        AuthResponseDTO response = authService.socialLogin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(testToken);
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo(testEmail);

        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, times(1)).generateToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should use default name when name is null in social login")
    void shouldUseDefaultName_WhenNameIsNullInSocialLogin() {
        // Given
        SocialLoginRequestDTO request = SocialLoginRequestDTO.builder()
                .provider("google")
                .accessToken("accessToken123")
                .email(testEmail)
                .name(null)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(testUserId);
            return savedUser;
        });
        when(jwtTokenProvider.generateToken(testUserId, testEmail)).thenReturn(testToken);

        // When
        AuthResponseDTO response = authService.socialLogin(request);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository, times(1)).save(argThat(user -> "User".equals(user.getName())));
    }

    @Test
    @DisplayName("Should set user plan to FREE when registering")
    void shouldSetUserPlanToFree_WhenRegistering() {
        // Given
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .name("Test User")
                .build();

        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(testUserId);
            return savedUser;
        });
        when(jwtTokenProvider.generateToken(testUserId, testEmail)).thenReturn(testToken);

        // When
        authService.register(request);

        // Then
        verify(userRepository, times(1)).save(argThat(user -> 
            user.getPlan() == PlanType.FREE && user.getExtraCredits() == 0));
    }
}
