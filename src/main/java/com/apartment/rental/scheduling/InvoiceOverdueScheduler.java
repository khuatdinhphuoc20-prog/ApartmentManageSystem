package com.apartment.rental.scheduling;

import com.apartment.rental.invoice.service.InvoiceOverdueService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvoiceOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvoiceOverdueScheduler.class);

    private final InvoiceOverdueService invoiceOverdueService;

    public InvoiceOverdueScheduler(InvoiceOverdueService invoiceOverdueService) {
        this.invoiceOverdueService = invoiceOverdueService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void checkOverdueInvoices() {
        Instant now = Instant.now();
        try {
            int updatedCount = invoiceOverdueService.markOverdueInvoices(now);
            if (updatedCount > 0) {
                log.info("Marked {} unpaid invoices as overdue at {}", updatedCount, now);
            }
        } catch (RuntimeException exception) {
            log.error("Could not update overdue invoices at {}; the next scheduled run will retry", now, exception);
        }
    }
}
