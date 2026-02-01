package com.mystictarot.backend.service;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomUserDetailsService
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường mock dependencies bằng NSubstitute hoặc Moq.
 * Ở đây, chúng ta dùng Mockito để mock UserRepository - tương tự như mock một
 * "DataService" trong Unity để test logic mà không cần thực sự query database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword123456789")
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(0)
                .build();
    }

    @Test
    @DisplayName("Should load user details when valid UUID is provided")
    void shouldLoadUserDetails_WhenValidUuidIsProvided() {
        // Given
        String userIdString = testUserId.toString();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(userIdString);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(testUserId.toString());
        assertThat(userDetails.getPassword()).isEqualTo(testUser.getPasswordHash());
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user is not found")
    void shouldThrowUsernameNotFoundException_WhenUserIsNotFound() {
        // Given
        String userIdString = testUserId.toString();
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(userIdString))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with id: " + userIdString);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when invalid UUID format is provided")
    void shouldThrowUsernameNotFoundException_WhenInvalidUuidFormatIsProvided() {
        // Given
        String invalidUuid = "invalid-uuid-format";

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(invalidUuid))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid user ID format: " + invalidUuid);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when empty string is provided")
    void shouldThrowUsernameNotFoundException_WhenEmptyStringIsProvided() {
        // Given
        String emptyString = "";

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(emptyString))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid user ID format");
    }

    @Test
    @DisplayName("Should load user details with correct authorities")
    void shouldLoadUserDetails_WithCorrectAuthorities() {
        // Given
        String userIdString = testUserId.toString();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(userIdString);

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should load user details with correct password hash")
    void shouldLoadUserDetails_WithCorrectPasswordHash() {
        // Given
        String userIdString = testUserId.toString();
        String expectedPasswordHash = "$2a$10$differentHashedPassword";
        testUser.setPasswordHash(expectedPasswordHash);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(userIdString);

        // Then
        assertThat(userDetails.getPassword()).isEqualTo(expectedPasswordHash);
    }

    @Test
    @DisplayName("Should handle different user IDs correctly")
    void shouldHandleDifferentUserIds_Correctly() {
        // Given
        UUID anotherUserId = UUID.randomUUID();
        User anotherUser = User.builder()
                .id(anotherUserId)
                .email("another@example.com")
                .passwordHash("$2a$10$anotherHashedPassword")
                .name("Another User")
                .plan(PlanType.MONTHLY)
                .extraCredits(5)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(anotherUserId)).thenReturn(Optional.of(anotherUser));

        // When
        UserDetails userDetails1 = userDetailsService.loadUserByUsername(testUserId.toString());
        UserDetails userDetails2 = userDetailsService.loadUserByUsername(anotherUserId.toString());

        // Then
        assertThat(userDetails1.getUsername()).isEqualTo(testUserId.toString());
        assertThat(userDetails2.getUsername()).isEqualTo(anotherUserId.toString());
        assertThat(userDetails1.getPassword()).isNotEqualTo(userDetails2.getPassword());
    }
}
