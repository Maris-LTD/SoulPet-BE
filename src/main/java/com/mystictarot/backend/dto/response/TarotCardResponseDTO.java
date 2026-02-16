package com.mystictarot.backend.dto.response;

import com.mystictarot.backend.entity.enums.SuitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tarot card with localized name and description")
public class TarotCardResponseDTO {

    @Schema(description = "Card ID (1-78)")
    private Integer id;

    @Schema(description = "Card number within suit")
    private Integer cardNumber;

    @Schema(description = "Suit type")
    private SuitType suit;

    @Schema(description = "Localized card name")
    private String name;

    @Schema(description = "Localized card description")
    private String description;

    @Schema(description = "Image URL")
    private String imageUrl;
}
