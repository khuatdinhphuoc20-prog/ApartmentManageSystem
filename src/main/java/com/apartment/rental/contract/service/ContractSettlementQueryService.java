package com.apartment.rental.contract.service;

import com.apartment.rental.contract.dto.ContractSettlementSnapshot;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.exception.BusinessException;
import com.apartment.rental.contract.exception.ResourceNotFoundException;
import com.apartment.rental.contract.repository.ContractRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractSettlementQueryService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ContractRepository contractRepository;

    public ContractSettlementQueryService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public ContractSettlementSnapshot prepareSettlementInput(
        Long contractId,
        ContractEndReason reason,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        return prepareSettlementInput(
            contractId,
            reason,
            periodStart,
            periodEnd,
            LocalDate.now(BUSINESS_ZONE)
        );
    }

    @Transactional(readOnly = true)
    public ContractSettlementSnapshot prepareSettlementInput(
        Long contractId,
        ContractEndReason reason,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate effectiveEndDate
    ) {
        validateRequest(contractId, reason, periodStart, periodEnd);

        if (effectiveEndDate == null
            || effectiveEndDate.isBefore(periodStart)
            || effectiveEndDate.isAfter(periodEnd)) {
            throw new BusinessException("The settlement date must fall within the supplied period or on its boundary");
        }

        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        validateContract(contract, effectiveEndDate);

        return new ContractSettlementSnapshot(
            contract.getId(),
            contract.getAgreedMonthlyRent(),
            contract.getStartDate(),
            contract.getPlannedEndDate(),
            effectiveEndDate,
            reason,
            periodStart,
            periodEnd
        );
    }

    private void validateRequest(
        Long contractId,
        ContractEndReason reason,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        if (contractId == null || contractId <= 0) {
            throw new BusinessException("Contract id must be positive");
        }
        if (reason != ContractEndReason.TENANT_EARLY_EXIT
            && reason != ContractEndReason.ADMIN_STOP_SERVICE) {
            throw new BusinessException("Invalid contract termination reason");
        }
        if (periodStart == null || periodEnd == null || !periodStart.isBefore(periodEnd)) {
            throw new BusinessException("A billing period must have a start date before its end date");
        }
    }

    private void validateContract(Contract contract, LocalDate effectiveEndDate) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Only active contracts can be prepared for early settlement");
        }
        if (contract.getStartDate() == null || contract.getPlannedEndDate() == null
            || !contract.getStartDate().isBefore(contract.getPlannedEndDate())) {
            throw new BusinessException("Contract dates are invalid");
        }
        if (effectiveEndDate.isBefore(contract.getStartDate())) {
            throw new BusinessException("The settlement date cannot be before the contract start date");
        }
        if (!effectiveEndDate.isBefore(contract.getPlannedEndDate())) {
            throw new BusinessException("Early settlement must be before the planned contract end date");
        }
        if (contract.getAgreedMonthlyRent() == null || contract.getAgreedMonthlyRent().signum() <= 0) {
            throw new BusinessException("The agreed monthly rent must be positive");
        }
    }
}
