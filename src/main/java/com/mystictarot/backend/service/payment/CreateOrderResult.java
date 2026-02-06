package com.mystictarot.backend.service.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderResult {

    private boolean success;
    private String paymentUrl;
    private String providerTransactionId;
    private String errorCode;
    private String errorMessage;
}
