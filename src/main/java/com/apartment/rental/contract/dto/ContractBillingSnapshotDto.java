package com.apartment.rental.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read-only contract data M05 needs to prepare an individual billing period. */
public record ContractBillingSnapshotDto(
    Long contractId,
    Long roomId,
    String roomCode,
    Long tenantId,
    String tenantName,
    String tenantEmail,
    BigDecimal agreedMonthlyRent,
    LocalDate startDate,
    LocalDate contractEndDate
) {
}
