package com.kenyafintech.connector.service;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FineractClient {

    @Value("${fineract.base-url}")
    private String baseUrl;
    @Value("${fineract.tenant_id}")
    private String tenantId;
    @Value("${fineract.username}")
    private String username;
    @Value("${fineract.password}")
    private String password;

    private final RestTemplate restTemplate;

    public FineractClient() {
        this.restTemplate = createInsecureRestTemplate();
    }

    private RestTemplate createInsecureRestTemplate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            System.err.println("Could not configure trust manager: " + e.getMessage());
        }
        return new RestTemplate();
    }

    public boolean recordSavingsDeposit(Long accountId, Double amount, String transactionId) {
        String url = baseUrl + "/savingsaccounts/" + accountId + "/transactions?command=deposit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", tenantId);
        headers.setBasicAuth(username, password);

        Map<String, Object> body = new HashMap<>();
        body.put("locale", "en");
        body.put("dateFormat", "dd MMMM yyyy");
        body.put("transactionDate",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)));
        body.put("transactionAmount", amount);
        body.put("paymentTypeId", 1); // 1 = Cash/Mobile
        body.put("receiptNumber", transactionId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Fineract Response: " + response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Fineract Deposit Notice: " + e.getMessage());
            return false;
        }
    }
}