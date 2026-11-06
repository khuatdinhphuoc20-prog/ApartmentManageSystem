package com.apartment.rental.billing.repository;

import com.apartment.rental.billing.entity.BillingCycleItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingCycleItemRepository extends JpaRepository<BillingCycleItem, Long> {

    List<BillingCycleItem> findAllByBillingCycle_IdOrderByIdAsc(Long billingCycleId);
}
