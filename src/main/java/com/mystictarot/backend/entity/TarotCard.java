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

@Entity
@Table(name = "tarot_cards",
        indexes = {
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
