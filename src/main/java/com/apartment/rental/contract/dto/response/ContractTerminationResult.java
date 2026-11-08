package com.apartment.rental.contract.dto.response;

import com.apartment.rental.contract.entity.FinalSettlementResult;
import java.time.LocalDate;

/** Outcome shown to the Admin after the termination transaction commits. */
public record ContractTerminationResult(
    LocalDate effectiveEndDate,
    FinalSettlementResult finalSettlementResult,
    Long invoiceId,
    boolean regularBillingRequired
) {
}
