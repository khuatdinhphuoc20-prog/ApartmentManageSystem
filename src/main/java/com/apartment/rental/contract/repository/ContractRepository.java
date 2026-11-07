package com.apartment.rental.contract.repository;

import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository owned by the Contract module (M04). */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @EntityGraph(attributePaths = {"room", "tenant"})
    @Query("""
        select c from Contract c
        where c.status <> :cancelled
          and c.startDate < :periodEnd
          and coalesce(c.effectiveEndDate, c.plannedEndDate) > :periodStart
          and (c.finalBillingPeriodEndDate is null or c.finalBillingPeriodEndDate <> :periodEnd)
        order by c.room.roomCode, c.id
        """)
    List<Contract> findBillingCandidates(
        @Param("periodStart") LocalDate periodStart,
        @Param("periodEnd") LocalDate periodEnd,
        @Param("cancelled") ContractStatus cancelled
    );

    // Find contracts by tenant
    List<Contract> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    // Find contract belonging to a specific tenant
    Optional<Contract> findByIdAndTenant_Id(Long contractId, Long tenantId);

    // Find contracts by room
    List<Contract> findAllByRoom_IdOrderByCreatedAtDesc(Long roomId);

    // Find contract created from an application
    Optional<Contract> findByApplication_Id(Long applicationId);

    // Check whether room has a SCHEDULED or ACTIVE contract
    boolean existsByRoom_IdAndStatusIn(
            Long roomId,
            Collection<ContractStatus> statuses
    );

    // Find scheduled contracts that should be activated
    List<Contract> findAllByStatusAndStartDateLessThanEqual(
            ContractStatus status,
            LocalDate date
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contract c WHERE c.id = :contractId")
    Optional<Contract> findByIdForUpdate(
            @Param("contractId") Long contractId
    );
    // Find active contracts that should be ended
    List<Contract> findAllByStatusAndPlannedEndDateLessThanEqual(
            ContractStatus status,
            LocalDate date
    );
    // Get all contracts for Admin
    List<Contract> findAllByOrderByCreatedAtDesc();
    @Query("""
    SELECT c
    FROM Contract c
    WHERE (:keyword IS NULL
           OR LOWER(c.tenant.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:status IS NULL OR c.status = :status)
    ORDER BY c.startDate DESC, c.createdAt DESC
    """)
    List<Contract> searchContractsForAdmin(
            @Param("keyword") String keyword,
            @Param("status") ContractStatus status
    );
}
