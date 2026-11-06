package com.apartment.rental.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingPeriodRowDto(
    Long contractId,
    Long periodId,
    String roomCode,
    String tenantName,
    String tenantEmail,
    LocalDate usageFrom,
    LocalDate usageTo,
    LocalDate billingPeriodEndDate,
    BigDecimal agreedMonthlyRent,
    BigDecimal electricityAmount,
    BigDecimal waterAmount,
    String processingResult
) {
}
