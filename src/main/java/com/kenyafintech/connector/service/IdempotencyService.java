package com.kenyafintech.connector.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    // Thread safety
    // in-memory store to keep processed M-pesa transIds
    private final Map<String, Instant> processedTransactions = new ConcurrentHashMap<>();

    /**
     * Checks if a transaction has already been recorded.
     * If not, marks it as processed.
     * 
     * @param transId Safaricom TransID ( e.g "RKt9...")
     * @return true if it was already processed (duplicate), false if new
     */
    public boolean isDuplicate(String transId) {
        if (transId == null || transId.isBlank()) {
            return false;
        }
        Instant previous = processedTransactions.putIfAbsent(transId, Instant.now());

        return previous != null;
    }

    public int getProcessedCount() {
        return processedTransactions.size();
    }
}
