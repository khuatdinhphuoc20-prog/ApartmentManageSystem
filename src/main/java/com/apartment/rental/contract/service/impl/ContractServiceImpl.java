package com.apartment.rental.contract.service.impl;

import com.apartment.rental.account.dto.response.UserInfoForM04Response;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.service.UserServiceForM04;
import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.contract.dto.request.ContractCoOccupantRequest;
import com.apartment.rental.contract.dto.request.CreateContractFromApplicationRequest;
import com.apartment.rental.contract.dto.request.DirectContractRequest;
import com.apartment.rental.contract.dto.response.ContractBillingResponse;
import com.apartment.rental.contract.dto.response.ContractDetailResponse;
import com.apartment.rental.contract.dto.response.ContractResponse;
import com.apartment.rental.contract.dto.response.ContractTerminationResult;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractCoOccupant;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.FinalSettlementResult;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.mapper.ContractMapper;
import com.apartment.rental.contract.repository.ContractCoOccupantRepository;
import com.apartment.rental.contract.repository.ContractRepository;
import com.apartment.rental.contract.service.ContractService;
import com.apartment.rental.contract.service.ContractSettlementQueryService;
import com.apartment.rental.contract.exception.*;

import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import com.apartment.rental.room.service.RoomService;
import com.apartment.rental.invoice.dto.FinalSettlementPeriod;
import com.apartment.rental.invoice.service.RegularInvoiceIssuanceService;
import com.apartment.rental.settlement.dto.SettlementAction;
import com.apartment.rental.settlement.dto.SettlementCalculationResult;
import com.apartment.rental.settlement.service.SettlementCalculator;
import jakarta.persistence.EntityManager;

