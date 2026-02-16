package com.mystictarot.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tarot_card_translations",
        indexes = {
                @Index(name = "idx_tarot_card_translations_card_id", columnList = "card_id"),
                @Index(name = "idx_tarot_card_translations_locale", columnList = "locale")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tarot_card_translations_card_locale", columnNames = {"card_id", "locale"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarotCardTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tarot_card_translations_card"))
    @NotNull(message = "Card is required")
    private TarotCard tarotCard;

    @Column(name = "locale", nullable = false, length = 10)
    @NotBlank(message = "Locale is required")
    @Size(max = 10)
    private String locale;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
