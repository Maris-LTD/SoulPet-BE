package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.TransactionStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Transaction entity validation
 * Following Given-When-Then pattern
 */
@DisplayName("Transaction Entity Validation Tests")
class TransactionEntityTest {

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
    @DisplayName("Should create transaction with valid data")
    void shouldCreateTransaction_WithValidData() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).isEmpty();
        assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getPlanType()).isEqualTo(PlanType.MONTHLY);
        assertThat(transaction.getProvider()).isEqualTo(PaymentProvider.STRIPE);
    }

    @Test
    @DisplayName("Should fail validation when user is null")
    void shouldFailValidation_WhenUserIsNull() {
        // Given
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("User is required");
    }

    @Test
    @DisplayName("Should fail validation when amount is null")
    void shouldFailValidation_WhenAmountIsNull() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Amount is required");
    }

    @Test
    @DisplayName("Should fail validation when amount is zero")
    void shouldFailValidation_WhenAmountIsZero() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Amount must be greater than 0");
    }

    @Test
    @DisplayName("Should fail validation when amount is negative")
    void shouldFailValidation_WhenAmountIsNegative() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("-10.00"))
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Amount must be greater than 0");
    }

    @Test
    @DisplayName("Should set default status when status is not provided")
    void shouldSetDefaultStatus_WhenStatusIsNotProvided() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        // Then
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    @DisplayName("Should allow null provider transaction ID")
    void shouldAllowNullProviderTransactionId() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .providerTransactionId(null)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).isEmpty();
        assertThat(transaction.getProviderTransactionId()).isNull();
    }

    @Test
    @DisplayName("Should accept valid amount with two decimal places")
    void shouldAcceptValidAmount_WithTwoDecimalPlaces() {
        // Given
        Transaction transaction = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("99.99"))
                .status(TransactionStatus.PENDING)
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.STRIPE)
                .build();

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).isEmpty();
        assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }
}
