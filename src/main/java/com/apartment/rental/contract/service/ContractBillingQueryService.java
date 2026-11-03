package com.apartment.rental.contract.service;

import com.apartment.rental.contract.dto.ContractBillingSnapshotDto;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.repository.ContractRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Supplies M05 with contract data for the selected billing period. */
@Service
public class ContractBillingQueryService {

    private final ContractRepository contractRepository;

    public ContractBillingQueryService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<ContractBillingSnapshotDto> findBillingObligations(LocalDate periodStart, LocalDate periodEnd) {
        return contractRepository.findBillingCandidates(periodStart, periodEnd, ContractStatus.CANCELLED)
            .stream()
            .map(contract -> toSnapshot(contract, periodStart, periodEnd))
            .toList();
    }

    private ContractBillingSnapshotDto toSnapshot(Contract contract, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate contractEnd = contract.getEffectiveEndDate() == null
            ? contract.getPlannedEndDate()
            : contract.getEffectiveEndDate();
        LocalDate usageFrom = contract.getStartDate().isAfter(periodStart)
            ? contract.getStartDate()
            : periodStart;
        LocalDate usageTo = contractEnd.isBefore(periodEnd) ? contractEnd : periodEnd;

        return new ContractBillingSnapshotDto(
            contract.getId(),
            contract.getRoom().getId(),
            contract.getTenant().getFullName(),
            contract.getTenant().getEmail(),
            contract.getAgreedMonthlyRent(),
            usageFrom,
            usageTo
        );
    }
}
