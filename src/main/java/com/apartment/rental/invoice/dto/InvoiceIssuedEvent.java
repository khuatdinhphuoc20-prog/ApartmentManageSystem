package com.apartment.rental.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;


public record InvoiceIssuedEvent(
    Long invoiceId,
    Long recipientUserId,
    Long contractId,
    String roomCode,
    LocalDate billingPeriodEndDate,
    BigDecimal totalAmount,
    Instant dueAt
) {
}
