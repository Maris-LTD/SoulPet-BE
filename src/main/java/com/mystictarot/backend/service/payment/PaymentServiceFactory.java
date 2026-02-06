package com.mystictarot.backend.service.payment;

import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.exception.InvalidPaymentException;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFactory {

    private final MomoPaymentService momoPaymentService;
    private final ZaloPayPaymentService zaloPayPaymentService;
    private final StripePaymentService stripePaymentService;

    public PaymentServiceFactory(MomoPaymentService momoPaymentService,
                                ZaloPayPaymentService zaloPayPaymentService,
                                StripePaymentService stripePaymentService) {
        this.momoPaymentService = momoPaymentService;
        this.zaloPayPaymentService = zaloPayPaymentService;
        this.stripePaymentService = stripePaymentService;
    }

    public PaymentProviderService getService(PaymentProvider provider) {
        return switch (provider) {
            case MOMO -> momoPaymentService;
            case ZALOPAY -> zaloPayPaymentService;
            case STRIPE -> stripePaymentService;
            default -> throw new InvalidPaymentException("Unsupported payment provider: " + provider);
        };
    }
}
