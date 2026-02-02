package com.mystictarot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for updating user profile")
public class UpdateProfileRequestDTO {

    @Schema(description = "User full name (optional)", example = "John Doe", minLength = 1, maxLength = 100)
    private String name;

    @Schema(description = "User avatar URL (optional)", example = "https://example.com/avatar.jpg", maxLength = 500)
    private String avatarUrl;
}
