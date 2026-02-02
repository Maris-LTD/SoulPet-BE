package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.UpdateProfileRequestDTO;
import com.mystictarot.backend.dto.response.UserProfileResponseDTO;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.repository.ReadingRepository;
import com.mystictarot.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Following Given-When-Then pattern
 * 
 * Career Transition Note:
 * Trong Unity, bạn thường test game logic bằng cách mock các dependencies như Input System,
 * Scene Manager, hoặc Data Service. Ở đây, chúng ta mock UserRepository và ReadingRepository
 * để test profile logic mà không cần thực sự query database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReadingRepository readingRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";

        testUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(5)
                .avatarUrl("https://example.com/avatar.jpg")
                .subscriptionExpiry(null)
                .build();

        ReflectionTestUtils.setField(userService, "freePlanLimit", 3);
        ReflectionTestUtils.setField(userService, "monthlyPlanLimit", 20);
        ReflectionTestUtils.setField(userService, "retail5PlanLimit", 5);
    }

    @Test
    @DisplayName("Should get user profile successfully with usage statistics")
    void shouldGetUserProfile_SuccessfullyWithUsageStatistics() {
        // Given
        long weeklyReadingsUsed = 2L;
        long totalReadings = 10L;

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(weeklyReadingsUsed);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(totalReadings);

        // When
        UserProfileResponseDTO response = userService.getProfile(testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo(testEmail);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(response.getExtraCredits()).isEqualTo(5);
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(response.getWeeklyReadingsUsed()).isEqualTo(2);
        assertThat(response.getWeeklyReadingsLimit()).isEqualTo(3);
        assertThat(response.getTotalReadings()).isEqualTo(10L);

        verify(userRepository, times(1)).findById(testUserId);
        verify(readingRepository, times(1)).countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class));
        verify(readingRepository, times(1)).countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void shouldThrowResourceNotFoundException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getProfile(testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(testUserId);
        verify(readingRepository, never()).countWeeklyReadingsByUserId(any(), any(), any());
        verify(readingRepository, never()).countByUserIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Should calculate weekly readings limit correctly for FREE plan")
    void shouldCalculateWeeklyReadingsLimit_CorrectlyForFreePlan() {
        // Given
        testUser.setPlan(PlanType.FREE);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.getProfile(testUserId);

        // Then
        assertThat(response.getWeeklyReadingsLimit()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should calculate weekly readings limit correctly for MONTHLY plan")
    void shouldCalculateWeeklyReadingsLimit_CorrectlyForMonthlyPlan() {
        // Given
        testUser.setPlan(PlanType.MONTHLY);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.getProfile(testUserId);

        // Then
        assertThat(response.getWeeklyReadingsLimit()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should calculate weekly readings limit correctly for UNLIMITED plan")
    void shouldCalculateWeeklyReadingsLimit_CorrectlyForUnlimitedPlan() {
        // Given
        testUser.setPlan(PlanType.UNLIMITED);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.getProfile(testUserId);

        // Then
        assertThat(response.getWeeklyReadingsLimit()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should calculate weekly readings limit correctly for RETAIL_5 plan")
    void shouldCalculateWeeklyReadingsLimit_CorrectlyForRetail5Plan() {
        // Given
        testUser.setPlan(PlanType.RETAIL_5);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.getProfile(testUserId);

        // Then
        assertThat(response.getWeeklyReadingsLimit()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should update profile with name only")
    void shouldUpdateProfile_WithNameOnly() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("Updated Name")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Name");
        verify(userRepository, times(1)).save(argThat(user -> 
            "Updated Name".equals(user.getName()) && 
            user.getAvatarUrl().equals("https://example.com/avatar.jpg")));
    }

    @Test
    @DisplayName("Should update profile with avatarUrl only")
    void shouldUpdateProfile_WithAvatarUrlOnly() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
        verify(userRepository, times(1)).save(argThat(user -> 
            user.getAvatarUrl().equals("https://example.com/new-avatar.jpg") &&
            user.getName().equals("Test User")));
    }

    @Test
    @DisplayName("Should update profile with both name and avatarUrl")
    void shouldUpdateProfile_WithBothNameAndAvatarUrl() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("Updated Name")
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
        verify(userRepository, times(1)).save(argThat(user -> 
            "Updated Name".equals(user.getName()) &&
            user.getAvatarUrl().equals("https://example.com/new-avatar.jpg")));
    }

    @Test
    @DisplayName("Should trim whitespace from name when updating")
    void shouldTrimWhitespace_FromNameWhenUpdating() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("  Trimmed Name  ")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        userService.updateProfile(testUserId, request);

        // Then
        verify(userRepository, times(1)).save(argThat(user -> 
            "Trimmed Name".equals(user.getName())));
    }

    @Test
    @DisplayName("Should not update fields when request fields are null")
    void shouldNotUpdateFields_WhenRequestFieldsAreNull() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name(null)
                .avatarUrl(null)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        verify(userRepository, times(1)).save(argThat(user -> 
            "Test User".equals(user.getName()) &&
            user.getAvatarUrl().equals("https://example.com/avatar.jpg")));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
    void shouldThrowResourceNotFoundException_WhenUpdatingNonExistentUser() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("Updated Name")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not update name when name is empty string")
    void shouldNotUpdateName_WhenNameIsEmptyString() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name("   ")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        userService.updateProfile(testUserId, request);

        // Then
        verify(userRepository, times(1)).save(argThat(user -> 
            "Test User".equals(user.getName())));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when name exceeds 100 characters")
    void shouldThrowIllegalArgumentException_WhenNameExceeds100Characters() {
        // Given
        String longName = "a".repeat(101);
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name(longName)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name must not exceed 100 characters");

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when avatarUrl exceeds 500 characters")
    void shouldThrowIllegalArgumentException_WhenAvatarUrlExceeds500Characters() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(500);
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .avatarUrl(longUrl)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Avatar URL must not exceed 500 characters");

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when avatarUrl is not valid HTTP/HTTPS URL")
    void shouldThrowIllegalArgumentException_WhenAvatarUrlIsNotValidUrl() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .avatarUrl("invalid-url")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Avatar URL must be a valid HTTP/HTTPS URL");

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should allow null name and null avatarUrl in request")
    void shouldAllowNullNameAndNullAvatarUrl_InRequest() {
        // Given
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .name(null)
                .avatarUrl(null)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(readingRepository.countWeeklyReadingsByUserId(eq(testUserId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(readingRepository.countByUserIdAndStatus(testUserId, ReadingStatus.ACTIVE))
                .thenReturn(0L);

        // When
        UserProfileResponseDTO response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        verify(userRepository, times(1)).save(argThat(user -> 
            "Test User".equals(user.getName()) &&
            user.getAvatarUrl().equals("https://example.com/avatar.jpg")));
    }
}
