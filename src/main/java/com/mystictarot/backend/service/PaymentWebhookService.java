package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.webhook.MomoWebhookPayloadDTO;
import com.mystictarot.backend.dto.webhook.ZaloPayCallbackDataDTO;
import com.mystictarot.backend.dto.webhook.ZaloPayWebhookPayloadDTO;
import com.mystictarot.backend.entity.Transaction;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.TransactionStatus;
import com.mystictarot.backend.exception.InvalidPaymentException;
import com.mystictarot.backend.repository.TransactionRepository;
import com.mystictarot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final int MOMO_SUCCESS_CODE = 0;
    private static final int RETAIL_5_CREDITS = 5;
    private static final int MONTHLY_DAYS = 30;
    private static final int UNLIMITED_YEARS = 100;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${payment.momo.secret-key}")
    private String momoSecretKey;

    @Value("${payment.zalopay.key2}")
    private String zaloPayKey2;

    @Transactional
    public void handleMomoCallback(MomoWebhookPayloadDTO payload) {
        String orderId = payload.getOrderId();
        Optional<Transaction> byProvider = transactionRepository.findByProviderTransactionId(orderId);
        if (byProvider.isEmpty()) {
            try {
                byProvider = transactionRepository.findById(UUID.fromString(orderId));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Transaction transaction = byProvider.orElseThrow(() -> new InvalidPaymentException("Transaction not found: " + orderId));
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            return;
        }
        if (payload.getResultCode() == null || payload.getResultCode() != MOMO_SUCCESS_CODE) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            return;
        }
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
        applySubscription(transaction);
    }

    public boolean verifyMomoSignature(MomoWebhookPayloadDTO payload) {
        String extraData = payload.getExtraData() != null ? payload.getExtraData() : "";
        String message = payload.getMessage() != null ? payload.getMessage() : "";
        String orderId = payload.getOrderId() != null ? payload.getOrderId() : "";
        String orderInfo = payload.getOrderInfo() != null ? payload.getOrderInfo() : "";
        String orderType = payload.getOrderType() != null ? payload.getOrderType() : "";
        String partnerCode = payload.getPartnerCode() != null ? payload.getPartnerCode() : "";
        String payType = payload.getPayType() != null ? payload.getPayType() : "";
        String requestId = payload.getRequestId() != null ? payload.getRequestId() : "";
        Long responseTime = payload.getResponseTime();
        Integer resultCode = payload.getResultCode();
        Long transId = payload.getTransId();
        String raw = "amount=" + payload.getAmount() +
                "&extraData=" + extraData +
                "&message=" + message +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&orderType=" + orderType +
                "&partnerCode=" + partnerCode +
                "&payType=" + payType +
                "&requestId=" + requestId +
                "&responseTime=" + (responseTime != null ? responseTime : "") +
                "&resultCode=" + (resultCode != null ? resultCode : "") +
                "&transId=" + (transId != null ? transId : "");
        String sign = signHmacSha256(raw, momoSecretKey);
        return sign != null && sign.equalsIgnoreCase(payload.getSignature());
    }

    @Transactional
    public void handleZaloPayCallback(ZaloPayCallbackDataDTO data) {
        String appTransId = data.getAppTransId();
        Transaction transaction = transactionRepository.findByProviderTransactionId(appTransId)
                .orElseThrow(() -> new InvalidPaymentException("Transaction not found: " + appTransId));
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            return;
        }
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
        applySubscription(transaction);
    }

    public boolean verifyZaloPayMac(ZaloPayWebhookPayloadDTO payload, String dataStr) {
        String mac = signHmacSha256(dataStr, zaloPayKey2);
        return mac != null && mac.equalsIgnoreCase(payload.getMac());
    }

    private void applySubscription(Transaction transaction) {
        User user = transaction.getUser();
        PlanType planType = transaction.getPlanType();
        if (planType == PlanType.RETAIL_5) {
            user.setExtraCredits((user.getExtraCredits() != null ? user.getExtraCredits() : 0) + RETAIL_5_CREDITS);
        } else if (planType == PlanType.MONTHLY) {
            user.setPlan(PlanType.MONTHLY);
            user.setSubscriptionExpiry(LocalDateTime.now().plusDays(MONTHLY_DAYS));
        } else if (planType == PlanType.UNLIMITED) {
            user.setPlan(PlanType.UNLIMITED);
            user.setSubscriptionExpiry(LocalDateTime.now().plusYears(UNLIMITED_YEARS));
        }
        userRepository.save(user);
    }

    private static String signHmacSha256(String data, String key) {
        if (key == null || key.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
