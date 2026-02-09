package com.pm.authservice.dto;

import java.util.UUID;

public class RegisterResponseDTO {
        private final UUID id;
    private final String email;
    private final String message;

    public RegisterResponseDTO(UUID id, String email, String message) {
        this.id = id;
        this.email = email;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }
    
}