import com.apartment.rental.room.dto.response.RoomRentalInfoForM04Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractCoOccupantRepository coOccupantRepository;
    private final ContractMapper contractMapper;
    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final RoomService roomService;
    private final EntityManager entityManager;
    private final UserServiceForM04 userServiceForM04;
    private final ContractSettlementQueryService contractSettlementQueryService;
    private final SettlementCalculator settlementCalculator;
    private final RegularInvoiceIssuanceService regularInvoiceIssuanceService;

    @Override
    public List<ContractBillingResponse> findEligibleContractsForRegularBilling(LocalDate today) {
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

    private boolean canReceiveRegularInvoice(Contract contract, LocalDate today) {
        boolean regularBillingStatus = contract.getStatus() == ContractStatus.ACTIVE
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

    @Override
    public List<ContractResponse> getTenantContracts(Long tenantId) {

        List<Contract> contracts =
                contractRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId);

        return contractMapper.toResponseList(contracts);
    }

    @Override
    public ContractDetailResponse getTenantContractDetail(
            Long contractId,
            Long tenantId
    ) {
        Contract contract = contractRepository
                .findByIdAndTenant_Id(contractId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Contract not found"));

        return getDetailResponse(contract);
    }

    @Override
    public ContractDetailResponse getContractDetail(Long contractId) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Contract not found"));

        return getDetailResponse(contract);
    }

    private ContractDetailResponse getDetailResponse(Contract contract) {

        List<ContractCoOccupant> coOccupants =
                coOccupantRepository.findAllByContract_Id(contract.getId());

        return contractMapper.toDetailResponse(contract, coOccupants);
    }
    @Override
    public List<ContractResponse> getAllContracts() {
        List<Contract> contracts =
                contractRepository.findAllByOrderByCreatedAtDesc();

        return contractMapper.toResponseList(contracts);
    }

    @Override
    @Transactional
    public void cancelScheduledContract(Long contractId, Long adminId) {

        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Contract not found"));

        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        if (contract.getStatus() != ContractStatus.SCHEDULED) {
            throw new BusinessException(
                    "Only scheduled contracts can be cancelled"
            );
        }

        if (!today.isBefore(contract.getStartDate())) {
            throw new BusinessException(
                    "Cannot cancel contract on or after its start date"
            );
        }

        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancelledAt(Instant.now());
        contract.setClosedByUser(
                entityManager.getReference(User.class, adminId)
        );

        roomService.releaseRoom(contract.getRoom().getId(), RoomStatus.AVAILABLE);
    }


    @Override
    @Transactional
    public void activateScheduledContracts(LocalDate currentDate) {

        List<Contract> contracts =
                contractRepository.findAllByStatusAndStartDateLessThanEqual(
                        ContractStatus.SCHEDULED,
                        currentDate
                );

        for (Contract contract : contracts) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setActivatedAt(Instant.now());
        }

        contractRepository.saveAll(contracts);
    }

    @Override
    @Transactional
    public void expireActiveContracts(LocalDate currentDate) {

        List<Contract> contracts =
                contractRepository.findAllByStatusAndPlannedEndDateLessThanEqual(
                        ContractStatus.ACTIVE,
                        currentDate
                );

        for (Contract contract : contracts) {
            contract.setStatus(ContractStatus.ENDED);
            contract.setEffectiveEndDate(contract.getPlannedEndDate());
            contract.setEndReason(ContractEndReason.NATURAL_EXPIRY);
            contract.setEndedAt(Instant.now());

            // Natural expiry must not create early settlement fields.
            // M05 must still be able to bill the final regular period.
        }

        contractRepository.saveAll(contracts);

        for (Contract contract : contracts) {
            // Room becomes available, but final billing remains M05's duty.
            roomService.releaseRoom(contract.getRoom().getId(), RoomStatus.AVAILABLE);
        }
    }

    @Override
    @Transactional
    public ContractTerminationResult terminateActiveContract(
            Long contractId,
            Long adminId,
            ContractEndReason reason,
            BigDecimal electricityAmount,
            BigDecimal waterAmount,
            String closingNote
    ) {
        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Contract not found"));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException(
                    "Only active contracts can be terminated"
            );
        }
        LocalDate effectiveEndDate = LocalDate.now(BUSINESS_ZONE);
        if (!effectiveEndDate.isBefore(contract.getPlannedEndDate())) {
            throw new BusinessException("Early termination must happen before the planned end date");
        }

        FinalSettlementPeriod billingPeriod = regularInvoiceIssuanceService.findPeriodForSettlement(
                contract.getStartDate(),
                contract.getPlannedEndDate(),
                effectiveEndDate
        );
        var snapshot = contractSettlementQueryService.prepareSettlementInput(
                contract.getId(),
                reason,
                billingPeriod.periodStart(),
                billingPeriod.periodEnd(),
                effectiveEndDate
        );
        SettlementCalculationResult calculation = settlementCalculator.calculate(
                snapshot,
                electricityAmount,
                waterAmount
        );

        FinalSettlementResult persistedSettlement = null;
        Long invoiceId = null;
        boolean regularBillingRequired = calculation.action() == SettlementAction.REGULAR_REQUIRED;
        if (!regularBillingRequired) {
            Optional<Long> issuedInvoiceId = regularInvoiceIssuanceService.issueFinalSettlement(
                    calculation,
                    adminId
            );

            invoiceId = issuedInvoiceId.orElse(null);
            persistedSettlement = invoiceId == null
                    ? FinalSettlementResult.NO_CHARGE
                    : FinalSettlementResult.INVOICED;
            contract.setFinalBillingPeriodEndDate(effectiveEndDate);
            contract.setFinalSettlementResult(persistedSettlement);
            contract.setSettledAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        }

        contract.setStatus(ContractStatus.ENDED);
        contract.setEndReason(reason);
        contract.setEffectiveEndDate(effectiveEndDate);
        contract.setEndedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        contract.setClosedByUser(entityManager.getReference(User.class, adminId));
        contract.setClosingNote(closingNote);

        RoomStatus roomTargetStatus = reason == ContractEndReason.ADMIN_STOP_SERVICE
                ? RoomStatus.INACTIVE
                : RoomStatus.AVAILABLE;
        roomService.releaseRoom(contract.getRoom().getId(), roomTargetStatus);

        return new ContractTerminationResult(
                effectiveEndDate,
                persistedSettlement,
                invoiceId,
                regularBillingRequired
        );
    }
    @Override
    @Transactional
    public ContractDetailResponse createDirectContract(
            DirectContractRequest request,
            Long adminId
    ) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        // 1. Validate start date
        if (request.getStartDate().isBefore(today)
                || request.getStartDate().isAfter(today.plusDays(10))) {
            throw new BusinessException(
                    "Start date must be between today and the next 10 days"
            );
        }

        // 2. Calculate planned end date
        LocalDate plannedEndDate = LocalDate.of(
                request.getEndYear(),
                request.getEndMonth(),
                10
        );

        if (!plannedEndDate.isAfter(request.getStartDate())) {
            throw new BusinessException(
                    "Planned end date must be after start date"
            );
        }

        // 3. Get room information
        RoomRentalInfoForM04Response roomInfo =
                roomService.getRoomRentalInfoForM04(request.getRoomId());

        if (roomInfo.status() != RoomStatus.AVAILABLE) {
            throw new BusinessException("Room is not available");
        }

        BigDecimal monthlyRent = roomInfo.listedMonthlyRent();

        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Room rental price is invalid");
        }

