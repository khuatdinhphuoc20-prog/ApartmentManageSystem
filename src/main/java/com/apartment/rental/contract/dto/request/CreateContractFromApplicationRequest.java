package com.apartment.rental.contract.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateContractFromApplicationRequest(
        @NotNull @Positive Long applicationId,
        @NotNull @Positive Long tenantId,
        @NotNull @Positive Long roomId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate plannedEndDate
) {
}