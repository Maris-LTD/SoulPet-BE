package com.mystictarot.backend.dto.response;

import com.mystictarot.backend.entity.enums.ChatRole;
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
@Schema(description = "Chat message item in reading detail")
public class ChatMessageItemDTO {

    @Schema(description = "Message unique identifier")
    private UUID id;

    @Schema(description = "Role of the message sender", example = "USER")
    private ChatRole role;

    @Schema(description = "Message content")
    private String content;

    @Schema(description = "Message creation timestamp")
    private LocalDateTime createdAt;
}
