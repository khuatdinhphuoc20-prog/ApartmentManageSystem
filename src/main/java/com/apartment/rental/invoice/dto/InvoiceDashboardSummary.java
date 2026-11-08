package com.apartment.rental.invoice.dto;

import java.math.BigDecimal;

/** Invoice totals across all contracts, grouped by persisted payment status. */
public record InvoiceDashboardSummary(
    BigDecimal paidTotal,
    long unpaidCount,
    long overdueCount
) {
}
