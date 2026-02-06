package com.mystictarot.backend.dto.request;

import com.mystictarot.backend.entity.enums.SpreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for tarot reading interpretation")
public class InterpretRequestDTO {

    @Schema(description = "User's question for the reading", example = "What should I focus on this week?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Question is required")
    @Size(min = 1, max = 1000, message = "Question must be between 1 and 1000 characters")
    private String question;

    @Schema(description = "Type of spread used", example = "THREE_CARDS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Spread type is required")
    private SpreadType spreadType;

    @Schema(description = "List of selected cards with orientation", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Cards are required")
    @Valid
    private List<CardDTO> cards;
}
