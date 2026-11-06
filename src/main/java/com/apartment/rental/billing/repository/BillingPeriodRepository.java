package com.apartment.rental.billing.repository;

import com.apartment.rental.billing.entity.BillingPeriod;
import java.time.LocalDate;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, Long> {

    Optional<BillingPeriod> findByContract_IdAndBillingPeriodEndDate(Long contractId, LocalDate periodEndDate);

    Optional<BillingPeriod> findTopByContract_IdOrderByBillingPeriodEndDateDescIdDesc(Long contractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select period from BillingPeriod period where period.contract.id = :contractId and period.billingPeriodEndDate = :periodEndDate")
    Optional<BillingPeriod> findByContract_IdAndBillingPeriodEndDateForUpdate(
        @Param("contractId") Long contractId,
        @Param("periodEndDate") LocalDate periodEndDate
    );
}
