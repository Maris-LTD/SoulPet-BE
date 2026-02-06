package com.mystictarot.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.dto.webhook.MomoWebhookPayloadDTO;
import com.mystictarot.backend.dto.webhook.ZaloPayCallbackDataDTO;
import com.mystictarot.backend.dto.webhook.ZaloPayWebhookPayloadDTO;
import com.mystictarot.backend.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Payment Webhooks", description = "Callback endpoints for payment providers (Momo, ZaloPay, Stripe). Not for client use.")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/momo")
    @Operation(summary = "Momo IPN callback", description = "Receives payment result from Momo. Signature verified server-side.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or payload")
    })
    public ResponseEntity<Void> momoWebhook(@RequestBody MomoWebhookPayloadDTO payload) {
        if (!paymentWebhookService.verifyMomoSignature(payload)) {
            return ResponseEntity.badRequest().build();
        }
        paymentWebhookService.handleMomoCallback(payload);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/zalopay")
    @Operation(summary = "ZaloPay callback", description = "Receives payment result from ZaloPay. MAC verified server-side.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid MAC or payload")
    })
    public ResponseEntity<String> zaloPayWebhook(@RequestBody ZaloPayWebhookPayloadDTO payload) {
        try {
            String dataStr = payload.getData();
            if (!paymentWebhookService.verifyZaloPayMac(payload, dataStr)) {
                return ResponseEntity.badRequest().body("{\"return_code\":-1,\"return_message\":\"Invalid mac\"}");
            }
            ZaloPayCallbackDataDTO data = objectMapper.readValue(dataStr, ZaloPayCallbackDataDTO.class);
            paymentWebhookService.handleZaloPayCallback(data);
            return ResponseEntity.ok("{\"return_code\":1,\"return_message\":\"success\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"return_code\":-1,\"return_message\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/stripe")
    @Operation(summary = "Stripe webhook", description = "Receives Stripe events (e.g. checkout.session.completed). Signature in header.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature")
    })
    public ResponseEntity<Void> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String stripeSignature) {
        return ResponseEntity.noContent().build();
    }
}
