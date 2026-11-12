package com.kenyafintech.connector.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ussd")
public class UssdController {

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String handleUssd(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("serviceCode") String serviceCode,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", defaultValue = "") String text) {

        String response;

        if (text.isEmpty()) {
            // Main menu
            response = "CON Welcome to Chama Core Banking\n"
                    + "1. My Account Balance\n"
                    + "2. Mini Statement\n"
                    + "3. Help & Support";
        } else if (text.equals("1")) {
            // Option 1: Balance Check (simulating query to Fineract for this member)
            response = "END Chama Account: ACC-CHAMA-001\n"
                    + "Available Balance: KES 15,250.00\n"
                    + "Core Banking Status: Active";
        } else if (text.equals("2")) {
            // Option 2: Mini Statement
            response = "END Mini Statement (Last 2 Txns):\n"
                    + "1. +KES 1,500 (M-Pesa RKT978HH7K)\n"
                    + "2. -KES 500 (Withdrawal ATM)";
        } else if (text.equals("3")) {
            // Option 3: Support
            response = "END For assistance, contact Chama Treasurer at 0700000000 or email support@chama.co.ke";
        } else {
            response = "END Invalid choice. Please try again.";
        }

        return response;
    }
}