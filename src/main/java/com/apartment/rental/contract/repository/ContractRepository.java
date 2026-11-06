package com.apartment.rental.contract.repository;

import com.apartment.rental.contract.entity.Contract;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository owned by the Contract module (M04). */
public interface ContractRepository extends JpaRepository<Contract, Long> {

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
                and c.endReason = com.apartment.rental.contract.entity.ContractEndReason.TENANT_EARLY_EXIT
                and c.finalBillingPeriodEndDate is null)
          )
        order by c.room.roomCode, c.id
        """)
    List<Contract> findEligibleForRegularBilling(@Param("today") LocalDate today);

    @EntityGraph(attributePaths = {"room", "tenant"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contract c where c.id = :id")
    Optional<Contract> findByIdForBillingUpdate(@Param("id") Long id);
}
