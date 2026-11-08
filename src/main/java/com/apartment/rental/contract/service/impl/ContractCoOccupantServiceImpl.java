package com.apartment.rental.contract.service.impl;

import com.apartment.rental.contract.dto.request.ContractCoOccupantRequest;
import com.apartment.rental.contract.dto.response.ContractCoOccupantResponse;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractCoOccupant;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.mapper.ContractCoOccupantMapper;
import com.apartment.rental.contract.repository.ContractCoOccupantRepository;
import com.apartment.rental.contract.repository.ContractRepository;
import com.apartment.rental.contract.service.ContractCoOccupantService;

import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.service.RoomService;
import com.apartment.rental.contract.exception.*;


import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ContractCoOccupantServiceImpl
        implements ContractCoOccupantService {

    private final ContractRepository contractRepository;
    private final ContractCoOccupantRepository coOccupantRepository;
    private final ContractCoOccupantMapper coOccupantMapper;
    private final RoomService roomService;


    @Override
    @Transactional(readOnly = true)
    public List<ContractCoOccupantResponse> getCoOccupants(
            Long contractId
    ) {
        if (!contractRepository.existsById(contractId)) {
            throw new ResourceNotFoundException("Contract not found");
        }

        return coOccupantMapper.toResponseList(
                coOccupantRepository.findAllByContract_Id(contractId)
        );
    }

    @Override
    public ContractCoOccupantResponse addCoOccupant(
            Long contractId,
            ContractCoOccupantRequest request
    ) {
        Contract contract = getContract(contractId);

        validateContractEditable(contract);

        String identityDocumentNo =
                normalizeIdentityDocumentNo(request.getIdentityDocumentNo());

        validateDuplicateIdentity(contractId, identityDocumentNo, null);

        validateRoomCapacity(contract);

        ContractCoOccupant coOccupant =
                coOccupantMapper.toEntity(request, contract);

        coOccupant.setIdentityDocumentNo(identityDocumentNo);

        ContractCoOccupant saved =
                coOccupantRepository.save(coOccupant);

        return coOccupantMapper.toResponse(saved);
    }

    @Override
    public ContractCoOccupantResponse updateCoOccupant(
            Long contractId,
            Long coOccupantId,
            ContractCoOccupantRequest request
    ) {
        Contract contract = getContract(contractId);

        validateContractEditable(contract);

        ContractCoOccupant coOccupant = coOccupantRepository
                .findByIdAndContract_Id(coOccupantId, contractId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Co-occupant not found"));

        String identityDocumentNo =
                normalizeIdentityDocumentNo(request.getIdentityDocumentNo());

        validateDuplicateIdentity(
                contractId,
                identityDocumentNo,
                coOccupantId
        );

        coOccupantMapper.updateEntity(coOccupant, request);
        coOccupant.setIdentityDocumentNo(identityDocumentNo);

        ContractCoOccupant updated =
                coOccupantRepository.save(coOccupant);

        return coOccupantMapper.toResponse(updated);
    }

    @Override
    public void deleteCoOccupant(
            Long contractId,
            Long coOccupantId
    ) {
        Contract contract = getContract(contractId);

        validateContractEditable(contract);

        ContractCoOccupant coOccupant = coOccupantRepository
                .findByIdAndContract_Id(coOccupantId, contractId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Co-occupant not found"));

        coOccupantRepository.delete(coOccupant);
    }

    private Contract getContract(Long contractId) {
        return contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Contract not found"));
    }

    private void validateContractEditable(Contract contract) {
        if (contract.getStatus() == ContractStatus.ENDED
                || contract.getStatus() == ContractStatus.CANCELLED) {
            throw new BusinessException(
                    "Cannot modify co-occupants of an ended or cancelled contract"
            );
        }
    }

    private void validateDuplicateIdentity(
            Long contractId,
            String identityDocumentNo,
            Long excludedCoOccupantId
    ) {
        boolean duplicated;

        if (excludedCoOccupantId == null) {
            duplicated = coOccupantRepository
                    .existsByContract_IdAndIdentityDocumentNo(
                            contractId,
                            identityDocumentNo
                    );
        } else {
            duplicated = coOccupantRepository
                    .existsByContract_IdAndIdentityDocumentNoAndIdNot(
                            contractId,
                            identityDocumentNo,
                            excludedCoOccupantId
                    );
        }

        if (duplicated) {
            throw new BusinessException(
                    "Identity document number already exists in this contract"
            );
        }
    }

    private void validateRoomCapacity(Contract contract) {

        RoomCapacityForM04Response roomCapacity =
                roomService.getRoomCapacityForM04 (
                        contract.getRoom().getId()
                );

        Short capacity = roomCapacity.capacity();

        if (capacity == null || capacity <= 0) {
            throw new BusinessException(
                    "Room capacity is unavailable or invalid"
            );
        }

        long coOccupantCount =
                coOccupantRepository.countByContract_Id(contract.getId());

        // +1 là Tenant đại diện hợp đồng
        if (coOccupantCount + 1 >= capacity) {
            throw new BusinessException(
                    "Room capacity has been reached"
            );
        }
    }

    private String normalizeIdentityDocumentNo(String identityDocumentNo) {
        return identityDocumentNo.trim();
    }
}
