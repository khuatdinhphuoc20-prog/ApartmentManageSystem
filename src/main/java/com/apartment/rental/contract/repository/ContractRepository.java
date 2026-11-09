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


    @EntityGraph(attributePaths = {"room", "tenant"})
    @Query("""
        select c from Contract c
        where c.startDate < :today
          and coalesce(c.effectiveEndDate, c.plannedEndDate) > c.startDate
          and (
            c.status = com.apartment.rental.contract.entity.ContractStatus.ACTIVE
            or (c.status = com.apartment.rental.contract.entity.ContractStatus.ENDED
                and c.endReason = com.apartment.rental.contract.entity.ContractEndReason.NATURAL_EXPIRY)
            or (c.status = com.apartment.rental.contract.entity.ContractStatus.ENDED
                and c.endReason in (
                    com.apartment.rental.contract.entity.ContractEndReason.TENANT_EARLY_EXIT,
                    com.apartment.rental.contract.entity.ContractEndReason.ADMIN_STOP_SERVICE
                ))
          )
        order by c.room.roomCode, c.id
        """)
    List<Contract> findEligibleForRegularBilling(@Param("today") LocalDate today);

    @EntityGraph(attributePaths = {"room", "tenant"})
    @Query("select c from Contract c where c.id = :id")
    Optional<Contract> findByIdForBillingUpdate(@Param("id") Long id);

    // Contract access and lifecycle queries.
    List<Contract> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<Contract> findByIdAndTenant_Id(Long contractId, Long tenantId);

    List<Contract> findAllByRoom_IdOrderByCreatedAtDesc(Long roomId);

    Optional<Contract> findByApplication_Id(Long applicationId);

    boolean existsByRoom_IdAndStatusIn(Long roomId, Collection<ContractStatus> statuses);

    List<Contract> findAllByStatusAndStartDateLessThanEqual(ContractStatus status, LocalDate date);


    @Query("SELECT c FROM Contract c WHERE c.id = :contractId")
    Optional<Contract> findByIdForUpdate(@Param("contractId") Long contractId);

    List<Contract> findAllByStatusAndPlannedEndDateLessThanEqual(ContractStatus status, LocalDate date);

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

    @EntityGraph(attributePaths = {"room"})
    List<Contract> findAllByTenant_IdAndStatusOrderByRoom_RoomCodeAsc(Long tenantId, ContractStatus status);

    @EntityGraph(attributePaths = {"room"})
    Optional<Contract> findByIdAndTenant_IdAndStatus(Long contractId, Long tenantId, ContractStatus status);
}
