package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction entity representing payment transactions
 */
@Entity
@Table(name = "transactions", 
    indexes = {
        @Index(name = "idx_transactions_user_id", columnList = "user_id"),
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_provider_transaction_id", columnList = "provider_transaction_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_provider_id", columnNames = {"provider", "provider_transaction_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_user"))
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Status is required")
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    @NotNull(message = "Plan type is required")
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @NotNull(message = "Provider is required")
    private PaymentProvider provider;

    @Column(name = "provider_transaction_id", length = 255)
    @Size(max = 255, message = "Provider transaction ID must not exceed 255 characters")
    private String providerTransactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
