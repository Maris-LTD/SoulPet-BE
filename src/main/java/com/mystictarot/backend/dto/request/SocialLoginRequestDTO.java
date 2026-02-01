package com.mystictarot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLoginRequestDTO {

    @NotBlank(message = "Provider is required")
    private String provider; // "google" or "facebook"

    @NotBlank(message = "Access token is required")
    private String accessToken;

    private String email;
    private String name;
    private String avatarUrl;
}
