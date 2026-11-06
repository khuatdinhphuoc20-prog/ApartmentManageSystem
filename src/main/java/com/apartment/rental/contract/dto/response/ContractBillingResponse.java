package com.apartment.rental.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;


public record ContractBillingResponse(
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
