package com.pm.billingservice.controller;

import com.pm.billingservice.service.BillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Billing")
public class BillingController {
    private BillingService billingService;


    public BillingController(BillingService billingService){
        this.billingService = billingService;
    }


    @GetMapping("/account/{PatientId}")
    public {

    }
}
