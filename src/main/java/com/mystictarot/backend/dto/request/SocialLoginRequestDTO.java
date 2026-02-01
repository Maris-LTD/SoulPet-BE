package com.mystictarot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for social login (Google/Facebook)")
public class SocialLoginRequestDTO {

    @Schema(description = "OAuth2 provider name", example = "google")
    @NotBlank(message = "Provider is required")
    private String provider; // "google" or "facebook"

    @Schema(description = "OAuth2 access token from provider", example = "ya29.a0AfH6SMC...")
    @NotBlank(message = "Access token is required")
    private String accessToken;

    @Schema(description = "User email from provider", example = "user@example.com")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(description = "User name from provider", example = "John Doe")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "User avatar URL from provider", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
}
