package com.mystictarot.backend.dto.response;

import com.mystictarot.backend.entity.enums.SpreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Full reading detail with chat messages")
public class ReadingDetailDTO {

    @Schema(description = "Reading unique identifier")
    private UUID id;

    @Schema(description = "User question")
    private String question;

    @Schema(description = "Spread type used", example = "THREE_CARDS")
    private SpreadType spreadType;

    @Schema(description = "JSON array of selected cards with orientations")
    private String cardsJson;

    @Schema(description = "AI-generated interpretation text")
    private String interpretationText;

    @Schema(description = "Reading creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Chat messages in conversation order")
    private List<ChatMessageItemDTO> chatMessages;
}
