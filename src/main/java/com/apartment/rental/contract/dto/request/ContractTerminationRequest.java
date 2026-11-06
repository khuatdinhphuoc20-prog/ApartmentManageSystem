package com.apartment.rental.contract.dto.request;

import com.apartment.rental.contract.entity.ContractEndReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTerminationRequest{

        @NotNull(message = "Termination reason is required")
        ContractEndReason reason;

        @Size(max = 1000)
        String closingNote;


}