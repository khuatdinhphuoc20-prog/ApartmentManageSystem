package com.apartment.rental.contract.dto.response;

import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.entity.FinalSettlementResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContractDetailResponse(
        Long id,
        Long applicationId,

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

        ContractStatus status,
        ContractEndReason endReason,

        Instant activatedAt,
        Instant endedAt,
        Instant cancelledAt,

        Long createdByUserId,
        Long closedByUserId,

        String closingNote,

        LocalDate finalBillingPeriodEndDate,
        FinalSettlementResult finalSettlementResult,
        Instant settledAt,

        Instant createdAt,
        Instant updatedAt,

        List<ContractCoOccupantResponse> coOccupants
) {
}