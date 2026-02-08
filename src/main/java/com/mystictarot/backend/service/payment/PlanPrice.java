package com.mystictarot.backend.service.payment;

import java.math.BigDecimal;

public record PlanPrice(BigDecimal amount, String currency) {
}
