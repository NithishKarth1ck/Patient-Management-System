package com.pm.billingservice.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pm.billingservice.model.BillingAccount;



@Repository
public interface BillingRepository extends JpaRepository<BillingAccount,UUID>{
    
    Optional<BillingAccount>findByPatientId(String patientId);
    boolean existsByPatientId(String patientId);
}
