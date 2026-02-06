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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MomoPaymentService implements PaymentProviderService {

    private static final String REQUEST_TYPE = "payWithATM";
    private static final String LANG = "vi";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.momo.partner-code}")
    private String partnerCode;

    @Value("${payment.momo.access-key}")
    private String accessKey;

    @Value("${payment.momo.secret-key}")
    private String secretKey;

    @Value("${payment.momo.endpoint:}")
    private String endpoint;

    @Value("${payment.momo.ipn-url:}")
    private String ipnUrl;

    @Value("${payment.momo.redirect-url:}")
    private String redirectUrl;

    @Override
    public CreateOrderResult createOrder(Transaction transaction, CreateOrderCommand command) {
        String orderId = transaction.getId().toString();
        String requestId = UUID.randomUUID().toString();
        long amount = command.getAmount().longValue();
        String orderInfo = "Mystic Tarot - " + command.getPlanType();
        String returnUrl = command.getReturnUrl() != null ? command.getReturnUrl() : redirectUrl;
        String ipn = ipnUrl != null && !ipnUrl.isBlank() ? ipnUrl : returnUrl;
        String extraData = Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8));

        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipn +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl +
                "&requestId=" + requestId +
                "&requestType=" + REQUEST_TYPE;
        String signature = signHmacSha256(rawSignature, secretKey);

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("accessKey", accessKey);
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", returnUrl);
        body.put("ipnUrl", ipn);
        body.put("extraData", extraData);
        body.put("requestType", REQUEST_TYPE);
        body.put("signature", signature);
        body.put("lang", LANG);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(endpoint, entity, String.class);
            if (response == null) {
                return CreateOrderResult.builder().success(false).errorCode("EMPTY_RESPONSE").errorMessage("Momo returned empty response").build();
            }
            JsonNode node = objectMapper.readTree(response);
            int resultCode = node.path("resultCode").asInt(-1);
            if (resultCode != 0) {
                return CreateOrderResult.builder()
                        .success(false)
                        .errorCode(String.valueOf(resultCode))
                        .errorMessage(node.path("message").asText("Unknown error"))
                        .build();
            }
            String payUrl = node.path("payUrl").asText(null);
            String transId = node.path("transId").asText(null);
            return CreateOrderResult.builder()
                    .success(true)
                    .paymentUrl(payUrl)
                    .providerTransactionId(transId != null ? transId : requestId)
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
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
