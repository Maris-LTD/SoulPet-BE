package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.ChatRole;
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
 * Unit tests for ChatMessage entity validation
 * Following Given-When-Then pattern
 */
@DisplayName("ChatMessage Entity Validation Tests")
class ChatMessageEntityTest {

    private Validator validator;
    private Reading testReading;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        User testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("Test User")
                .plan(PlanType.FREE)
                .build();

        testReading = Reading.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .question("Should I change my job?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .status(ReadingStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should create chat message with valid data")
    void shouldCreateChatMessage_WithValidData() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.USER)
                .content("Can you explain more about the first card?")
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).isEmpty();
        assertThat(chatMessage.getRole()).isEqualTo(ChatRole.USER);
        assertThat(chatMessage.getContent()).isEqualTo("Can you explain more about the first card?");
    }

    @Test
    @DisplayName("Should create AI chat message with valid data")
    void shouldCreateAIChatMessage_WithValidData() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.AI)
                .content("The first card represents your past situation...")
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).isEmpty();
        assertThat(chatMessage.getRole()).isEqualTo(ChatRole.AI);
    }

    @Test
    @DisplayName("Should fail validation when reading is null")
    void shouldFailValidation_WhenReadingIsNull() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .role(ChatRole.USER)
                .content("Can you explain more?")
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Reading is required");
    }

    @Test
    @DisplayName("Should fail validation when role is null")
    void shouldFailValidation_WhenRoleIsNull() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .content("Can you explain more?")
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Role is required");
    }

    @Test
    @DisplayName("Should fail validation when content is null")
    void shouldFailValidation_WhenContentIsNull() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.USER)
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Content is required");
    }

    @Test
    @DisplayName("Should fail validation when content is empty")
    void shouldFailValidation_WhenContentIsEmpty() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.USER)
                .content("")
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("Content must be between 1 and 10000 characters");
    }

    @Test
    @DisplayName("Should accept content with minimum length")
    void shouldAcceptContent_WithMinimumLength() {
        // Given
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.USER)
                .content("?")
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).isEmpty();
        assertThat(chatMessage.getContent()).isEqualTo("?");
    }

    @Test
    @DisplayName("Should accept content within max length")
    void shouldAcceptContent_WithinMaxLength() {
        // Given
        String longContent = "a".repeat(10000); // Exactly 10000 characters
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.USER)
                .content(longContent)
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when content exceeds max length")
    void shouldFailValidation_WhenContentExceedsMaxLength() {
        // Given
        String longContent = "a".repeat(10001); // > 10000 characters
        ChatMessage chatMessage = ChatMessage.builder()
                .reading(testReading)
                .role(ChatRole.USER)
                .content(longContent)
                .build();

        // When
        Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Content must be between 1 and 10000 characters");
    }
}
