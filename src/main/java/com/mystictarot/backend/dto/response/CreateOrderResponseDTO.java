package com.mystictarot.backend.dto.response;

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
@Schema(description = "Response DTO for created payment order")
public class CreateOrderResponseDTO {

    @Schema(description = "Transaction ID")
    private UUID transactionId;

    @Schema(description = "URL to redirect user for payment")
    private String paymentUrl;

    @Schema(description = "When the payment link expires")
    private LocalDateTime expiresAt;
}
