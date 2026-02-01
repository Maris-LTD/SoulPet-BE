package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.SuitType;
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
 * Unit tests for TarotCard entity validation
 * Following Given-When-Then pattern
 */
@DisplayName("TarotCard Entity Validation Tests")
class TarotCardEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create tarot card with valid data")
    void shouldCreateTarotCard_WithValidData() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .description("Represents new beginnings, innocence, and spontaneity")
                .imageUrl("https://example.com/cards/fool.jpg")
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).isEmpty();
        assertThat(tarotCard.getName()).isEqualTo("The Fool");
        assertThat(tarotCard.getSuit()).isEqualTo(SuitType.MAJOR_ARCANA);
        assertThat(tarotCard.getCardNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should create tarot card with minimal required data")
    void shouldCreateTarotCard_WithMinimalRequiredData() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).isEmpty();
        assertThat(tarotCard.getDescription()).isNull();
        assertThat(tarotCard.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should fail validation when id is null")
    void shouldFailValidation_WhenIdIsNull() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .name("The Fool")
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Card ID is required");
    }

    @Test
    @DisplayName("Should fail validation when name is null")
    void shouldFailValidation_WhenNameIsNull() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Card name is required");
    }

    @Test
    @DisplayName("Should fail validation when name exceeds max length")
    void shouldFailValidation_WhenNameExceedsMaxLength() {
        // Given
        String longName = "a".repeat(101); // > 100 characters
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name(longName)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Card name must not exceed 100 characters");
    }

    @Test
    @DisplayName("Should fail validation when card number is null")
    void shouldFailValidation_WhenCardNumberIsNull() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Card number is required");
    }

    @Test
    @DisplayName("Should fail validation when suit is null")
    void shouldFailValidation_WhenSuitIsNull() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .cardNumber(0)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Suit is required");
    }

    @Test
    @DisplayName("Should allow null description")
    void shouldAllowNullDescription() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .description(null)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).isEmpty();
        assertThat(tarotCard.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should allow null image URL")
    void shouldAllowNullImageUrl() {
        // Given
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .imageUrl(null)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).isEmpty();
        assertThat(tarotCard.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should fail validation when image URL exceeds max length")
    void shouldFailValidation_WhenImageUrlExceedsMaxLength() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(500); // > 500 characters
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .imageUrl(longUrl)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Image URL must not exceed 500 characters");
    }

    @Test
    @DisplayName("Should create card for different suits")
    void shouldCreateCard_ForDifferentSuits() {
        // Given - Test all suit types
        TarotCard majorArcana = TarotCard.builder()
                .id(1)
                .name("The Fool")
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        TarotCard wands = TarotCard.builder()
                .id(14)
                .name("Ace of Wands")
                .cardNumber(1)
                .suit(SuitType.WANDS)
                .build();

        TarotCard cups = TarotCard.builder()
                .id(28)
                .name("Ace of Cups")
                .cardNumber(1)
                .suit(SuitType.CUPS)
                .build();

        TarotCard swords = TarotCard.builder()
                .id(42)
                .name("Ace of Swords")
                .cardNumber(1)
                .suit(SuitType.SWORDS)
                .build();

        TarotCard pentacles = TarotCard.builder()
                .id(56)
                .name("Ace of Pentacles")
                .cardNumber(1)
                .suit(SuitType.PENTACLES)
                .build();

        // When
        Set<ConstraintViolation<TarotCard>> violations1 = validator.validate(majorArcana);
        Set<ConstraintViolation<TarotCard>> violations2 = validator.validate(wands);
        Set<ConstraintViolation<TarotCard>> violations3 = validator.validate(cups);
        Set<ConstraintViolation<TarotCard>> violations4 = validator.validate(swords);
        Set<ConstraintViolation<TarotCard>> violations5 = validator.validate(pentacles);

        // Then
        assertThat(violations1).isEmpty();
        assertThat(violations2).isEmpty();
        assertThat(violations3).isEmpty();
        assertThat(violations4).isEmpty();
        assertThat(violations5).isEmpty();
    }
}
