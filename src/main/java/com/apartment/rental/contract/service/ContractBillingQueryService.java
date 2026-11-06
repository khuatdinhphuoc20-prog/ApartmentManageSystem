package com.apartment.rental.contract.service;

import com.apartment.rental.contract.dto.ContractBillingSnapshotDto;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.repository.ContractRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds eligible contracts and exposes only the data needed by regular billing. */
@Service
public class ContractBillingQueryService {

    private final ContractRepository contractRepository;

    public ContractBillingQueryService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<ContractBillingSnapshotDto> findEligibleContractsForRegularBilling(LocalDate today) {
        return contractRepository.findEligibleForRegularBilling(today)
            .stream()
            .filter(contract -> canReceiveRegularInvoice(contract, today))
            .map(this::toSnapshot)
            .toList();
    }

    @Transactional
    public Optional<ContractBillingSnapshotDto> lockEligibleContractForBilling(Long contractId, LocalDate today) {
        return contractRepository.findByIdForBillingUpdate(contractId)
            .filter(contract -> canReceiveRegularInvoice(contract, today))
            .map(this::toSnapshot);
    }

    private boolean canReceiveRegularInvoice(Contract contract, LocalDate today) {
        boolean regularBillingStatus = contract.getStatus() == ContractStatus.ACTIVE
            || (contract.getStatus() == ContractStatus.ENDED
                && contract.getEndReason() == ContractEndReason.NATURAL_EXPIRY)
            || (contract.getStatus() == ContractStatus.ENDED
                && contract.getEndReason() == ContractEndReason.TENANT_EARLY_EXIT
                && contract.getFinalBillingPeriodEndDate() == null
                && isContractAnniversary(contract.getStartDate(), contract.getEffectiveEndDate()));
        return regularBillingStatus
            && contract.getStartDate().isBefore(today)
            && (contract.getEffectiveEndDate() == null
                ? contract.getPlannedEndDate()
                : contract.getEffectiveEndDate()).isAfter(contract.getStartDate());
    }

    private boolean isContractAnniversary(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return false;
        }
        YearMonth endMonth = YearMonth.from(endDate);
        int anniversaryDay = Math.min(startDate.getDayOfMonth(), endMonth.lengthOfMonth());
        return endDate.getDayOfMonth() == anniversaryDay;
    }

    private ContractBillingSnapshotDto toSnapshot(Contract contract) {
        LocalDate contractEnd = contract.getEffectiveEndDate() == null
            ? contract.getPlannedEndDate()
            : contract.getEffectiveEndDate();
        return new ContractBillingSnapshotDto(
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