// 4. Get and validate co-occupants
        List<ContractCoOccupantRequest> coOccupants = new ArrayList<>();

        if (request.getCoOccupants() != null) {
            for (ContractCoOccupantRequest occupant : request.getCoOccupants()) {

                if (occupant == null) {
                    continue;
                }

                String fullName = occupant.getFullName() == null
                        ? ""
                        : occupant.getFullName().trim();

                String identityNo = occupant.getIdentityDocumentNo() == null
                        ? ""
                        : occupant.getIdentityDocumentNo().trim();

                String phone = occupant.getPhone() == null
                        ? ""
                        : occupant.getPhone().trim();

                // Skip completely empty rows
                if (fullName.isEmpty()
                        && identityNo.isEmpty()
                        && phone.isEmpty()) {
                    continue;
                }

                // Reject partially completed rows
                if (fullName.isEmpty() || identityNo.isEmpty()) {
                    throw new BusinessException(
                            "Co-occupant name and identity document number are required"
                    );
                }

                occupant.setFullName(fullName);
                occupant.setIdentityDocumentNo(identityNo);
                occupant.setPhone(phone.isEmpty() ? null : phone);

                coOccupants.add(occupant);
            }
        }

// Get room capacity
        RoomCapacityForM04Response roomCapacity =
                roomService.getRoomCapacityForM04(request.getRoomId());

        if (roomCapacity.capacity() == null
                || roomCapacity.capacity() <= 0) {
            throw new BusinessException("Room capacity is invalid");
        }

// +1 for the representative tenant
        if (coOccupants.size() + 1 > roomCapacity.capacity()) {
            throw new BusinessException(
                    "Number of tenants exceeds room capacity"
            );
        }

// Check duplicate identity numbers among co-occupants
        Set<String> identityNumbers = new HashSet<>();

        for (ContractCoOccupantRequest occupant : coOccupants) {
            String identityNo = occupant.getIdentityDocumentNo();

            if (!identityNumbers.add(identityNo)) {
                throw new BusinessException(
                        "Duplicate identity document number among co-occupants"
                );
            }
        }

