package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.CreateOrderRequestDTO;
import com.mystictarot.backend.dto.response.CreateOrderResponseDTO;
import com.mystictarot.backend.entity.Transaction;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.TransactionStatus;
import com.mystictarot.backend.exception.InvalidPaymentException;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.repository.TransactionRepository;
import com.mystictarot.backend.repository.UserRepository;
import com.mystictarot.backend.service.payment.CreateOrderResult;
import com.mystictarot.backend.service.payment.PaymentServiceFactory;
import com.mystictarot.backend.service.payment.PlanPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrderService Tests")
class PaymentOrderServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentServiceFactory paymentServiceFactory;

    @Mock
    private PaymentPlanService paymentPlanService;

    @Mock
    private com.mystictarot.backend.service.payment.PaymentProviderService paymentProviderService;

    @InjectMocks
    private PaymentOrderService paymentOrderService;

    private UUID userId;
    private User user;
    private CreateOrderRequestDTO request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentOrderService, "orderExpiryMinutes", 15);
        when(paymentPlanService.getPlanPrice(eq(PlanType.MONTHLY), any())).thenReturn(new PlanPrice(BigDecimal.valueOf(99000), "VND"));
        when(paymentPlanService.getPlanPrice(eq(PlanType.UNLIMITED), any())).thenReturn(new PlanPrice(BigDecimal.valueOf(499000), "VND"));
        when(paymentPlanService.getPlanPrice(eq(PlanType.RETAIL_5), any())).thenReturn(new PlanPrice(BigDecimal.valueOf(29000), "VND"));
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test")
                .passwordHash("hash")
                .plan(PlanType.FREE)
                .extraCredits(0)
                .build();
        request = CreateOrderRequestDTO.builder()
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.MOMO)
                .returnUrl("https://app/return")
                .build();
    }

    @Test
    @DisplayName("Should throw when plan type is FREE")
    void createOrder_WhenPlanFree_Throws() {
        request.setPlanType(PlanType.FREE);
        assertThatThrownBy(() -> paymentOrderService.createOrder(userId, request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("FREE");
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw when user not found")
    void createOrder_WhenUserNotFound_Throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentOrderService.createOrder(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create order and return payment URL")
    void createOrder_Success_ReturnsResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });
        when(paymentServiceFactory.getService(PaymentProvider.MOMO)).thenReturn(paymentProviderService);
        when(paymentProviderService.createOrder(any(Transaction.class), any())).thenReturn(
                CreateOrderResult.builder()
                        .success(true)
                        .paymentUrl("https://momo.vn/pay/xxx")
                        .providerTransactionId("momo-123")
                        .build()
        );
        CreateOrderResponseDTO response = paymentOrderService.createOrder(userId, request);
        assertThat(response).isNotNull();
        assertThat(response.getPaymentUrl()).isEqualTo("https://momo.vn/pay/xxx");
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getExpiresAt()).isNotNull();
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw when provider returns failure")
    void createOrder_WhenProviderFails_Throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(paymentServiceFactory.getService(PaymentProvider.MOMO)).thenReturn(paymentProviderService);
        when(paymentProviderService.createOrder(any(Transaction.class), any())).thenReturn(
                CreateOrderResult.builder().success(false).errorMessage("Insufficient balance").build()
        );
        assertThatThrownBy(() -> paymentOrderService.createOrder(userId, request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Insufficient balance");
    }
}
