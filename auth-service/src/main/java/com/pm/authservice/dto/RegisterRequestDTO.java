package com.pm.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {
    @NotBlank(message = "Email should not be blank")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Password should not be blank")
    @Size(min = 8, message = "Password should have at least 8 characters")
    private String password;

    @NotBlank(message = "Name should not be blank")
    @Size(min = 2, max = 100, message = "Name should be between 2 and 100 characters")
    private String name;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
