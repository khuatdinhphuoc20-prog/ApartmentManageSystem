package com.apartment.rental.contract.dto.response;

import com.apartment.rental.contract.entity.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(
        Long id,
        Long roomId,
        String roomCode,
        Long tenantId,
        String tenantName,
        String identityDocumentNo,
        String phone,

        BigDecimal agreedMonthlyRent,

        LocalDate startDate,
        LocalDate plannedEndDate,
        LocalDate effectiveEndDate,

        ContractStatus status
) {
}