package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.UpdateProfileRequestDTO;
import com.mystictarot.backend.dto.response.UserProfileResponseDTO;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.repository.ReadingRepository;
import com.mystictarot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReadingRepository readingRepository;

    @Value("${subscription.plan.limits.free:3}")
    private Integer freePlanLimit;

    @Value("${subscription.plan.limits.monthly:20}")
    private Integer monthlyPlanLimit;

    @Value("${subscription.plan.limits.retail5:5}")
    private Integer retail5PlanLimit;

    /**
     * Get user profile with usage statistics
     * @param userId user ID
     * @return UserProfileResponseDTO with profile and usage stats
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfile(UUID userId) {
        log.debug("Getting profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new ResourceNotFoundException("User", userId);
                });

        UsageStats usageStats = calculateUsageStats(userId, user.getPlan());

        log.debug("User profile retrieved: userId={}, weeklyReadingsUsed={}, totalReadings={}, limit={}",
                userId, usageStats.weeklyReadingsUsed(), usageStats.totalReadings(), usageStats.weeklyReadingsLimit());

        return buildUserProfileResponse(user, usageStats);
    }

    /**
     * Update user profile (partial update)
     * @param userId user ID
     * @param request update request DTO
     * @return updated UserProfileResponseDTO
     */
    @Transactional
    public UserProfileResponseDTO updateProfile(UUID userId, UpdateProfileRequestDTO request) {
        log.debug("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for update: {}", userId);
                    return new ResourceNotFoundException("User", userId);
                });

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (!trimmedName.isEmpty()) {
                if (trimmedName.length() > 100) {
                    throw new IllegalArgumentException("Name must not exceed 100 characters");
                }
                user.setName(trimmedName);
                log.debug("Updated name for user: {}", userId);
            }
        }

        if (request.getAvatarUrl() != null) {
            String trimmedAvatarUrl = request.getAvatarUrl().trim();
            if (!trimmedAvatarUrl.isEmpty()) {
                if (trimmedAvatarUrl.length() > 500) {
                    throw new IllegalArgumentException("Avatar URL must not exceed 500 characters");
                }
                if (!trimmedAvatarUrl.matches("^(https?://).+")) {
                    throw new IllegalArgumentException("Avatar URL must be a valid HTTP/HTTPS URL");
                }
                user.setAvatarUrl(trimmedAvatarUrl);
                log.debug("Updated avatar URL for user: {}", userId);
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully: userId={}", userId);

        UsageStats usageStats = calculateUsageStats(userId, updatedUser.getPlan());

        return buildUserProfileResponse(updatedUser, usageStats);
    }

    /**
     * Calculate weekly readings limit based on plan type
     * @param plan plan type
     * @return weekly limit (-1 for unlimited)
     */
    private Integer calculateWeeklyReadingsLimit(PlanType plan) {
        return switch (plan) {
            case FREE -> freePlanLimit;
            case MONTHLY -> monthlyPlanLimit;
            case UNLIMITED -> -1;
            case RETAIL_5 -> retail5PlanLimit;
        };
    }

    /**
     * Get start of current week (Monday 00:00:00 UTC)
     * @return LocalDateTime representing Monday 00:00:00 UTC of current week
     */
    private LocalDateTime getStartOfCurrentWeek() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int daysToSubtract = dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue();
        
        if (daysToSubtract < 0) {
            daysToSubtract += 7;
        }
        
        return now.minusDays(daysToSubtract)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Calculate usage statistics for a user
     * @param userId user ID
     * @param plan user's plan type
     * @return UsageStats containing weekly readings used, limit, and total readings
     */
    private UsageStats calculateUsageStats(UUID userId, PlanType plan) {
        LocalDateTime startOfWeek = getStartOfCurrentWeek();
        long weeklyReadingsUsed = readingRepository.countWeeklyReadingsByUserId(userId, ReadingStatus.ACTIVE, startOfWeek);
        long totalReadings = readingRepository.countByUserIdAndStatus(userId, ReadingStatus.ACTIVE);
        Integer weeklyReadingsLimit = calculateWeeklyReadingsLimit(plan);
        return new UsageStats((int) weeklyReadingsUsed, weeklyReadingsLimit, totalReadings);
    }

    /**
     * Build UserProfileResponseDTO from User entity and usage stats
     */
    private UserProfileResponseDTO buildUserProfileResponse(User user, UsageStats usageStats) {
        return UserProfileResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .plan(user.getPlan())
                .extraCredits(user.getExtraCredits())
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .avatarUrl(user.getAvatarUrl())
                .weeklyReadingsUsed(usageStats.weeklyReadingsUsed())
                .weeklyReadingsLimit(usageStats.weeklyReadingsLimit())
                .totalReadings(usageStats.totalReadings())
                .build();
    }

    /**
     * Record class to hold usage statistics
     */
    private record UsageStats(int weeklyReadingsUsed, Integer weeklyReadingsLimit, long totalReadings) {}
}
