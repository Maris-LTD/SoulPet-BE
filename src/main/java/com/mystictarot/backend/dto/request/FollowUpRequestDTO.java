package com.mystictarot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for follow-up question on an existing reading")
public class FollowUpRequestDTO {

    @Schema(description = "Reading ID to ask follow-up about", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Reading ID is required")
    private UUID readingId;

    @Schema(description = "Follow-up question message", example = "Can you elaborate on the second card?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String message;
}
