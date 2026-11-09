package com.apartment.rental.settlement.service;

import com.apartment.rental.contract.dto.ContractSettlementSnapshot;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import com.apartment.rental.settlement.dto.SettlementAction;
import com.apartment.rental.settlement.dto.SettlementCalculationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

/** UC35's deterministic calculator; does not read or change persisted data. */
@Service
public class SettlementCalculator {

    private static final BigDecimal PRORATION_DIVISOR = BigDecimal.valueOf(30);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999999999");


    public SettlementCalculationResult calculate(
        ContractSettlementSnapshot snapshot,
        BigDecimal electricityAmount,
        BigDecimal waterAmount
    ) {
        validateSnapshot(snapshot);
        if (electricityAmount != null) {
            validateAmount(electricityAmount, "Electricity amount", false);
        }
        if (waterAmount != null) {
            validateAmount(waterAmount, "Water amount", false);
        }

        LocalDate usageFrom = snapshot.contractStartDate().isAfter(snapshot.periodStart())
            ? snapshot.contractStartDate() : snapshot.periodStart();
        long usedDays = ChronoUnit.DAYS.between(usageFrom, snapshot.effectiveEndDate());
        BigDecimal monthlyRent = snapshot.agreedMonthlyRent().setScale(0);

        if (snapshot.reason() == ContractEndReason.TENANT_EARLY_EXIT
            && snapshot.contractStartDate().isBefore(snapshot.effectiveEndDate())) {
            if (snapshot.effectiveEndDate().equals(snapshot.periodEnd())) {
                if (electricityAmount != null || waterAmount != null) {
                    throw new IllegalArgumentException(
                        "At a billing-cycle boundary, leave utility amounts blank; M05 handles the regular period"
                    );
                }
                return regularRequired(snapshot, usageFrom, usedDays, monthlyRent);
            }
            if (snapshot.effectiveEndDate().equals(snapshot.periodStart())) {
                throw new IllegalArgumentException(
                    "For a tenant boundary exit after prior usage, supply the preceding period ending on the exit date"
                );
            }
        }

        BigDecimal electricity = validateAmount(electricityAmount, "Electricity amount", false);
        BigDecimal water = validateAmount(waterAmount, "Water amount", false);
        RentCalculationRule rule;
        Integer proratedDays = null;
        BigDecimal rent;

        if (snapshot.reason() == ContractEndReason.TENANT_EARLY_EXIT) {
            rule = RentCalculationRule.TENANT_EARLY_FULL;
            rent = monthlyRent;
        } else {
            rule = RentCalculationRule.ADMIN_STOP_PRORATED;
            if (usedDays > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Prorated usage exceeds the supported day count");
            }
            proratedDays = (int) usedDays;
            rent = monthlyRent.multiply(BigDecimal.valueOf(usedDays))
                .divide(PRORATION_DIVISOR, 0, RoundingMode.HALF_UP);
        }

        BigDecimal total = validateAmount(rent.add(electricity).add(water), "Total amount", false);
        SettlementAction action = total.signum() == 0
            ? SettlementAction.NO_CHARGE : SettlementAction.EARLY_SETTLEMENT_REQUIRED;

        return new SettlementCalculationResult(
            snapshot.contractId(),
            snapshot.reason(),
            snapshot.periodStart(),
            snapshot.effectiveEndDate(),
            usageFrom,
            snapshot.effectiveEndDate(),
            usedDays,
            monthlyRent,
            action,
            rule,
            proratedDays,
            rent,
            electricity,
            water,
            total
        );
    }

    private SettlementCalculationResult regularRequired(
        ContractSettlementSnapshot snapshot,
        LocalDate usageFrom,
        long usedDays,
        BigDecimal monthlyRent
    ) {
        return new SettlementCalculationResult(
            snapshot.contractId(),
            snapshot.reason(),
            snapshot.periodStart(),
            snapshot.effectiveEndDate(),
            usageFrom,
            snapshot.effectiveEndDate(),
            usedDays,
            monthlyRent,
            SettlementAction.REGULAR_REQUIRED,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private void validateSnapshot(ContractSettlementSnapshot snapshot) {
        if (snapshot == null || snapshot.contractId() == null || snapshot.contractId() <= 0) {
            throw new IllegalArgumentException("A snapshot with a positive contract id is required");
        }
        if (snapshot.reason() != ContractEndReason.TENANT_EARLY_EXIT
            && snapshot.reason() != ContractEndReason.ADMIN_STOP_SERVICE) {
            throw new IllegalArgumentException("Invalid contract termination reason");
        }
        if (snapshot.contractStartDate() == null || snapshot.plannedEndDate() == null
            || snapshot.effectiveEndDate() == null || snapshot.periodStart() == null
            || snapshot.periodEnd() == null) {
            throw new IllegalArgumentException("Contract and billing period dates are required");
        }
        if (!snapshot.contractStartDate().isBefore(snapshot.plannedEndDate())
            || snapshot.effectiveEndDate().isBefore(snapshot.contractStartDate())
            || !snapshot.effectiveEndDate().isBefore(snapshot.plannedEndDate())) {
            throw new IllegalArgumentException("The exit date must be within the contract and before its planned end");
        }
        if (!snapshot.periodStart().isBefore(snapshot.periodEnd())
            || snapshot.effectiveEndDate().isBefore(snapshot.periodStart())
            || snapshot.effectiveEndDate().isAfter(snapshot.periodEnd())) {
            throw new IllegalArgumentException("The exit date must fall within the billing period or on its boundary");
        }
        if (!snapshot.contractStartDate().isBefore(snapshot.periodEnd())) {
            throw new IllegalArgumentException("The supplied billing period must extend beyond the contract start date");
        }
        validateAmount(snapshot.agreedMonthlyRent(), "Agreed monthly rent", true);
    }

    private BigDecimal validateAmount(BigDecimal amount, String label, boolean positive) {
        if (amount == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (amount.signum() < 0 || (positive && amount.signum() == 0)) {
            throw new IllegalArgumentException(label + (positive ? " must be positive" : " cannot be negative"));
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(label + " must be a whole VND amount");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(label + " exceeds the supported 19-digit amount");
        }
        return amount.setScale(0);
    }
}
