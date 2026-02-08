package com.mystictarot.backend.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentProviderService {

    private static final String STRIPE_API_BASE = "https://api.stripe.com/v1/checkout/sessions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @Value("${payment.stripe.currency:vnd}")
    private String currency;

    @Value("${payment.stripe.success-url:}")
    private String successUrl;

    @Value("${payment.stripe.cancel-url:}")
    private String cancelUrl;

    private static final java.util.Set<String> ZERO_DECIMAL_CURRENCIES = java.util.Set.of(
            "vnd", "jpy", "krw", "clp", "gnf", "jod", "kmf", "mga", "pyg", "rwf", "ugx", "vuv", "xaf", "xof", "xpf"
    );

    @Override
    public CreateOrderResult createOrder(Transaction transaction, CreateOrderCommand command) {
        String returnUrl = command.getReturnUrl() != null ? command.getReturnUrl() : successUrl;
        String cancel = command.getCancelUrl() != null ? command.getCancelUrl() : cancelUrl;
        String effectiveCurrency = (command.getCurrency() != null && !command.getCurrency().isBlank())
                ? command.getCurrency() : currency;
        String currencyLower = effectiveCurrency != null ? effectiveCurrency.toLowerCase() : "vnd";
        long unitAmount = ZERO_DECIMAL_CURRENCIES.contains(currencyLower)
                ? command.getAmount().longValue()
                : command.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        String clientReferenceId = transaction.getId().toString();
        String description = "Mystic Tarot - " + command.getPlanType();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + secretKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mode", "payment");
        body.add("success_url", returnUrl);
        body.add("cancel_url", cancel);
        body.add("client_reference_id", clientReferenceId);
        body.add("line_items[0][quantity]", "1");
        body.add("line_items[0][price_data][currency]", currencyLower);
        body.add("line_items[0][price_data][unit_amount]", String.valueOf(unitAmount));
        body.add("line_items[0][price_data][product_data][name]", description);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(STRIPE_API_BASE, entity, String.class);
            if (response == null) {
                return CreateOrderResult.builder().success(false).errorCode("EMPTY_RESPONSE").errorMessage("Stripe returned empty response").build();
            }
            Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
            String id = (String) parsed.get("id");
            String url = (String) parsed.get("url");
            if (id == null || url == null) {
                return CreateOrderResult.builder().success(false).errorCode("PARSE_ERROR").errorMessage("Missing id or url in response").build();
            }
            return CreateOrderResult.builder()
                    .success(true)
                    .paymentUrl(url)
                    .providerTransactionId(id)
                    .build();
        } catch (Exception e) {
            return CreateOrderResult.builder()
                    .success(false)
                    .errorCode("CALL_FAILED")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
