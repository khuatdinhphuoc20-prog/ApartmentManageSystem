package com.apartment.rental.settlement.dto;

import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Final-period calculation handed back to M04 for subsequent M05 processing.
 *
 * <p>Usage is [usageFrom, usageTo); the exit date is excluded. usedDays describes
 * actual usage even when the tenant is charged the full monthly rent.
 * proratedDays is populated only for ADMIN_STOP_PRORATED.
 *
 * <p>For REGULAR_REQUIRED, rentCalculationRule, proratedDays and all four amount
 * fields are null: M05 must handle the regular obligation under its own rules.
 * Null amounts must not be interpreted as zero or NO_CHARGE. monthlyRentSnapshot
 * remains available as contract context.
 *
 * <p>EARLY_SETTLEMENT_REQUIRED and NO_CHARGE contain a complete breakdown. M04
 * must still obtain M05's actual processing result before recording settlement
 * or ending the contract. This record is not a persisted invoice.
 */
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
