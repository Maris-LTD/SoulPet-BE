package com.mystictarot.backend.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentProviderService {

    private static final String STRIPE_API_BASE = "https://api.stripe.com/v1/checkout/sessions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @Value("${payment.stripe.success-url:}")
    private String successUrl;

    @Value("${payment.stripe.cancel-url:}")
    private String cancelUrl;

    @Override
    public CreateOrderResult createOrder(Transaction transaction, CreateOrderCommand command) {
        String returnUrl = command.getReturnUrl() != null ? command.getReturnUrl() : successUrl;
        String cancel = command.getCancelUrl() != null ? command.getCancelUrl() : cancelUrl;
        long amountCents = command.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        String clientReferenceId = transaction.getId().toString();
        String description = "Mystic Tarot - " + command.getPlanType();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);

        Map<String, Object> body = new HashMap<>();
        body.put("mode", "payment");
        body.put("success_url", returnUrl);
        body.put("cancel_url", cancel);
        body.put("client_reference_id", clientReferenceId);
        Map<String, Object> lineItem = new HashMap<>();
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("currency", "usd");
        priceData.put("unit_amount", amountCents);
        Map<String, Object> productData = new HashMap<>();
        productData.put("name", description);
        priceData.put("product_data", productData);
        lineItem.put("price_data", priceData);
        lineItem.put("quantity", 1);
        body.put("line_items", java.util.List.of(lineItem));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

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
