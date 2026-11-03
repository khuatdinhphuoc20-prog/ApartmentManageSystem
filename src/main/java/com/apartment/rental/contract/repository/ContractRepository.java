package com.apartment.rental.contract.repository;

import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository owned by the Contract module (M04). */
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
}
