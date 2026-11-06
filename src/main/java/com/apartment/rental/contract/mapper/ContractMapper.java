package com.apartment.rental.contract.mapper;

import com.apartment.rental.contract.dto.response.ContractDetailResponse;
import com.apartment.rental.contract.dto.response.ContractResponse;
import com.apartment.rental.contract.entity.Contract;
import java.util.List;

import com.apartment.rental.contract.entity.ContractCoOccupant;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    private final ContractCoOccupantMapper coOccupantMapper;

    public ContractMapper(
            ContractCoOccupantMapper coOccupantMapper
    ) {
        this.coOccupantMapper = coOccupantMapper;
    }

    public ContractResponse toResponse(Contract entity) {
        return new ContractResponse(
                entity.getId(),
                entity.getRoom().getId(),
                entity.getRoom().getRoomCode(),
                entity.getTenant().getId(),
                entity.getTenant().getFullName(),
                entity.getTenant().getIdentityDocumentNo(),
                entity.getTenant().getPhone(),
                entity.getAgreedMonthlyRent(),
                entity.getStartDate(),
                entity.getPlannedEndDate(),
                entity.getEffectiveEndDate(),
                entity.getStatus()
        );
    }

    public List<ContractResponse> toResponseList(
            List<Contract> entities
    ) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public ContractDetailResponse toDetailResponse(
            Contract entity,
            List<ContractCoOccupant> coOccupants
    ) {
        return new ContractDetailResponse(
                entity.getId(),
                entity.getApplication() == null
                        ? null
                        : entity.getApplication().getId(),

                entity.getRoom().getId(),
                entity.getRoom().getRoomCode(),

                entity.getTenant().getId(),
                entity.getTenant().getFullName(),
                entity.getTenant().getIdentityDocumentNo(),
                entity.getTenant().getPhone(),

                entity.getAgreedMonthlyRent(),

                entity.getStartDate(),
                entity.getPlannedEndDate(),
                entity.getEffectiveEndDate(),

                entity.getStatus(),
                entity.getEndReason(),

                entity.getActivatedAt(),
                entity.getEndedAt(),
                entity.getCancelledAt(),

                entity.getCreatedByUser().getId(),
                entity.getClosedByUser() == null
                        ? null
                        : entity.getClosedByUser().getId(),

                entity.getClosingNote(),

                entity.getFinalBillingPeriodEndDate(),
                entity.getFinalSettlementResult(),
                entity.getSettledAt(),

                entity.getCreatedAt(),
                entity.getUpdatedAt(),

                coOccupantMapper.toResponseList(coOccupants)
        );
    }


}