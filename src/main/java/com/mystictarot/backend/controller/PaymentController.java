package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.request.CreateOrderRequestDTO;
import com.mystictarot.backend.dto.response.CreateOrderResponseDTO;
import com.mystictarot.backend.service.PaymentOrderService;
import com.mystictarot.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment and subscription APIs")
public class PaymentController {

    private final PaymentOrderService paymentOrderService;

    @PostMapping("/create-order")
    @Operation(summary = "Create payment order", description = "Creates a payment order for subscription plan (MONTHLY, UNLIMITED, RETAIL_5). Returns transaction ID and payment URL for redirect. Requires JWT.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order created successfully", content = @Content(schema = @Schema(implementation = CreateOrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. FREE plan or validation error)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT")
    })
    public ResponseEntity<CreateOrderResponseDTO> createOrder(@Valid @RequestBody CreateOrderRequestDTO request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CreateOrderResponseDTO response = paymentOrderService.createOrder(userId, request);
        return ResponseEntity.ok(response);
    }
}
