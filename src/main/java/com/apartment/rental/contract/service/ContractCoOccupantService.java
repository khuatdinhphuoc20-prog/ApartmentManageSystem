package com.apartment.rental.contract.service;

import com.apartment.rental.contract.dto.request.ContractCoOccupantRequest;
import com.apartment.rental.contract.dto.response.ContractCoOccupantResponse;

import java.util.List;

public interface ContractCoOccupantService {

    List<ContractCoOccupantResponse> getCoOccupants(Long contractId);

    ContractCoOccupantResponse addCoOccupant(
            Long contractId,
            ContractCoOccupantRequest request
    );

    ContractCoOccupantResponse updateCoOccupant(
            Long contractId,
            Long coOccupantId,
            ContractCoOccupantRequest request
    );

    void deleteCoOccupant(Long contractId, Long coOccupantId);
}