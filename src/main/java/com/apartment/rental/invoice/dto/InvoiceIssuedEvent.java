package com.apartment.rental.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Event payload published after a regular Invoice is committed. */
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
