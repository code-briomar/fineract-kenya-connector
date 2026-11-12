package com.kenyafintech.connector.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Map<String, Instant> processedTransactions = new ConcurrentHashMap<>();

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
