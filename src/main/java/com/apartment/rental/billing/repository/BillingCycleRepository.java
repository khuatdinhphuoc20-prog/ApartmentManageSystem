package com.apartment.rental.billing.repository;

import com.apartment.rental.billing.entity.BillingCycle;
import com.apartment.rental.billing.entity.BillingCycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    Optional<BillingCycle> findByCollectionDate(LocalDate collectionDate);

    List<BillingCycle> findAllByOrderByCollectionDateDesc();

    Optional<BillingCycle> findFirstByStatusOrderByCollectionDateAsc(BillingCycleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BillingCycle c where c.id = :id")
    Optional<BillingCycle> findByIdForUpdate(@Param("id") Long id);
}
