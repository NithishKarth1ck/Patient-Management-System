package com.pm.patientservice.dto;

import com.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class PatientRequestDTO {
    @NotBlank(message = "name is required")
    @Size(max = 100,message = "Name cannot exceed 100 characters ")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "enter a valid email")
    private String email;

    @NotBlank(message = "phone number is required") 
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "DateOfBirth is required")
    private String dateOfBirth;

    @NotBlank(groups = CreatePatientValidationGroup.class,message = "Registered date is required" )
    private String registeredDate;

}

