package com.kenyafintech.connector.controller;

import com.kenyafintech.connector.model.MpesaC2BPayload;
import com.kenyafintech.connector.service.FineractClient;
import com.kenyafintech.connector.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mpesa")
public class MpesaWebhookController {

    private final FineractClient fineractClient;
    private final NotificationService notificationService;

    public MpesaWebhookController(FineractClient fineractClient, NotificationService notificationService) {
        this.fineractClient = fineractClient;
        this.notificationService = notificationService;
    }

    // Safaricom C2B Validation hook
    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> validatePayment(@RequestBody MpesaC2BPayload payload) {
        // Return 0 to accept the payment
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Accepted"));
    }

    // Safaricom C2B Confirmation hook (actual payment completed)
    @PostMapping("/confirmation")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody MpesaC2BPayload payload) {
        System.out
                .println("Processing M-Pesa payment: " + payload.getTransId() + " of KES " + payload.getTransAmount());

        // 1. Post to Fineract Core Banking
        // (For demo/testing, mapping BillRefNumber to account ID 1L)
        fineractClient.recordSavingsDeposit(1L, Double.parseDouble(payload.getTransAmount()), payload.getTransId());

        // 2. Dispatch Africa's Talking SMS receipt
        notificationService.sendDepositReceipt(payload.getMsisdn(), payload.getTransAmount(),
                payload.getBillRefNumber(), payload.getTransId());

        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Success"));
    }
}