package com.apartment.rental.contract.service;

import com.apartment.rental.contract.dto.response.ContractBillingResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractBillingQueryService {

    List<ContractBillingResponse> findEligibleContractsForRegularBilling(
            LocalDate today
    );

    Optional<ContractBillingResponse> lockEligibleContractForBilling(
            Long contractId,
            LocalDate today
    );
}