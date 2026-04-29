package com.pm.billingservice.Repository;

import com.pm.billingservice.enums.AccountStatus;
import com.pm.billingservice.enums.InvoiceStatus;
import com.pm.billingservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice>findByStatus(AccountStatus accountStatus);
    List<Invoice>findByStatusAndDueDateBefore(InvoiceStatus InvoiceStatus, LocalDateTime dueDate);
}
