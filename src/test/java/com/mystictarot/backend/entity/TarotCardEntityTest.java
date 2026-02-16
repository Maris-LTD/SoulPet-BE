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
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .imageUrl("https://example.com/cards/fool.jpg")
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).isEmpty();
        assertThat(tarotCard.getSuit()).isEqualTo(SuitType.MAJOR_ARCANA);
        assertThat(tarotCard.getCardNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should create tarot card with minimal required data")
    void shouldCreateTarotCard_WithMinimalRequiredData() {
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).isEmpty();
        assertThat(tarotCard.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should fail validation when id is null")
    void shouldFailValidation_WhenIdIsNull() {
        TarotCard tarotCard = TarotCard.builder()
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Card ID is required");
    }

    @Test
    @DisplayName("Should fail validation when card number is null")
    void shouldFailValidation_WhenCardNumberIsNull() {
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Card number is required");
    }

    @Test
    @DisplayName("Should fail validation when suit is null")
    void shouldFailValidation_WhenSuitIsNull() {
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .cardNumber(0)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Suit is required");
    }

    @Test
    @DisplayName("Should allow null image URL")
    void shouldAllowNullImageUrl() {
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .imageUrl(null)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).isEmpty();
        assertThat(tarotCard.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should fail validation when image URL exceeds max length")
    void shouldFailValidation_WhenImageUrlExceedsMaxLength() {
        String longUrl = "https://example.com/" + "a".repeat(500);
        TarotCard tarotCard = TarotCard.builder()
                .id(1)
                .imageUrl(longUrl)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();

        Set<ConstraintViolation<TarotCard>> violations = validator.validate(tarotCard);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Image URL must not exceed 500 characters");
    }

    @Test
    @DisplayName("Should create card for different suits")
    void shouldCreateCard_ForDifferentSuits() {
        TarotCard majorArcana = TarotCard.builder()
                .id(1)
                .cardNumber(0)
                .suit(SuitType.MAJOR_ARCANA)
                .build();
        TarotCard wands = TarotCard.builder()
                .id(14)
                .cardNumber(1)
                .suit(SuitType.WANDS)
                .build();
        TarotCard cups = TarotCard.builder()
                .id(28)
                .cardNumber(1)
                .suit(SuitType.CUPS)
                .build();
        TarotCard swords = TarotCard.builder()
                .id(42)
                .cardNumber(1)
                .suit(SuitType.SWORDS)
                .build();
        TarotCard pentacles = TarotCard.builder()
                .id(56)
                .cardNumber(1)
                .suit(SuitType.PENTACLES)
                .build();

        assertThat(validator.validate(majorArcana)).isEmpty();
        assertThat(validator.validate(wands)).isEmpty();
        assertThat(validator.validate(cups)).isEmpty();
        assertThat(validator.validate(swords)).isEmpty();
        assertThat(validator.validate(pentacles)).isEmpty();
    }
}
