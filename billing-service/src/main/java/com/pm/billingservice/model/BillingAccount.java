package com.pm.billingservice.model;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "billing_account")
public class BillingAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false ,unique = true)
    private String email;

    
    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

        public String getPatientId() {
        return this.patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
