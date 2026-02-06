package com.mystictarot.backend.dto.request;

import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.entity.enums.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for creating a payment order")
public class CreateOrderRequestDTO {

    @Schema(description = "Subscription plan to purchase", example = "MONTHLY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Plan type is required")
    private PlanType planType;

    @Schema(description = "Payment provider", example = "MOMO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @Schema(description = "URL to redirect after successful payment")
    @Size(max = 2000)
    private String returnUrl;

    @Schema(description = "URL to redirect when payment is cancelled")
    @Size(max = 2000)
    private String cancelUrl;

    @Schema(description = "Client-generated idempotency key to prevent duplicate orders")
    @Size(max = 255)
    private String idempotencyKey;

    @AssertTrue(message = "Plan type must be MONTHLY, UNLIMITED or RETAIL_5")
    @Schema(hidden = true)
    public boolean isPlanTypePurchasable() {
        return planType == null || planType != PlanType.FREE;
    }
}
