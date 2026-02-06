package com.mystictarot.backend.dto.request;

import com.mystictarot.backend.entity.enums.CardOrientation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A tarot card selection with orientation for interpretation")
public class CardDTO {

    @Schema(description = "Tarot card ID (1-78)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Card ID is required")
    private Integer id;

    @Schema(description = "Card orientation", example = "UPRIGHT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Card orientation is required")
    private CardOrientation orientation;
}
