package com.pm.billingservice.service;

import com.pm.billingservice.Repository.BillingRepository;
import com.pm.billingservice.Repository.InvoiceRepository;
import com.pm.billingservice.enums.InvoiceStatus;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final BillingRepository billingRepository;


    public InvoiceService(InvoiceRepository invoiceRepository, BillingRepository billingRepository){
        this.invoiceRepository = invoiceRepository;
        this.billingRepository = billingRepository;
    }


    public void createInvoice(UUID billingAccountId, BigDecimal amount, String description, LocalDateTime dueDate, LocalDateTime createdAt){
        BillingAccount account = billingRepository.findById(billingAccountId)
                .orElseThrow(() -> new RuntimeException("Billing account not found: " + billingAccountId));

             Invoice invoice = new Invoice();
             invoice.setBillingAccount(account);
             invoice.setAmount(amount);
             invoice.setDueDate(dueDate);
             invoice.setDescription(description);
             invoice.setStatus(InvoiceStatus.PENDING);

             Invoice saved = invoiceRepository.save(invoice);

             log.info("Invoice created: {} for billingAccountId: {}",saved.getId(),billingAccountId);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void markOverdueInvoices(){
    int update = invoiceRepository.updateOverdueInvoices(
            InvoiceStatus.OVERDUE,
            InvoiceStatus.PENDING,
            LocalDateTime.now()
    );
    log.info("Marked {} invoices as OVERDUE", update);
    }
}
