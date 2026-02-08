package com.mystictarot.backend.dto.response;

import com.mystictarot.backend.entity.enums.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Plan info with price for display")
public class PlanInfoDTO {

    @Schema(description = "Plan type", example = "MONTHLY")
    private PlanType planType;

    @Schema(description = "Price amount: for VND the value in đồng (e.g. 199000); for USD the value in dollars (e.g. 19.99)")
    private Double amount;

    @Schema(description = "Currency code (VND, USD, etc.)", example = "VND")
    private String currency;

    @Schema(description = "Extra credits (e.g. for RETAIL_5)", example = "5")
    private Integer extraCredits;
}
