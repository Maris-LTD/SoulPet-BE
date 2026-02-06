package com.mystictarot.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO for follow-up question answer")
public class FollowUpResponseDTO {

    @Schema(description = "AI-generated response to the follow-up question")
    private String content;
}
