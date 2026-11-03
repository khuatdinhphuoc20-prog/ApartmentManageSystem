package com.apartment.rental.billing.dto;

import java.util.List;

public record BillingCyclePage(
    List<BillingCycleOption> cycles,
    BillingCycleOption selectedCycle,
    BillingCycleItemsForm form,
    boolean canIssue
) {
}
