package com.kenyafintech.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MpesaC2BPayload {
    @JsonProperty("TransactionType")
    private String transactionType;

    @JsonProperty("TransID")
    private String transId; // e.g "SDF492K12"

    @JsonProperty("TransTime")
    private String transTime;

    @JsonProperty("TransAmount")
    private String transAmount;

    @JsonProperty("BusinessShortCode")
    private String businessShortCode;

    @JsonProperty("BillRefNumber")
    private String billRefNumber; // Member's SACCO/Chama Acc No.

    @JsonProperty("MSISDN")
    private String msisdn; // Phone Number "2547123..."

    @JsonProperty("FirstName")
    private String firstName;
}