package com.apartment.rental.contract.dto.response;

public record ContractCoOccupantResponse(

        Long id,
        Long contractId,
        String fullName,
        String identityDocumentNo,
        String phone

) {
}