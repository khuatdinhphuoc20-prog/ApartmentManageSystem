package com.apartment.rental.billing.dto;

/** Summary returned after the system processes a billing cycle. */
public record InvoiceIssuanceResult(
    int invoicesIssued,
    int noChargeItems,
    boolean cycleCompleted,
    boolean alreadyCompleted
) {
}
