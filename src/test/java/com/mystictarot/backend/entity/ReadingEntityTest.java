package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.entity.enums.SpreadType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Reading entity validation
 * Following Given-When-Then pattern
 */
@DisplayName("Reading Entity Validation Tests")
class ReadingEntityTest {

    private Validator validator;
    private User testUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .plan(PlanType.FREE)
                .build();
    }

    @Test
    @DisplayName("Should create reading with valid data")
    void shouldCreateReading_WithValidData() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).isEmpty();
        assertThat(reading.getQuestion()).isEqualTo("Should I change my job?");
        assertThat(reading.getSpreadType()).isEqualTo(SpreadType.THREE_CARDS);
        assertThat(reading.getStatus()).isEqualTo(ReadingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should fail validation when user is null")
    void shouldFailValidation_WhenUserIsNull() {
        // Given
        Reading reading = Reading.builder()
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("User is required");
    }

    @Test
    @DisplayName("Should fail validation when question is null")
    void shouldFailValidation_WhenQuestionIsNull() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Question is required");
    }

    @Test
    @DisplayName("Should fail validation when question is empty")
    void shouldFailValidation_WhenQuestionIsEmpty() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Question must be between 1 and 1000 characters");
    }

    @Test
    @DisplayName("Should fail validation when question exceeds max length")
    void shouldFailValidation_WhenQuestionExceedsMaxLength() {
        // Given
        String longQuestion = "a".repeat(1001); // > 1000 characters
        Reading reading = Reading.builder()
                .user(testUser)
                .question(longQuestion)
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Question must be between 1 and 1000 characters");
    }

    @Test
    @DisplayName("Should fail validation when spread type is null")
    void shouldFailValidation_WhenSpreadTypeIsNull() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("Should I change my job?")
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Spread type is required");
    }

    @Test
    @DisplayName("Should fail validation when cards JSON is null")
    void shouldFailValidation_WhenCardsJsonIsNull() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Cards JSON is required");
    }

    @Test
    @DisplayName("Should set default status when status is not provided")
    void shouldSetDefaultStatus_WhenStatusIsNotProvided() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .build();

        // When
        // Then
        assertThat(reading.getStatus()).isEqualTo(ReadingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should allow null interpretation text when reading is created")
    void shouldAllowNullInterpretationText_WhenReadingIsCreated() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .interpretationText(null)
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).isEmpty();
        assertThat(reading.getInterpretationText()).isNull();
    }

    @Test
    @DisplayName("Should allow null deleted at when reading is active")
    void shouldAllowNullDeletedAt_WhenReadingIsActive() {
        // Given
        Reading reading = Reading.builder()
                .user(testUser)
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .status(ReadingStatus.ACTIVE)
                .deletedAt(null)
                .build();

        // When
        Set<ConstraintViolation<Reading>> violations = validator.validate(reading);

        // Then
        assertThat(violations).isEmpty();
        assertThat(reading.getDeletedAt()).isNull();
    }
}
