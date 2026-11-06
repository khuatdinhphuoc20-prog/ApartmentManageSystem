package com.apartment.rental.contract.mapper;

import com.apartment.rental.contract.dto.request.ContractCoOccupantRequest;
import com.apartment.rental.contract.dto.response.ContractCoOccupantResponse;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractCoOccupant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ContractCoOccupantMapper {

    public ContractCoOccupant toEntity(
            ContractCoOccupantRequest request,
            Contract contract
    ) {
        ContractCoOccupant coOccupant = new ContractCoOccupant();

        coOccupant.setContract(contract);
        coOccupant.setFullName(request.getFullName().trim());
        coOccupant.setIdentityDocumentNo(
                request.getIdentityDocumentNo().trim()
        );
        coOccupant.setPhone(
                request.getPhone() == null ? null : request.getPhone().trim()
        );

        return coOccupant;
    }

    public ContractCoOccupantResponse toResponse(
            ContractCoOccupant entity
    ) {
        return new ContractCoOccupantResponse(
                entity.getId(),
                entity.getContract().getId(),
                entity.getFullName(),
                entity.getIdentityDocumentNo(),
                entity.getPhone()
        );
    }

    public List<ContractCoOccupantResponse> toResponseList(
            List<ContractCoOccupant> entities
    ) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public void updateEntity(
            ContractCoOccupant entity,
            ContractCoOccupantRequest request
    ) {
        entity.setFullName(request.getFullName().trim());
        entity.setIdentityDocumentNo(
                request.getIdentityDocumentNo().trim()
        );
        entity.setPhone(
                request.getPhone() == null ? null : request.getPhone().trim()
        );
    }
}