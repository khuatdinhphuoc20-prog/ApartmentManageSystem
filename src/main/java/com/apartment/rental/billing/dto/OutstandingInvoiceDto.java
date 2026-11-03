package com.apartment.rental.billing.dto;

import com.apartment.rental.invoice.entity.InvoiceStatus;
import java.math.BigDecimal;

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
