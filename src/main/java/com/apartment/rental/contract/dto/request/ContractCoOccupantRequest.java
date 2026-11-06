package com.apartment.rental.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractCoOccupantRequest{

        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName;

        @NotBlank(message = "Identity document number is required")
        @Size(max = 30)
        String identityDocumentNo;

        @Size(max = 20)
        String phone;

}