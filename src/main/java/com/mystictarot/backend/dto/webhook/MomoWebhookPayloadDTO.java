package com.mystictarot.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomoWebhookPayloadDTO {

    private String partnerCode;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("requestId")
    private String requestId;

    private Long transId;

    private Integer resultCode;

    private String message;

    private BigDecimal amount;

    private Long responseTime;

    private String orderType;

    private String signature;

    private String extraData;

    private String payType;

    private String orderInfo;
}
