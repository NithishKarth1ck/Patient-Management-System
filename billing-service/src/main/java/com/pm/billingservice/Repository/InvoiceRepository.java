package com.pm.billingservice.Repository;

import com.pm.billingservice.enums.AccountStatus;
import com.pm.billingservice.enums.InvoiceStatus;
import com.pm.billingservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice>findByStatus(AccountStatus accountStatus);
    List<Invoice>findByStatusAndDueDateBefore(InvoiceStatus InvoiceStatus, LocalDateTime dueDate);

    @Modifying
    @Transactional
    @Query("UPDATE Invoice i SET i.status = :newStatus WHERE i.status = :currentStatus AND i.dueDate < :now")
    int updateOverdueInvoices(
            @Param("newStatus") InvoiceStatus newStatus,
            @Param("currentStatus") InvoiceStatus currentStatus,
            @Param("now") LocalDateTime now
    );
}
