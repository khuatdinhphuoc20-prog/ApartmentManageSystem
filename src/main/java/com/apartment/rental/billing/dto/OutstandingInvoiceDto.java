package com.apartment.rental.billing.dto;

import com.apartment.rental.invoice.entity.InvoiceStatus;
import java.math.BigDecimal;

/** Data shown for one unpaid or overdue invoice on the admin page. */
public record OutstandingInvoiceDto(
    Long invoiceId,
    String roomCode,
    String tenantName,
    BigDecimal totalAmount,
    InvoiceStatus status,
    String dueAt,
    boolean cashPaymentRequested
) {
}
