package com.kenyafintech.connector.controller;

import com.kenyafintech.connector.model.MpesaC2BPayload;
import com.kenyafintech.connector.service.FineractClient;
import com.kenyafintech.connector.service.IdempotencyService;
import com.kenyafintech.connector.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/mpesa")
public class MpesaWebhookController {

    private final FineractClient fineractClient;
    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;

    public MpesaWebhookController(FineractClient fineractClient,
            NotificationService notificationService,
            IdempotencyService idempotencyService) {
        this.fineractClient = fineractClient;
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Safaricom C2B Validation hook:
     * Validates whether the account exists in Fineract BEFORE money leaves the
     * customer's phone!
     */
    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> validatePayment(@RequestBody MpesaC2BPayload payload) {
        System.out.println("Validating C2B payment for Account Ref: " + payload.getBillRefNumber());

        Optional<Long> accountIdOpt = fineractClient.resolveSavingsAccountId(payload.getBillRefNumber());

        if (accountIdOpt.isEmpty()) {
            System.out.println("Validation REJECTED: Account not found in Fineract Core Banking.");
            // Safaricom standard C2B rejection code
            return ResponseEntity.ok(Map.of(
                    "ResultCode", "C2B00012",
                    "ResultDesc", "Rejected: Account number does not exist"));
        }

        System.out.println("Validation ACCEPTED for Fineract Account ID: " + accountIdOpt.get());
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Accepted"));
    }

    /**
     * Safaricom C2B Confirmation hook:
     * Protects against duplicate replays and posts to core banking.
     */
    @PostMapping("/confirmation")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody MpesaC2BPayload payload) {
        String transId = payload.getTransId();
        System.out
                .println("Processing M-Pesa payment confirmation: " + transId + " of KES " + payload.getTransAmount());

        // 1. Idempotency Check: Protect against duplicate webhook replays
        if (idempotencyService.isDuplicate(transId)) {
            System.out.println("⚠️ Replay detected! Transaction " + transId
                    + " was already processed. Acknowledging Safaricom safely.");
            return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Duplicate transaction acknowledged"));
        }

        // 2. Dynamic Account Resolution
        Optional<Long> accountIdOpt = fineractClient.resolveSavingsAccountId(payload.getBillRefNumber());
        Long accountId = accountIdOpt.orElse(1L);

        // 3. Post to Fineract Core Banking Ledger
        fineractClient.recordSavingsDeposit(accountId, Double.parseDouble(payload.getTransAmount()), transId);

        // 4. Dispatch Africa's Talking SMS receipt
        notificationService.sendDepositReceipt(payload.getMsisdn(), payload.getTransAmount(),
                payload.getBillRefNumber(), transId);

        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Success"));
    }
}