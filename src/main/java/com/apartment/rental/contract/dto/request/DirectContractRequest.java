
package com.apartment.rental.contract.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectContractRequest {

        @NotNull
        @Positive
        Long tenantId;

        @NotNull
        @Positive
        Long roomId;

        @NotNull
        LocalDate startDate;

        @NotNull
        @Min(1)
        @Max(12)
        Integer endMonth;

        @NotNull
        @Positive
        Integer endYear;

        @Size(max = 20)
        @Builder.Default
        List<ContractCoOccupantRequest> coOccupants = new ArrayList<>();
}
