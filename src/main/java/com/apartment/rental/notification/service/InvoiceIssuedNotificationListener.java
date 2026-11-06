package com.apartment.rental.notification.service;

import com.apartment.rental.invoice.dto.InvoiceIssuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvoiceIssuedNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceIssuedNotificationListener.class);

    private final InvoiceNotificationService notificationService;

    public InvoiceIssuedNotificationListener(InvoiceNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceIssued(InvoiceIssuedEvent event) {
        try {
            notificationService.createInvoiceIssuedNotification(event);
        } catch (RuntimeException exception) {
            // Notification is a soft dependency: keep the issued Invoice committed.
            log.warn("Could not create in-app notification for invoice {}", event.invoiceId(), exception);
        }
    }
}
