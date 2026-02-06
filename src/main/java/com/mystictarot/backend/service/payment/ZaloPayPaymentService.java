package com.mystictarot.backend.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ZaloPayPaymentService implements PaymentProviderService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.zalopay.app-id}")
    private String appId;

    @Value("${payment.zalopay.key1}")
    private String key1;

    @Value("${payment.zalopay.key2}")
    private String key2;

    @Value("${payment.zalopay.endpoint:}")
    private String endpoint;

    @Value("${payment.zalopay.callback-url:}")
    private String callbackUrl;

    @Override
    public CreateOrderResult createOrder(Transaction transaction, CreateOrderCommand command) {
        String appTransId = String.format("%02d", (int) (System.currentTimeMillis() % 100)) + System.currentTimeMillis();
        long amount = command.getAmount().longValue();
        String appUser = "user_" + transaction.getUser().getId();
        String embedData = "{}";
        String item = "[]";
        String orderInfo = "Mystic Tarot - " + command.getPlanType();
        String callback = callbackUrl != null && !callbackUrl.isBlank() ? callbackUrl : command.getReturnUrl();

        String data = appId + "|" + appTransId + "|" + transaction.getUser().getId() + "|" + amount + "|" + System.currentTimeMillis() + "|" + embedData + "|" + item;
        String mac = signHmacSha256(data, key1);

        Map<String, Object> body = new HashMap<>();
        body.put("app_id", Long.parseLong(appId));
        body.put("app_user", appUser);
        body.put("app_time", System.currentTimeMillis());
        body.put("amount", amount);
        body.put("app_trans_id", appTransId);
        body.put("embed_data", embedData);
        body.put("item", item);
        body.put("description", orderInfo);
        body.put("callback_url", callback != null ? callback : "");
        body.put("mac", mac);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(endpoint, entity, String.class);
            if (response == null) {
                return CreateOrderResult.builder().success(false).errorCode("EMPTY_RESPONSE").errorMessage("ZaloPay returned empty response").build();
            }
            JsonNode node = objectMapper.readTree(response);
            int returnCode = node.path("return_code").asInt(-1);
            if (returnCode != 1) {
                return CreateOrderResult.builder()
                        .success(false)
                        .errorCode(String.valueOf(returnCode))
                        .errorMessage(node.path("return_message").asText("Unknown error"))
                        .build();
            }
            String orderUrl = node.path("order_url").asText(null);
            return CreateOrderResult.builder()
                    .success(true)
                    .paymentUrl(orderUrl)
                    .providerTransactionId(appTransId)
                    .build();
        } catch (Exception e) {
            return CreateOrderResult.builder()
                    .success(false)
                    .errorCode("CALL_FAILED")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private static String signHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 failed", e);
        }
    }
}
