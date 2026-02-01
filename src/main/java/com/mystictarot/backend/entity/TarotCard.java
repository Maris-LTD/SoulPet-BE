package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.SuitType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tarot card entity representing master data for 78 tarot cards
 * [Phỏng đoán] Sử dụng Entity thay vì Enum để linh hoạt hơn trong việc lưu description và image_url
 */
@Entity
@Table(name = "tarot_cards", 
    indexes = {
        @Index(name = "idx_tarot_cards_name", columnList = "name", unique = true),
        @Index(name = "idx_tarot_cards_suit", columnList = "suit")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tarot_cards_suit_number", columnNames = {"suit", "card_number"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarotCard {

    @Id
    @Column(name = "id", nullable = false)
    @NotNull(message = "Card ID is required")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    @NotNull(message = "Card name is required")
    @Size(max = 100, message = "Card name must not exceed 100 characters")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Column(name = "card_number", nullable = false)
    @NotNull(message = "Card number is required")
    private Integer cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "suit", nullable = false)
    @NotNull(message = "Suit is required")
    private SuitType suit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
