package com.kenyafintech.connector.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;

import jakarta.annotation.PostConstruct;

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
                    amount, accountRef, transId);
            List<Recipient> response = sms.send(message, new String[] { "+" + phoneNumber }, true);
            System.out.println("SMS Dispatched: " + response.get(0).status);
        } catch (Exception e) {
            System.err.println("SMS notification failed: " + e.getMessage());
        }
    }
}