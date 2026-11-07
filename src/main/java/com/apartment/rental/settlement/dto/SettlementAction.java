package com.apartment.rental.settlement.dto;

/** Calculation decisions for M04; none of these values certify a database write. */
public enum SettlementAction {
    EARLY_SETTLEMENT_REQUIRED,
    NO_CHARGE,
    REGULAR_REQUIRED
}
