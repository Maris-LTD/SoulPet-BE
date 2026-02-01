package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.PlanType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for User entity validation
 * Following Given-When-Then pattern
 */
@DisplayName("User Entity Validation Tests")
class UserEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create user with valid data")
    void shouldCreateUser_WithValidData() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(0)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).isEmpty();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("Test User");
        assertThat(user.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(user.getExtraCredits()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should fail validation when email is null")
    void shouldFailValidation_WhenEmailIsNull() {
        // Given
        User user = User.builder()
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Email is required");
    }

    @Test
    @DisplayName("Should fail validation when email is invalid")
    void shouldFailValidation_WhenEmailIsInvalid() {
        // Given
        User user = User.builder()
                .email("invalid-email")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Email should be valid");
    }

    @Test
    @DisplayName("Should fail validation when email exceeds max length")
    void shouldFailValidation_WhenEmailExceedsMaxLength() {
        // Given
        String longEmail = "a".repeat(250) + "@example.com"; // > 255 characters
        User user = User.builder()
                .email(longEmail)
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Email must not exceed 255 characters");
    }

    @Test
    @DisplayName("Should fail validation when name is null")
    void shouldFailValidation_WhenNameIsNull() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Name is required");
    }

    @Test
    @DisplayName("Should fail validation when name is empty")
    void shouldFailValidation_WhenNameIsEmpty() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Name must be between 1 and 100 characters");
    }

    @Test
    @DisplayName("Should fail validation when name exceeds max length")
    void shouldFailValidation_WhenNameExceedsMaxLength() {
        // Given
        String longName = "a".repeat(101); // > 100 characters
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name(longName)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Name must be between 1 and 100 characters");
    }

    @Test
    @DisplayName("Should fail validation when extra credits is negative")
    void shouldFailValidation_WhenExtraCreditsIsNegative() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .extraCredits(-1)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Extra credits must be non-negative");
    }

    @Test
    @DisplayName("Should set default plan when plan is not provided")
    void shouldSetDefaultPlan_WhenPlanIsNotProvided() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .build();

        // When
        // Then
        assertThat(user.getPlan()).isEqualTo(PlanType.FREE);
    }

    @Test
    @DisplayName("Should set default extra credits when extra credits is not provided")
    void shouldSetDefaultExtraCredits_WhenExtraCreditsIsNotProvided() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .build();

        // When
        // Then
        assertThat(user.getExtraCredits()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should allow null subscription expiry for FREE plan")
    void shouldAllowNullSubscriptionExpiry_ForFreePlan() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .plan(PlanType.FREE)
                .subscriptionExpiry(null)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).isEmpty();
        assertThat(user.getSubscriptionExpiry()).isNull();
    }

    @Test
    @DisplayName("Should allow null avatar URL")
    void shouldAllowNullAvatarUrl() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .avatarUrl(null)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).isEmpty();
        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("Should fail validation when avatar URL exceeds max length")
    void shouldFailValidation_WhenAvatarUrlExceedsMaxLength() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(500); // > 500 characters
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .avatarUrl(longUrl)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Avatar URL must not exceed 500 characters");
    }
}
