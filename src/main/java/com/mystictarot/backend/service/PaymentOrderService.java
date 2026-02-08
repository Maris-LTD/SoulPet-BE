package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.CreateOrderRequestDTO;
import com.mystictarot.backend.dto.response.CreateOrderResponseDTO;
import com.mystictarot.backend.entity.Transaction;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.exception.InvalidPaymentException;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.repository.TransactionRepository;
import com.mystictarot.backend.repository.UserRepository;
import com.mystictarot.backend.service.payment.CreateOrderCommand;
import com.mystictarot.backend.service.payment.CreateOrderResult;
import com.mystictarot.backend.service.payment.PaymentServiceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentServiceFactory paymentServiceFactory;
    private final PaymentPlanService paymentPlanService;

    @Value("${payment.order-expiry-minutes:15}")
    private int orderExpiryMinutes;

    @Transactional
    public CreateOrderResponseDTO createOrder(UUID userId, CreateOrderRequestDTO request) {
        if (request.getPlanType() == PlanType.FREE) {
            throw new InvalidPaymentException("Cannot create order for FREE plan");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        var planPrice = paymentPlanService.getPlanPrice(request.getPlanType(), request.getLang());
        BigDecimal amount = planPrice.amount();
        String currency = planPrice.currency();
        Transaction transaction;
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = transactionRepository.findByUser_IdAndIdempotencyKey(userId, request.getIdempotencyKey());
            if (existing.isPresent()) {
                Transaction t = existing.get();
                if (t.getPaymentUrl() != null) {
                    LocalDateTime expiresAt = t.getCreatedAt().plusMinutes(orderExpiryMinutes);
                    return CreateOrderResponseDTO.builder()
                            .transactionId(t.getId())
                            .paymentUrl(t.getPaymentUrl())
                            .expiresAt(expiresAt)
                            .build();
                }
                transaction = t;
            } else {
                transaction = Transaction.builder()
                        .user(user)
                        .amount(amount)
                        .planType(request.getPlanType())
                        .provider(request.getProvider())
                        .idempotencyKey(request.getIdempotencyKey())
                        .build();
                transaction = transactionRepository.save(transaction);
            }
        } else {
            transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .planType(request.getPlanType())
                .provider(request.getProvider())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
            transaction = transactionRepository.save(transaction);
        }
        CreateOrderCommand command = CreateOrderCommand.builder()
                .planType(request.getPlanType())
                .amount(amount)
                .currency(currency)
                .returnUrl(request.getReturnUrl())
                .cancelUrl(request.getCancelUrl())
                .idempotencyKey(request.getIdempotencyKey())
                .orderId(transaction.getId().toString())
                .build();
        CreateOrderResult result = paymentServiceFactory.getService(request.getProvider())
                .createOrder(transaction, command);
        if (!result.isSuccess()) {
            throw new InvalidPaymentException(result.getErrorMessage() != null ? result.getErrorMessage() : "Payment provider error");
        }
        transaction.setProviderTransactionId(result.getProviderTransactionId());
        transaction.setPaymentUrl(result.getPaymentUrl());
        transactionRepository.save(transaction);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(orderExpiryMinutes);
        return CreateOrderResponseDTO.builder()
                .transactionId(transaction.getId())
                .paymentUrl(result.getPaymentUrl())
                .expiresAt(expiresAt)
                .build();
    }

}
