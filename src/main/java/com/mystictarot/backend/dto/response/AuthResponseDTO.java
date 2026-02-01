package com.mystictarot.backend.dto.response;

import com.mystictarot.backend.entity.enums.PlanType;
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
@Schema(description = "Response DTO containing authentication token and user information")
public class AuthResponseDTO {

    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "User unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "User full name", example = "John Doe")
    private String name;

    @Schema(description = "User subscription plan", example = "FREE")
    private PlanType plan;

    @Schema(description = "Extra credits available", example = "0")
    private Integer extraCredits;

    @Schema(description = "Subscription expiry date (null for FREE plan)", example = "2024-12-31T23:59:59")
    private LocalDateTime subscriptionExpiry;

    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}
