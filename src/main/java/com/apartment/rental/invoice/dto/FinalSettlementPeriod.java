package com.apartment.rental.invoice.dto;

import java.time.LocalDate;


public record FinalSettlementPeriod(LocalDate periodStart, LocalDate periodEnd) {
}
