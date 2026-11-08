package com.apartment.rental.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinalBillingData(
        LocalDate usageFrom,
        LocalDate usageTo,
        LocalDate billingPeriodEndDate,
        BigDecimal electricityAmount,
        BigDecimal waterAmount
) {
}