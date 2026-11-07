package com.apartment.rental.contract.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractCancellationRequest{

        @Size(max = 1000)
        String closingNote;

}