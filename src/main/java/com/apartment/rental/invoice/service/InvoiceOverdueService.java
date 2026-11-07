package com.apartment.rental.invoice.service;

import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceOverdueService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceOverdueService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public int markOverdueInvoices(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Processing time is required");
        }
        return invoiceRepository.markUnpaidInvoicesOverdue(
            now.truncatedTo(ChronoUnit.MILLIS), InvoiceStatus.UNPAID, InvoiceStatus.OVERDUE
        );
    }
}
