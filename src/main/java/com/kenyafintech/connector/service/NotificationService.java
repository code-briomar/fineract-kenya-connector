package com.kenyafintech.connector.service;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Value("${africastalking.username}")
    private String username;

    @Value("${africastalking.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        AfricasTalking.initialize(username, apiKey);
    }

    public void sendDepositReceipt(String phoneNumber, String amount, String accountRef, String transId) {
        try {
            SmsService sms = AfricasTalking.getService(SmsService.class);
            String message = String.format(
                    "Confirmed! KES %s received for Account %s via M-Pesa Ref: %s. Your SACCO ledger balance has been updated.",
                    amount, accountRef, transId
            );
            List<Recipient> response = sms.send(message, new String[]{"+" + phoneNumber}, true);
            if (!response.isEmpty()) {
                log.info("SMS dispatched to {} (status: {})", phoneNumber, response.get(0).status);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
