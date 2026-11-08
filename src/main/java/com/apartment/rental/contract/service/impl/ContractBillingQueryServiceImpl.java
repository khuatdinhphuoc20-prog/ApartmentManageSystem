package com.apartment.rental.contract.service.impl;

import com.apartment.rental.contract.dto.response.ContractBillingResponse;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.repository.ContractRepository;
import com.apartment.rental.contract.service.ContractBillingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractBillingQueryServiceImpl
        implements ContractBillingQueryService {

    private final ContractRepository contractRepository;

    @Override
    public List<ContractBillingResponse> findEligibleContractsForRegularBilling(
            LocalDate today
    ) {
        return contractRepository.findEligibleForRegularBilling(today)
                .stream()
                .filter(contract -> canReceiveRegularInvoice(contract, today))
                .map(this::toBillingResponse)
                .toList();
    }

    @Override
    @Transactional
    public Optional<ContractBillingResponse> lockEligibleContractForBilling(
            Long contractId,
            LocalDate today
    ) {
        return contractRepository.findByIdForBillingUpdate(contractId)
                .filter(contract -> canReceiveRegularInvoice(contract, today))
                .map(this::toBillingResponse);
    }

    private boolean canReceiveRegularInvoice(
            Contract contract,
            LocalDate today
    ) {
        boolean regularBillingStatus =
                contract.getStatus() == ContractStatus.ACTIVE

                        || (contract.getStatus() == ContractStatus.ENDED
                        && contract.getEndReason() == ContractEndReason.NATURAL_EXPIRY)

                        || (contract.getStatus() == ContractStatus.ENDED
                        && (contract.getEndReason() == ContractEndReason.TENANT_EARLY_EXIT
                        || contract.getEndReason() == ContractEndReason.ADMIN_STOP_SERVICE));

        LocalDate contractEnd = contract.getEffectiveEndDate() == null
                ? contract.getPlannedEndDate()
                : contract.getEffectiveEndDate();

        boolean endedEarly = contract.getStatus() == ContractStatus.ENDED
                && (contract.getEndReason() == ContractEndReason.TENANT_EARLY_EXIT
                || contract.getEndReason() == ContractEndReason.ADMIN_STOP_SERVICE);

        return regularBillingStatus
                && contractEnd.isAfter(contract.getStartDate())
                && (endedEarly || contract.getStartDate().isBefore(today));
    }

    private ContractBillingResponse toBillingResponse(Contract contract) {
        LocalDate contractEnd = contract.getEffectiveEndDate() == null
                ? contract.getPlannedEndDate()
                : contract.getEffectiveEndDate();

        return new ContractBillingResponse(
                contract.getId(),
                contract.getRoom().getId(),
                contract.getRoom().getRoomCode(),
                contract.getTenant().getId(),
                contract.getTenant().getFullName(),
                contract.getTenant().getEmail(),
                contract.getAgreedMonthlyRent(),
                contract.getStartDate(),
                contractEnd
        );
    }
}
