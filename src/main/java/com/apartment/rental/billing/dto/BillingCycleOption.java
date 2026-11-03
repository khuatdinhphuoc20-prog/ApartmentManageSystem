package com.apartment.rental.billing.dto;

import com.apartment.rental.billing.entity.BillingCycleStatus;
import java.time.LocalDate;

public record BillingCycleOption(Long id, LocalDate collectionDate, BillingCycleStatus status) {
}
