package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.request.UpdateProfileRequestDTO;
import com.mystictarot.backend.dto.response.UserProfileResponseDTO;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.service.UserService;
import com.mystictarot.backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường test UI/Input bằng cách simulate user interactions trong Play Mode.
 * Ở đây, chúng ta test controller logic bằng cách mock service và SecurityUtils - tương tự như
 * test một GameManager method mà không cần thực sự chạy game loop.
 * 
 * Note: These are unit tests without Spring context. For integration tests with GlobalExceptionHandler,
 * use @WebMvcTest or @SpringBootTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UUID testUserId;
    private UserProfileResponseDTO mockProfileResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        mockProfileResponse = UserProfileResponseDTO.builder()
                .userId(testUserId)
                .email("test@example.com")
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(5)
                .avatarUrl("https://example.com/avatar.jpg")
                .weeklyReadingsUsed(2)
                .weeklyReadingsLimit(3)
                .totalReadings(10L)
                .build();
    }

    @Test
    @DisplayName("Should get user profile successfully and return 200 OK")
    void shouldGetUserProfile_SuccessfullyAndReturn200Ok() {
        // Given
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(userService.getProfile(testUserId)).thenReturn(mockProfileResponse);

            // When
            ResponseEntity<UserProfileResponseDTO> response = userController.getProfile();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
            assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
            assertThat(response.getBody().getName()).isEqualTo("Test User");
            assertThat(response.getBody().getWeeklyReadingsUsed()).isEqualTo(2);
            assertThat(response.getBody().getWeeklyReadingsLimit()).isEqualTo(3);
            assertThat(response.getBody().getTotalReadings()).isEqualTo(10L);

            verify(userService, times(1)).getProfile(testUserId);
        }
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void shouldThrowResourceNotFoundException_WhenUserNotFound() {
        // Given
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(userService.getProfile(testUserId))
                    .thenThrow(new ResourceNotFoundException("User", testUserId));

            // When & Then
            assertThatThrownBy(() -> userController.getProfile())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(userService, times(1)).getProfile(testUserId);
        }
    }

    @Test
    @DisplayName("Should update user profile successfully and return 200 OK")
    void shouldUpdateUserProfile_SuccessfullyAndReturn200Ok() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("Updated Name")
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

        UserProfileResponseDTO updatedResponse = UserProfileResponseDTO.builder()
                .userId(testUserId)
                .email("test@example.com")
                .name("Updated Name")
                .plan(PlanType.FREE)
                .extraCredits(5)
                .avatarUrl("https://example.com/new-avatar.jpg")
                .weeklyReadingsUsed(2)
                .weeklyReadingsLimit(3)
                .totalReadings(10L)
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class)))
                    .thenReturn(updatedResponse);

            // When
            ResponseEntity<UserProfileResponseDTO> response = userController.updateProfile(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("Updated Name");
            assertThat(response.getBody().getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");

            verify(userService, times(1)).updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class));
        }
    }

    @Test
    @DisplayName("Should update profile with name only")
    void shouldUpdateProfile_WithNameOnly() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("Updated Name")
                .build();

        UserProfileResponseDTO updatedResponse = UserProfileResponseDTO.builder()
                .userId(testUserId)
                .name("Updated Name")
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class)))
                    .thenReturn(updatedResponse);

            // When
            ResponseEntity<UserProfileResponseDTO> response = userController.updateProfile(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getName()).isEqualTo("Updated Name");

            verify(userService, times(1)).updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class));
        }
    }

    @Test
    @DisplayName("Should update profile with avatarUrl only")
    void shouldUpdateProfile_WithAvatarUrlOnly() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

        UserProfileResponseDTO updatedResponse = UserProfileResponseDTO.builder()
                .userId(testUserId)
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class)))
                    .thenReturn(updatedResponse);

            // When
            ResponseEntity<UserProfileResponseDTO> response = userController.updateProfile(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");

            verify(userService, times(1)).updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class));
        }
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
    void shouldThrowResourceNotFoundException_WhenUpdatingNonExistentUser() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("Updated Name")
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("User", testUserId));

            // When & Then
            assertThatThrownBy(() -> userController.updateProfile(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(userService, times(1)).updateProfile(eq(testUserId), any(UpdateProfileRequestDTO.class));
        }
    }
}
