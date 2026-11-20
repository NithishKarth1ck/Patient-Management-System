package com.pm.patientservice.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendPatientRegistrationEmail(
            String patientName,
            String patientEmail) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(patientEmail);
        message.setSubject("Patient Registration Successful");
        message.setText(
                "Hello " + patientName + ",\n\n" +
                "Your patient registration was successful.\n\n" +
                "Thank you."
        );

        javaMailSender.send(message);
    }
}