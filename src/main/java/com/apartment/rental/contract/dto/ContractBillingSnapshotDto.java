package com.apartment.rental.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read-only contract data needed to prepare a billing-cycle obligation. */
public record ContractBillingSnapshotDto(
    Long contractId,
    Long roomId,
    String tenantName,
    String tenantEmail,
    BigDecimal agreedMonthlyRent,
    LocalDate usageFrom,
    LocalDate usageTo
) {
}
