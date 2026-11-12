package com.kenyafintech.connector.controller;

import com.kenyafintech.connector.model.MpesaC2BPayload;
import com.kenyafintech.connector.service.FineractClient;
import com.kenyafintech.connector.service.IdempotencyService;
import com.kenyafintech.connector.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/mpesa")
public class MpesaWebhookController {

    private final FineractClient fineractClient;
    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;

    public MpesaWebhookController(
            FineractClient fineractClient,
            NotificationService notificationService,
            IdempotencyService idempotencyService) {
        this.fineractClient = fineractClient;
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> validatePayment(@RequestBody MpesaC2BPayload payload) {
        log.info("C2B validation request received for ref: {}", payload.getBillRefNumber());

        Optional<Long> accountIdOpt = fineractClient.resolveSavingsAccountId(payload.getBillRefNumber());
        if (accountIdOpt.isEmpty()) {
            log.warn("Validation rejected: account not found for ref {}", payload.getBillRefNumber());
            return ResponseEntity.ok(Map.of(
                    "ResultCode", "C2B00012",
                    "ResultDesc", "Rejected: Account number does not exist"
            ));
        }

        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Accepted"));
    }

    @PostMapping("/confirmation")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody MpesaC2BPayload payload) {
        String transId = payload.getTransId();
        log.info("Processing C2B confirmation: txn={}, amount=KES {}", transId, payload.getTransAmount());

        if (idempotencyService.isDuplicate(transId)) {
            log.warn("Duplicate webhook ignored for txn: {}", transId);
            return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Duplicate transaction acknowledged"));
        }

        Long accountId = fineractClient.resolveSavingsAccountId(payload.getBillRefNumber()).orElse(1L);
        fineractClient.recordSavingsDeposit(accountId, Double.parseDouble(payload.getTransAmount()), transId);

        notificationService.sendDepositReceipt(
                payload.getMsisdn(),
                payload.getTransAmount(),
                payload.getBillRefNumber(),
                transId
        );

        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Success"));
    }
}
