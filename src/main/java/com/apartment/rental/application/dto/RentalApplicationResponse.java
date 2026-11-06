package com.apartment.rental.application.dto;


import com.apartment.rental.application.entity.RentalApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RentalApplicationResponse(
        Long id,
        Long applicantId,
        String applicantName,
        String applicantEmail,
        String applicantPhone,
        String applicantIdentityDoc,
        Long roomId,
        String roomCode,
        Integer roomFloor,
        BigDecimal roomListedRent,
        LocalDate requestedStartDate,
        LocalDate requestedEndDate,
        RentalApplicationStatus status,
        Instant processedAt,
        Long processedByUserId,
        String processedByName,
        String processingNote,
        Instant createdAt,
        Instant updatedAt
){
    public boolean isCanWithdraw() {
        return this.status == RentalApplicationStatus.PENDING;
    }
}
