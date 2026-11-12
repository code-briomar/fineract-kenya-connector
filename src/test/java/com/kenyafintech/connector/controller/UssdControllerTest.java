package com.kenyafintech.connector.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UssdController.class)
class UssdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Initial USSD dial returns main menu with CON prefix")
    void testInitialDialReturnsMainMenu() throws Exception {
        mockMvc.perform(post("/api/v1/ussd")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("sessionId", "s101")
                .param("serviceCode", "*384*100#")
                .param("phoneNumber", "+254712345678")
                .param("text", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CON Welcome to Chama Core Banking")))
                .andExpect(content().string(containsString("1. My Account Balance")));
    }

    @Test
    @DisplayName("Selecting option 1 returns balance with END prefix")
    void testOptionOneReturnsBalance() throws Exception {
        mockMvc.perform(post("/api/v1/ussd")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("sessionId", "s101")
                .param("serviceCode", "*384*100#")
                .param("phoneNumber", "+254712345678")
                .param("text", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("END Chama Account: ACC-CHAMA-001")))
                .andExpect(content().string(containsString("Available Balance: KES 15,250.00")));
    }
}