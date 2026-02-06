package com.mystictarot.backend.dto.response;

import com.mystictarot.backend.entity.enums.SpreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Reading summary item in history list")
public class ReadingHistoryItemDTO {

    @Schema(description = "Reading unique identifier")
    private UUID id;

    @Schema(description = "User question (may be truncated for list view)")
    private String question;

    @Schema(description = "Spread type used", example = "THREE_CARDS")
    private SpreadType spreadType;

    @Schema(description = "Reading creation timestamp")
    private LocalDateTime createdAt;
}
