package com.apartment.rental.contract.dto;

import com.apartment.rental.contract.entity.ContractEndReason;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only contract data prepared by M04 for M06's final-settlement calculation.
 *
 * <p>The supplied billing period is [periodStart, periodEnd). The effective end
 * date is excluded from usage and may equal either period boundary. Keeping the
 * original contract start date lets the calculator distinguish a same-day early
 * exit from an exit at a boundary after a previously used period.
 *
 * <p>This snapshot does not certify that an invoice was issued or settlement was
 * recorded. Electricity and water amounts must be supplied separately to M06.
 */
public record ContractSettlementSnapshot(
    Long contractId,
    BigDecimal agreedMonthlyRent,
    LocalDate contractStartDate,
    LocalDate plannedEndDate,
    LocalDate effectiveEndDate,
    ContractEndReason reason,
    LocalDate periodStart,
    LocalDate periodEnd
) {
}
