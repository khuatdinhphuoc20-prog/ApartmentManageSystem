package com.apartment.rental.settlement.dto;

import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import java.math.BigDecimal;
import java.time.LocalDate;


public record SettlementCalculationResult(
    Long contractId,
    ContractEndReason reason,
    LocalDate periodStart,
    LocalDate billingPeriodEndDate,
    LocalDate usageFrom,
    LocalDate usageTo,
    long usedDays,
    BigDecimal monthlyRentSnapshot,
    SettlementAction action,
    RentCalculationRule rentCalculationRule,
    Integer proratedDays,
    BigDecimal rentAmount,
    BigDecimal electricityAmount,
    BigDecimal waterAmount,
    BigDecimal totalAmount
) {
}