// Check co-occupant identity against representative tenant
        UserInfoForM04Response tenant = userServiceForM04.getAvailableTenants()
                .stream()
                .filter(user -> user.id().equals(request.getTenantId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Tenant not found"));

        if (tenant.identityDocumentNo() != null
                && identityNumbers.contains(tenant.identityDocumentNo().trim())) {
            throw new BusinessException(
                    "Co-occupant identity document number duplicates tenant"
            );
        }
        // 5. Reserve room through M02
        roomService.reserveRoom(request.getRoomId());

        // 6. Create contract
        Contract contract = new Contract();

        contract.setApplication(null);
        contract.setRoom(
                entityManager.getReference(Room.class, request.getRoomId())
        );
        contract.setTenant(
                entityManager.getReference(User.class, request.getTenantId())
        );
        contract.setCreatedByUser(
                entityManager.getReference(User.class, adminId)
        );

        contract.setAgreedMonthlyRent(monthlyRent);
        contract.setStartDate(request.getStartDate());
        contract.setPlannedEndDate(plannedEndDate);

        if (request.getStartDate().equals(today)) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setActivatedAt(Instant.now());
        } else {
            contract.setStatus(ContractStatus.SCHEDULED);
        }

        Contract savedContract = contractRepository.save(contract);

        // 7. Save co-occupants
        for (ContractCoOccupantRequest occupantRequest : coOccupants) {
            ContractCoOccupant occupant = new ContractCoOccupant();

            occupant.setContract(savedContract);
            occupant.setFullName(occupantRequest.getFullName().trim());
            occupant.setIdentityDocumentNo(
                    occupantRequest.getIdentityDocumentNo().trim()
            );

            occupant.setPhone(
                    occupantRequest.getPhone() == null
                            || occupantRequest.getPhone().isBlank()
                            ? null
                            : occupantRequest.getPhone().trim()
            );

            coOccupantRepository.save(occupant);
        }

        return getDetailResponse(savedContract);
    }
    @Override
    public List<UserInfoForM04Response> getAvailableTenants() {
        return userServiceForM04.getAvailableTenants()
                .stream()
                .map(user -> new UserInfoForM04Response(
                        user.id(),
                        user.fullName(),
                        user.identityDocumentNo(),
                        user.phone()
                ))
                .toList();
    }

    @Override
    public List<RoomOptionForM04Response> getAvailableRooms() {
        return roomService.getAvailableRooms();
    }
    @Override
    public List<ContractResponse> getAllContracts(
            String keyword,
            ContractStatus status
    ) {
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        if (keyword != null) {
            keyword = keyword.trim();
        }

        List<Contract> contracts =
                contractRepository.searchContractsForAdmin(keyword, status);

        return contractMapper.toResponseList(contracts);
    }
    @Override
    @Transactional
    public ContractDetailResponse createContractFromApplication(
            CreateContractFromApplicationRequest request,
            Long adminId
    ) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        // 1. Validate start date
        if (request.startDate().isBefore(today)
                || request.startDate().isAfter(today.plusDays(10))) {
            throw new BusinessException(
                    "Start date must be between today and the next 10 days"
            );
        }

        // 2. Validate planned end date
        if (!request.plannedEndDate().isAfter(request.startDate())) {
            throw new BusinessException(
                    "Planned end date must be after start date"
            );
        }

        // 3. Prevent duplicate contract for the same application
        if (contractRepository.findByApplication_Id(request.applicationId()).isPresent()) {
            throw new BusinessException(
                    "A contract already exists for this application"
            );
        }

        // 4. Get room information from M02
        RoomRentalInfoForM04Response roomInfo =
                roomService.getRoomRentalInfoForM04(request.roomId());

        if (roomInfo.status() != RoomStatus.AVAILABLE) {
            throw new BusinessException("Room is not available");
        }

        BigDecimal monthlyRent = roomInfo.listedMonthlyRent();

        if (monthlyRent == null
                || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Room rental price is invalid");
        }

        // 5. Reserve room through M02
        roomService.reserveRoom(request.roomId());

        // 6. Create contract
        Contract contract = new Contract();

        contract.setApplication(
                entityManager.getReference(
                        RentalApplication.class,
                        request.applicationId()
                )
        );

        contract.setRoom(
                entityManager.getReference(Room.class, request.roomId())
        );

        contract.setTenant(
                entityManager.getReference(User.class, request.tenantId())
        );

        contract.setCreatedByUser(
                entityManager.getReference(User.class, adminId)
        );

        contract.setAgreedMonthlyRent(monthlyRent);
        contract.setStartDate(request.startDate());
        contract.setPlannedEndDate(request.plannedEndDate());

        // 7. Determine contract status
        if (request.startDate().equals(today)) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setActivatedAt(Instant.now());
        } else {
            contract.setStatus(ContractStatus.SCHEDULED);
        }

        // 8. Save contract
        Contract savedContract = contractRepository.save(contract);

        return getDetailResponse(savedContract);
    }
}
