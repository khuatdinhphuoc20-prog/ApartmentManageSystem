package com.apartment.rental.billing.dto;

import com.apartment.rental.billing.entity.BillingCycle;
import java.util.List;

/** Data shown on the admin page for a billing cycle. */
public record BillingCyclePageDto(
    List<BillingCycle> cycles,
    BillingCycle selectedCycle,
    BillingCycleItemsForm form,
    boolean canIssue
) {
}
