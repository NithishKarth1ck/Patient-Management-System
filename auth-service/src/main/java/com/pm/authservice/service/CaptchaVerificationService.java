package com.pm.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.pm.authservice.dto.CaptchaVerificationResponse;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.MediaType;

@Service 
public class CaptchaVerificationService {
    
    @Value("${recaptcha.secret-key}")
    private String secretKey;
    private final RestClient restClient;

    public boolean verifyToken(String captchaToken) {
        MultiValueMap<String, String> formData =
        new LinkedMultiValueMap<>();

        formData.add("secret", secretKey);
        formData.add("response", captchaToken);

        CaptchaVerificationResponse response = restClient
                .post()
                .uri("https://www.google.com/recaptcha/api/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(CaptchaVerificationResponse.class);

        return response.isSuccess();
    
    }

    public CaptchaVerificationService(RestClient.Builder builder) {
    this.restClient = builder.build();
}
}
