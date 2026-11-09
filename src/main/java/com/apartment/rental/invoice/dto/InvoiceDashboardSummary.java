package com.apartment.rental.invoice.dto;

import java.math.BigDecimal;


public record InvoiceDashboardSummary(
    BigDecimal paidTotal,
    long unpaidCount,
    long overdueCount
) {
}
