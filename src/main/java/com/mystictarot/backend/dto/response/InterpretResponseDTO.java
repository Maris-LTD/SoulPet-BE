package com.mystictarot.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO for tarot reading interpretation")
public class InterpretResponseDTO {

    @Schema(description = "Reading unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID readingId;

    @Schema(description = "AI-generated interpretation text")
    private String interpretation;
}
