package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.request.CreateOrderRequestDTO;
import com.mystictarot.backend.dto.response.CreateOrderResponseDTO;
import com.mystictarot.backend.entity.enums.PaymentProvider;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.exception.InvalidPaymentException;
import com.mystictarot.backend.service.PaymentOrderService;
import com.mystictarot.backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Controller Tests")
class PaymentControllerTest {

    @Mock
    private PaymentOrderService paymentOrderService;

    @InjectMocks
    private PaymentController paymentController;

    private UUID userId;
    private CreateOrderRequestDTO request;
    private CreateOrderResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        request = CreateOrderRequestDTO.builder()
                .planType(PlanType.MONTHLY)
                .provider(PaymentProvider.MOMO)
                .returnUrl("https://app/return")
                .build();
        responseDTO = CreateOrderResponseDTO.builder()
                .transactionId(UUID.randomUUID())
                .paymentUrl("https://momo.vn/pay/xxx")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Test
    @DisplayName("Should create order and return 200 with payment URL")
    void createOrder_Success_Returns200() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(paymentOrderService.createOrder(eq(userId), eq(request))).thenReturn(responseDTO);

            ResponseEntity<CreateOrderResponseDTO> response = paymentController.createOrder(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPaymentUrl()).isEqualTo("https://momo.vn/pay/xxx");
            assertThat(response.getBody().getTransactionId()).isNotNull();
            verify(paymentOrderService).createOrder(userId, request);
        }
    }

    @Test
    @DisplayName("Should return 400 when service throws InvalidPaymentException")
    void createOrder_WhenInvalidPayment_Throws() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(paymentOrderService.createOrder(eq(userId), eq(request)))
                    .thenThrow(new InvalidPaymentException("Cannot create order for FREE plan"));

            assertThatThrownBy(() -> paymentController.createOrder(request))
                    .isInstanceOf(InvalidPaymentException.class)
                    .hasMessageContaining("FREE");
        }
    }
}
