package com.kenyafintech.connector.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kenyafintech.connector.model.MpesaC2BPayload;
import com.kenyafintech.connector.service.FineractClient;
import com.kenyafintech.connector.service.IdempotencyService;
import com.kenyafintech.connector.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MpesaWebhookController.class)
class MpesaWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FineractClient fineractClient;

    @MockBean
    private NotificationService notificationService;

    @SpyBean
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("Validation rejects invalid account reference with C2B00012")
    void testValidationRejectsInvalidAccount() throws Exception {
        when(fineractClient.resolveSavingsAccountId("UNKNOWN_ACC")).thenReturn(Optional.empty());

        MpesaC2BPayload payload = new MpesaC2BPayload();
        payload.setBillRefNumber("UNKNOWN_ACC");

        mockMvc.perform(post("/api/v1/mpesa/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResultCode").value("C2B00012"))
                .andExpect(jsonPath("$.ResultDesc").value("Rejected: Account number does not exist"));
    }

    @Test
    @DisplayName("Confirmation processes deposit and dispatches SMS for valid payment")
    void testConfirmationProcessesSuccessfully() throws Exception {
        when(fineractClient.resolveSavingsAccountId("ACC-001")).thenReturn(Optional.of(10L));
        when(fineractClient.recordSavingsDeposit(eq(10L), eq(1000.0), eq("TXN_100"))).thenReturn(true);

        MpesaC2BPayload payload = new MpesaC2BPayload();
        payload.setTransId("TXN_100");
        payload.setTransAmount("1000.0");
        payload.setBillRefNumber("ACC-001");
        payload.setMsisdn("254712345678");

        mockMvc.perform(post("/api/v1/mpesa/confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResultCode").value(0))
                .andExpect(jsonPath("$.ResultDesc").value("Success"));

        verify(fineractClient, times(1)).recordSavingsDeposit(10L, 1000.0, "TXN_100");
        verify(notificationService, times(1)).sendDepositReceipt("254712345678", "1000.0", "ACC-001", "TXN_100");
    }

    @Test
    @DisplayName("Duplicate confirmation is intercepted by Idempotency check")
    void testDuplicateConfirmationIsSuppressed() throws Exception {
        MpesaC2BPayload payload = new MpesaC2BPayload();
        payload.setTransId("TXN_DUP");
        payload.setTransAmount("500.0");
        payload.setBillRefNumber("ACC-001");
        payload.setMsisdn("254712345678");

        mockMvc.perform(post("/api/v1/mpesa/confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResultDesc").value("Success"));

        // (duplicate)
        mockMvc.perform(post("/api/v1/mpesa/confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResultDesc").value("Duplicate transaction acknowledged"));

        // Verify Fineract was only called once, not twice!
        verify(fineractClient, times(1)).recordSavingsDeposit(anyLong(), anyDouble(), eq("TXN_DUP"));
    }
}