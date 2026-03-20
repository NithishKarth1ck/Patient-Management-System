package com.pm.billingservice.service;

import com.pm.billingservice.Repository.BillingRepository;
import com.pm.billingservice.enums.AccountStatus;
import com.pm.billingservice.model.BillingAccount;

import billing.BillingRequest;
import billing.BillingResponse;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BillingService {
    
    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private final BillingRepository billingRepository;

    public BillingService(BillingRepository billingRepository) {
        this.billingRepository = billingRepository;
    }
    
    public BillingResponse createBillingAccount(BillingRequest request){

        if(billingRepository.existsByPatientId(request.getPatientId())){
            log.warn("BillingAccount already exsists for patientId :{}",
            request.getPatientId());

        BillingAccount existing =  billingRepository
                                    .findByPatientId(request.getPatientId())
                                    .orElseThrow();
       
        return BillingResponse.newBuilder() 
            .setAccountId(existing.getId().toString())
            .setStatus(existing.getStatus().toString())                         
            .build();
    }

    BillingAccount account = new BillingAccount();
    account.setPatientId(request.getPatientId());
    account.setEmail(request.getEmail());
    account.setName(request.getName());
    account.setStatus(AccountStatus.ACTIVE);
    account.setCreatedAt(LocalDateTime.now());

    BillingAccount saved = billingRepository.save(account);
    
    log.info("Billing account created: {} for patientId: {}",
            saved.getId(),saved.getPatientId());
    
    return BillingResponse.newBuilder()
           .setAccountId(saved.getId().toString())
           .setStatus(saved.getStatus().toString())
           .build();
    }
}
