package com.apartment.rental.dashboard.dto;

import java.math.BigDecimal;

/** Read-only figures shown on the admin dashboard. */
public record AdminDashboardSummary(
    long totalRooms,
    long availableRooms,
    long rentedRooms,
    long inactiveRooms,
    BigDecimal availablePercent,
    BigDecimal rentedPercent,
    BigDecimal inactivePercent,
    BigDecimal paidTotal,
    long unpaidInvoices,
    long overdueInvoices,
    long outstandingInvoices,
    long unresolvedIssues
) {
}
