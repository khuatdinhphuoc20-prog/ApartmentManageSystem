package com.apartment.rental.contract.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractCreateRequest{

        @NotNull(message = "Application ID is required")
        @Positive(message = "Application ID must be positive")
        Long applicationId;

        @NotNull(message = "Start date is required")
        LocalDate startDate;


}