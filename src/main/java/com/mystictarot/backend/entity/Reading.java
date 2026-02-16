package com.mystictarot.backend.entity;

import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.entity.enums.SpreadType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reading entity representing a tarot reading session
 * Note: Soft delete is handled via status field and repository query methods
 */
@Entity
@Table(name = "readings", indexes = {
    @Index(name = "idx_readings_user_id", columnList = "user_id"),
    @Index(name = "idx_readings_created_at", columnList = "created_at"),
    @Index(name = "idx_readings_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_readings_user"))
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    @NotNull(message = "Question is required")
    @Size(min = 1, max = 1000, message = "Question must be between 1 and 1000 characters")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "spread_type", nullable = false)
    @NotNull(message = "Spread type is required")
    private SpreadType spreadType;

    /**
     * JSON array of selected tarot cards with their orientations
     * Expected format: [{"id": 1, "orientation": "UPRIGHT"}, {"id": 15, "orientation": "REVERSED"}]
     * Validation is performed at service layer to ensure valid JSON format
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cards_json", nullable = false, columnDefinition = "JSONB")
    @NotNull(message = "Cards JSON is required")
    private String cardsJson;

    @Column(name = "interpretation_text", columnDefinition = "TEXT")
    private String interpretationText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private ReadingStatus status = ReadingStatus.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reading", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> chatMessages = new ArrayList<>();
}
