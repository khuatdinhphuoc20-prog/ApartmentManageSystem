package com.apartment.rental.contract.repository;

import com.apartment.rental.contract.entity.ContractCoOccupant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractCoOccupantRepository
        extends JpaRepository<ContractCoOccupant, Long> {

    List<ContractCoOccupant> findAllByContract_Id(Long contractId);

    Optional<ContractCoOccupant> findByIdAndContract_Id(
            Long id,
            Long contractId
    );

    boolean existsByContract_IdAndIdentityDocumentNo(
            Long contractId,
            String identityDocumentNo
    );

    boolean existsByContract_IdAndIdentityDocumentNoAndIdNot(
            Long contractId,
            String identityDocumentNo,
            Long id
    );

    long countByContract_Id(Long contractId);
}