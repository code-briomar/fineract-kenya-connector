package com.kenyafintech.connector.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();
    }

    @Test
    @DisplayName("New transaction should not be flagged as duplicate")
    void testNewTransactionIsNotDuplicate() {
        boolean isDuplicate = idempotencyService.isDuplicate("TRANS_001");
        assertFalse(isDuplicate, "First TRANS_001 should not be a duplicate");
    }

    @Test
    @DisplayName("Repeated transaction ID should be flagged as duplicate")
    void testRepeatedTransactionIsDuplicate() {
        idempotencyService.isDuplicate("TRANS_001");
        boolean isDuplicate = idempotencyService.isDuplicate("TRANS_001");

        assertTrue(isDuplicate, "Second TRANS_001 must be flagged as duplicate");
    }

    @Test
    @DisplayName("Different transaction IDs should be tracked independently")
    void testDifferentTransactionsAreIndependent() {
        assertFalse(idempotencyService.isDuplicate("TXN_A"));
        assertFalse(idempotencyService.isDuplicate("TXN_B"));
        assertEquals(2, idempotencyService.getProcessedCount());
    }
}