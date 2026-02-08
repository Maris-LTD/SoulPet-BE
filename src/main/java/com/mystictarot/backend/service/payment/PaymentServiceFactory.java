package com.mystictarot.backend.service.payment;

import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.exception.InvalidPaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFactory {

    private final MomoPaymentService momoPaymentService;
    private final ZaloPayPaymentService zaloPayPaymentService;
    private final StripePaymentService stripePaymentService;

    @Value("${payment.momo.enabled:false}")
    private boolean momoEnabled;

    @Value("${payment.zalopay.enabled:false}")
    private boolean zaloPayEnabled;

    public PaymentServiceFactory(MomoPaymentService momoPaymentService,
                                ZaloPayPaymentService zaloPayPaymentService,
                                StripePaymentService stripePaymentService) {
        this.momoPaymentService = momoPaymentService;
        this.zaloPayPaymentService = zaloPayPaymentService;
        this.stripePaymentService = stripePaymentService;
    }

    public PaymentProviderService getService(PaymentProvider provider) {
        return switch (provider) {
            case MOMO -> {
                if (!momoEnabled) throw new InvalidPaymentException("Momo payment is currently disabled");
                yield momoPaymentService;
            }
            case ZALOPAY -> {
                if (!zaloPayEnabled) throw new InvalidPaymentException("ZaloPay payment is currently disabled");
                yield zaloPayPaymentService;
            }
            case STRIPE -> stripePaymentService;
            default -> throw new InvalidPaymentException("Unsupported payment provider: " + provider);
        };
    }
}
