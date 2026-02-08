package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.response.PlanInfoDTO;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.exception.InvalidPaymentException;
import com.mystictarot.backend.service.payment.PlanPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentPlanService {

    @Value("${payment.plans.monthly.amount:199000}")
    private long monthlyAmount;

    @Value("${payment.plans.monthly.currency:VND}")
    private String monthlyCurrency;

    @Value("${payment.plans.unlimited.amount:399000}")
    private long unlimitedAmount;

    @Value("${payment.plans.unlimited.currency:VND}")
    private String unlimitedCurrency;

    @Value("${payment.plans.retail_5.amount:19000}")
    private long retail5Amount;

    @Value("${payment.plans.retail_5.currency:VND}")
    private String retail5Currency;

    @Value("${payment.plans.retail_5.extra-credits:5}")
    private int retail5ExtraCredits;

    @Value("${payment.locales.en.monthly.amount:19.99}")
    private double enMonthlyAmount;

    @Value("${payment.locales.en.monthly.currency:USD}")
    private String enMonthlyCurrency;

    @Value("${payment.locales.en.unlimited.amount:39.99}")
    private double enUnlimitedAmount;

    @Value("${payment.locales.en.unlimited.currency:USD}")
    private String enUnlimitedCurrency;

    @Value("${payment.locales.en.retail_5.amount:1.99}")
    private double enRetail5Amount;

    @Value("${payment.locales.en.retail_5.currency:USD}")
    private String enRetail5Currency;

    @Value("${payment.locales.en.retail_5.extra-credits:5}")
    private int enRetail5ExtraCredits;

    @Value("${payment.default-locale:vi}")
    private String defaultLocale;

    public List<PlanInfoDTO> getPurchasablePlans(String languageCode) {
        String locale = normalizeLocale(languageCode);
        if (isVietnamLocale(locale)) {
            return buildPlanList((double) monthlyAmount, monthlyCurrency, (double) unlimitedAmount, unlimitedCurrency,
                    (double) retail5Amount, retail5Currency, retail5ExtraCredits);
        }
        return buildPlanList(enMonthlyAmount, enMonthlyCurrency, enUnlimitedAmount, enUnlimitedCurrency,
                enRetail5Amount, enRetail5Currency, enRetail5ExtraCredits);
    }

    public PlanPrice getPlanPrice(PlanType planType, String languageCode) {
        String locale = normalizeLocale(languageCode);
        if (isVietnamLocale(locale)) {
            return switch (planType) {
                case MONTHLY -> new PlanPrice(BigDecimal.valueOf(monthlyAmount), monthlyCurrency);
                case UNLIMITED -> new PlanPrice(BigDecimal.valueOf(unlimitedAmount), unlimitedCurrency);
                case RETAIL_5 -> new PlanPrice(BigDecimal.valueOf(retail5Amount), retail5Currency);
                default -> throw new InvalidPaymentException("Invalid plan type for payment: " + planType);
            };
        }
        return switch (planType) {
            case MONTHLY -> new PlanPrice(BigDecimal.valueOf(enMonthlyAmount), enMonthlyCurrency);
            case UNLIMITED -> new PlanPrice(BigDecimal.valueOf(enUnlimitedAmount), enUnlimitedCurrency);
            case RETAIL_5 -> new PlanPrice(BigDecimal.valueOf(enRetail5Amount), enRetail5Currency);
            default -> throw new InvalidPaymentException("Invalid plan type for payment: " + planType);
        };
    }

    private static boolean isVietnamLocale(String locale) {
        return "vi".equalsIgnoreCase(locale) || "vn".equalsIgnoreCase(locale);
    }

    private String normalizeLocale(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return defaultLocale != null ? defaultLocale : "vi";
        }
        return languageCode.trim();
    }

    private List<PlanInfoDTO> buildPlanList(double monthlyAmount, String monthlyCurrency,
                                            double unlimitedAmount, String unlimitedCurrency,
                                            double retail5Amount, String retail5Currency, int retail5ExtraCredits) {
        return List.of(
                PlanInfoDTO.builder()
                        .planType(PlanType.MONTHLY)
                        .amount(monthlyAmount)
                        .currency(monthlyCurrency)
                        .extraCredits(null)
                        .build(),
                PlanInfoDTO.builder()
                        .planType(PlanType.UNLIMITED)
                        .amount(unlimitedAmount)
                        .currency(unlimitedCurrency)
                        .extraCredits(null)
                        .build(),
                PlanInfoDTO.builder()
                        .planType(PlanType.RETAIL_5)
                        .amount(retail5Amount)
                        .currency(retail5Currency)
                        .extraCredits(retail5ExtraCredits)
                        .build()
        );
    }
}
