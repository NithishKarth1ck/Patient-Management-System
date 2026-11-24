package com.pm.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter 
@Setter 
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

    @NotBlank(message = "CAPTCHA token should not be blank")
    private String captchaToken;


}
