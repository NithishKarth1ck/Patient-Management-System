package com.pm.patientservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class SmsService {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;

    public SmsService(
            @Value("${fast2sms.api.url}") String apiUrl,
            @Value("${fast2sms.api.key}") String apiKey) {

        this.apiUrl = apiUrl;
        this.apiKey = apiKey;

        this.restClient = RestClient.builder().build();
    }

    public void sendRegistrationSms(String patientName, String phoneNumber) {

        String message = "Hello " + patientName
                + ", your registration with Patient Management System was successful. Thank you.";

        Map<String, Object> requestBody = Map.of(
                "route", "q",
                "message", message,
                "numbers", phoneNumber
        );

        Map response = restClient.post()
                .uri(apiUrl)
                .header("Authorization", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null || !Boolean.TRUE.equals(response.get("return"))) {
            throw new RuntimeException("Fast2SMS failed to send the registration SMS.");
        }
    }
}