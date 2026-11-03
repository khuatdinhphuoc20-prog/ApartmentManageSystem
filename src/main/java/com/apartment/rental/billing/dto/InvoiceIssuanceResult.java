package com.apartment.rental.billing.dto;

public record InvoiceIssuanceResult(
    int invoicesIssued,
    int noChargeItems,
    boolean cycleCompleted,
    boolean alreadyCompleted
) {
}
