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
import java.time.YearMonth;
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

                        // Tenant kết thúc đúng ngày cuối kỳ:
                        // vẫn xử lý bằng hóa đơn thường.
                        || (contract.getStatus() == ContractStatus.ENDED
                        && contract.getEndReason() == ContractEndReason.TENANT_EARLY_EXIT
                        && contract.getFinalBillingPeriodEndDate() == null
                        && isContractAnniversary(
                        contract.getStartDate(),
                        contract.getEffectiveEndDate()
                ))

                        // Hợp đồng kết thúc sớm, chờ nhập điện nước.
                        || (contract.getStatus() == ContractStatus.ENDED
                        && (contract.getEndReason() == ContractEndReason.TENANT_EARLY_EXIT
                        || contract.getEndReason() == ContractEndReason.ADMIN_STOP_SERVICE));

        LocalDate contractEnd = contract.getEffectiveEndDate() == null
                ? contract.getPlannedEndDate()
                : contract.getEffectiveEndDate();

        boolean earlyTermination =
                contract.getStatus() == ContractStatus.ENDED
                        && (contract.getEndReason() == ContractEndReason.TENANT_EARLY_EXIT
                        || contract.getEndReason() == ContractEndReason.ADMIN_STOP_SERVICE);

        return regularBillingStatus
                && (
                earlyTermination
                        || (
                        contract.getStartDate().isBefore(today)
                                && contractEnd.isAfter(contract.getStartDate())
                )
        );
    }

    private boolean isContractAnniversary(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null
                || !endDate.isAfter(startDate)) {
            return false;
        }

        YearMonth endMonth = YearMonth.from(endDate);

        int anniversaryDay = Math.min(
                startDate.getDayOfMonth(),
                endMonth.lengthOfMonth()
        );

        return endDate.getDayOfMonth() == anniversaryDay;
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