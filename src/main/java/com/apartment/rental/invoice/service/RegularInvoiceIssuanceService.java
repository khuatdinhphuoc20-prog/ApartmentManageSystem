package com.apartment.rental.invoice.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.billing.dto.BillingPeriodRowDto;
import com.apartment.rental.billing.entity.BillingPeriod;
import com.apartment.rental.billing.entity.BillingPeriodResult;
import com.apartment.rental.billing.repository.BillingPeriodRepository;
import com.apartment.rental.contract.dto.response.ContractBillingResponse;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.service.ContractBillingQueryService;
import com.apartment.rental.invoice.dto.FinalSettlementPeriod;
import com.apartment.rental.invoice.dto.InvoiceIssuedEvent;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import com.apartment.rental.settlement.dto.SettlementAction;
import com.apartment.rental.settlement.dto.SettlementCalculationResult;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RegularInvoiceIssuanceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BillingPeriodRepository periodRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ContractBillingQueryService contractBillingQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public RegularInvoiceIssuanceService(
        BillingPeriodRepository periodRepository,
        InvoiceRepository invoiceRepository,
        UserRepository userRepository,
        ContractBillingQueryService contractBillingQueryService,
        ApplicationEventPublisher eventPublisher,
        EntityManager entityManager
    ) {
        this.periodRepository = periodRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.contractBillingQueryService = contractBillingQueryService;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }


    @Transactional(readOnly = true)
    public List<BillingPeriodRowDto> loadDuePeriods() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        List<BillingPeriodRowDto> rows = new ArrayList<>();
        for (ContractBillingResponse contract : contractBillingQueryService.findEligibleContractsForRegularBilling(today)) {
            DuePeriod duePeriod = findDuePeriod(contract, today);
            if (duePeriod != null) {
                rows.add(toBillingPeriodRow(contract, duePeriod));
            }
        }
        return rows;
    }


    @Transactional
    public Optional<Long> issuePeriod(
        Long contractId,
        BigDecimal electricityAmount,
        BigDecimal waterAmount,
        String currentEmail
    ) {
        User admin = requireAdmin(currentEmail);
        validateAmounts(electricityAmount, waterAmount);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        ContractBillingResponse contract = requireEligibleContract(contractId, today);
        DuePeriod duePeriod = requireDuePeriod(contract, today);
        BillingPeriod period = findPendingPeriod(contractId, duePeriod);
        ensureCanIssue(contractId, duePeriod, period);

        RentAmount rent = calculateRent(period.getUsageFrom(), period.getUsageTo(), contract);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        BigDecimal total = rent.amount().add(electricityAmount).add(waterAmount);
        completeInputs(period, admin, electricityAmount, waterAmount, rent, now);

        if (total.signum() == 0) {
            period.setProcessingResult(BillingPeriodResult.NO_CHARGE);
            periodRepository.save(period);
            return Optional.empty();
        }

        Long invoiceId = createInvoice(
            contractId, contract, duePeriod, period, rent,
            electricityAmount, waterAmount, total, now
        );
        return Optional.of(invoiceId);
    }


    public FinalSettlementPeriod findPeriodForSettlement(
        LocalDate contractStartDate,
        LocalDate plannedEndDate,
        LocalDate effectiveEndDate
    ) {
        if (contractStartDate == null || plannedEndDate == null || effectiveEndDate == null
            || !contractStartDate.isBefore(plannedEndDate)
            || effectiveEndDate.isBefore(contractStartDate)
            || !effectiveEndDate.isBefore(plannedEndDate)) {
            throw new IllegalArgumentException("Invalid contract dates for final settlement");
        }

        LocalDate periodStart = contractStartDate;
        LocalDate nextBoundary = nextAnniversary(contractStartDate, periodStart);
        while (nextBoundary.isBefore(effectiveEndDate)) {
            periodStart = nextBoundary;
            nextBoundary = nextAnniversary(contractStartDate, periodStart);
        }

        LocalDate periodEnd = nextBoundary.isBefore(plannedEndDate)
            ? nextBoundary : plannedEndDate;
        if (periodEnd.isBefore(effectiveEndDate)) {
            throw new IllegalArgumentException("The final billing period ends before the effective exit date");
        }
        return new FinalSettlementPeriod(periodStart, periodEnd);
    }


    @Transactional
    public Optional<Long> issueFinalSettlement(
        SettlementCalculationResult calculation,
        Long adminUserId
    ) {
        validateFinalSettlementCalculation(calculation);
        User admin = requireAdmin(adminUserId);

        LocalDate billingPeriodEndDate = calculation.usageTo();
        if (!billingPeriodEndDate.equals(calculation.billingPeriodEndDate())) {
            throw new IllegalArgumentException("The final billing date does not match the usage end date");
        }
        if (!billingPeriodEndDate.equals(LocalDate.now(BUSINESS_ZONE))) {
            throw new IllegalStateException("The settlement date has changed; reload and try again");
        }

        Optional<BillingPeriod> existingPeriod = periodRepository
            .findByContract_IdAndBillingPeriodEndDateForUpdate(
                calculation.contractId(), billingPeriodEndDate);
        if (invoiceRepository.existsByContract_IdAndBillingPeriodEndDate(
                calculation.contractId(), billingPeriodEndDate)) {
            throw new IllegalStateException("This contract period has already been processed; no duplicate settlement was created");
        }

        Contract contract = entityManager.find(Contract.class, calculation.contractId());
        if (contract == null) {
            throw new IllegalArgumentException("The contract for this settlement no longer exists");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE
            || contract.getEndReason() != null
            || contract.getAgreedMonthlyRent().compareTo(calculation.monthlyRentSnapshot()) != 0) {
            throw new IllegalStateException("The contract changed while its final settlement was being prepared");
        }

        BillingPeriod period = existingPeriod.orElseGet(() -> BillingPeriod.pending(
            contract,
            calculation.usageFrom(),
            calculation.usageTo(),
            billingPeriodEndDate
        ));
        if (period.getProcessingResult() != BillingPeriodResult.PENDING
            || !period.getUsageFrom().equals(calculation.usageFrom())
            || !period.getUsageTo().equals(calculation.usageTo())) {
            throw new IllegalStateException("This contract period has already been processed or its usage dates conflict with the settlement");
        }

        Instant processedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        period.setElectricityAmount(calculation.electricityAmount());
        period.setWaterAmount(calculation.waterAmount());
        period.setInputCompletedAt(processedAt);
        period.setEnteredByUser(admin);
        period.setRentAmount(calculation.rentAmount());
        period.setProcessedAt(processedAt);

        if (calculation.action() == SettlementAction.NO_CHARGE) {
            period.setProcessingResult(BillingPeriodResult.NO_CHARGE);
            periodRepository.save(period);
            return Optional.empty();
        }

        period.setProcessingResult(BillingPeriodResult.INVOICED);
        BillingPeriod savedPeriod = periodRepository.saveAndFlush(period);
        Invoice invoice = Invoice.earlySettlement(
            contract,
            savedPeriod,
            billingPeriodEndDate,
            calculation.usageFrom(),
            calculation.usageTo(),
            calculation.monthlyRentSnapshot(),
            calculation.rentCalculationRule(),
            calculation.proratedDays(),
            calculation.rentAmount(),
            calculation.electricityAmount(),
            calculation.waterAmount(),
            calculation.totalAmount(),
            processedAt
        );
        Invoice savedInvoice = invoiceRepository.save(invoice);
        eventPublisher.publishEvent(new InvoiceIssuedEvent(
            savedInvoice.getId(),
            contract.getTenant().getId(),
            contract.getId(),
            contract.getRoom().getRoomCode(),
            billingPeriodEndDate,
            calculation.totalAmount(),
            savedInvoice.getDueAt()
        ));
        return Optional.of(savedInvoice.getId());
    }

    private void validateFinalSettlementCalculation(SettlementCalculationResult calculation) {
        if (calculation == null || calculation.contractId() == null || calculation.contractId() <= 0) {
            throw new IllegalArgumentException("A valid settlement calculation is required");
        }
        if (calculation.action() != SettlementAction.EARLY_SETTLEMENT_REQUIRED
            && calculation.action() != SettlementAction.NO_CHARGE) {
            throw new IllegalArgumentException("A regular boundary period must use regular invoice issuance");
        }
        if (calculation.usageFrom() == null || calculation.usageTo() == null
            || calculation.usageFrom().isAfter(calculation.usageTo())
            || calculation.billingPeriodEndDate() == null
            || calculation.monthlyRentSnapshot() == null
            || calculation.rentAmount() == null
            || calculation.electricityAmount() == null
            || calculation.waterAmount() == null
            || calculation.totalAmount() == null) {
            throw new IllegalArgumentException("The settlement calculation is missing required period or amount data");
        }
        BigDecimal expectedTotal = calculation.rentAmount()
            .add(calculation.electricityAmount())
            .add(calculation.waterAmount());
        if (expectedTotal.compareTo(calculation.totalAmount()) != 0) {
            throw new IllegalArgumentException("The settlement total does not match its rent and utility amounts");
        }
        boolean zeroTotal = calculation.totalAmount().signum() == 0;
        if ((calculation.action() == SettlementAction.NO_CHARGE) != zeroTotal) {
            throw new IllegalArgumentException("The settlement action does not match its total amount");
        }
        if (calculation.totalAmount().signum() < 0) {
            throw new IllegalArgumentException("The settlement total cannot be negative");
        }
    }

    private User requireAdmin(String email) {
        User admin = userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản quản trị."));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chỉ Admin được phát hành hóa đơn.");
        }
        return admin;
    }

    private User requireAdmin(Long adminUserId) {
        User admin = userRepository.findById(adminUserId)
            .orElseThrow(() -> new AccessDeniedException("The current Admin account could not be found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Admin can issue a final settlement");
        }
        return admin;
    }

    private void validateAmounts(BigDecimal electricityAmount, BigDecimal waterAmount) {
        if (electricityAmount == null || waterAmount == null) {
            throw new IllegalArgumentException("Vui lòng nhập đủ tiền điện và tiền nước; số 0 được tính là đã nhập.");
        }
        validateAmount(electricityAmount, "tiền điện");
        validateAmount(waterAmount, "tiền nước");
    }

    private ContractBillingResponse requireEligibleContract(Long contractId, LocalDate today) {
        return contractBillingQueryService.lockEligibleContractForBilling(contractId, today)
            .orElseThrow(() -> new IllegalArgumentException("Hợp đồng này hiện không có kỳ thu định kỳ cần phát hành."));
    }

    private DuePeriod requireDuePeriod(ContractBillingResponse contract, LocalDate today) {
        DuePeriod duePeriod = findDuePeriod(contract, today);
        if (duePeriod == null) {
            throw new IllegalArgumentException("Kỳ tiếp theo chưa đến hạn hoặc hợp đồng đã được quyết toán.");
        }
        return duePeriod;
    }

    private BillingPeriod findPendingPeriod(Long contractId, DuePeriod duePeriod) {
        return periodRepository.findByContract_IdAndBillingPeriodEndDateForUpdate(
                contractId,
                duePeriod.periodEnd()
            )
            .orElseGet(() -> BillingPeriod.pending(
                entityManager.getReference(Contract.class, contractId),
                duePeriod.usageFrom(),
                duePeriod.usageTo(),
                duePeriod.periodEnd()
            ));
    }

    private void ensureCanIssue(Long contractId, DuePeriod duePeriod, BillingPeriod period) {
        if (period.getProcessingResult() != BillingPeriodResult.PENDING) {
            throw new IllegalStateException("Kỳ này đã được xử lý; không thể phát hành hóa đơn trùng.");
        }
        if (invoiceRepository.existsByContract_IdAndBillingPeriodEndDate(contractId, duePeriod.periodEnd())) {
            throw new IllegalStateException("Đã có hóa đơn cho kỳ này; hệ thống dừng để tránh thu trùng.");
        }
    }

    private void completeInputs(
        BillingPeriod period,
        User admin,
        BigDecimal electricityAmount,
        BigDecimal waterAmount,
        RentAmount rent,
        Instant now
    ) {
        period.setElectricityAmount(electricityAmount);
        period.setWaterAmount(waterAmount);
        period.setInputCompletedAt(now);
        period.setEnteredByUser(admin);
        period.setRentAmount(rent.amount());
        period.setProcessedAt(now);
    }

    private Long createInvoice(
        Long contractId,
        ContractBillingResponse contract,
        DuePeriod duePeriod,
        BillingPeriod period,
        RentAmount rent,
        BigDecimal electricityAmount,
        BigDecimal waterAmount,
        BigDecimal total,
        Instant now
    ) {
        period.setProcessingResult(BillingPeriodResult.INVOICED);
        BillingPeriod savedPeriod = periodRepository.save(period);
        Contract contractReference = entityManager.getReference(Contract.class, contractId);
        Invoice invoice = Invoice.regular(
            contractReference,
            savedPeriod,
            duePeriod.periodEnd(),
            savedPeriod.getUsageFrom(),
            savedPeriod.getUsageTo(),
            contract.agreedMonthlyRent(),
            rent.rule(),
            rent.proratedDays(),
            rent.amount(),
            electricityAmount,
            waterAmount,
            total,
            now
        );
        Invoice savedInvoice = invoiceRepository.save(invoice);
        eventPublisher.publishEvent(new InvoiceIssuedEvent(
            savedInvoice.getId(),
            contract.tenantId(),
            contractId,
            contract.roomCode(),
            duePeriod.periodEnd(),
            total,
            savedInvoice.getDueAt()
        ));
        return savedInvoice.getId();
    }

    private BillingPeriodRowDto toBillingPeriodRow(ContractBillingResponse contract, DuePeriod duePeriod) {
        BillingPeriod pending = duePeriod.pendingPeriod();
        return new BillingPeriodRowDto(
            contract.contractId(),
            pending == null ? null : pending.getId(),
            contract.roomCode(),
            contract.tenantName(),
            contract.tenantEmail(),
            duePeriod.usageFrom(),
            duePeriod.usageTo(),
            duePeriod.periodEnd(),
            contract.agreedMonthlyRent(),
            pending == null ? null : pending.getElectricityAmount(),
            pending == null ? null : pending.getWaterAmount(),
            pending == null ? BillingPeriodResult.PENDING.name() : pending.getProcessingResult().name()
        );
    }

    private DuePeriod findDuePeriod(ContractBillingResponse contract, LocalDate today) {
        LocalDate usageFrom = contract.startDate();
        while (usageFrom.isBefore(contract.contractEndDate())) {
            LocalDate anniversary = nextAnniversary(contract.startDate(), usageFrom);
            LocalDate periodEnd = anniversary.isBefore(contract.contractEndDate())
                ? anniversary
                : contract.contractEndDate();
            if (!periodEnd.isAfter(usageFrom)) {
                throw new IllegalStateException("The contract has a non-progressing billing period");
            }
            if (periodEnd.isAfter(today)) {
                return null;
            }

            BillingPeriod processedPeriod = periodRepository
                .findByContract_IdAndBillingPeriodEndDate(contract.contractId(), periodEnd)
                .orElse(null);
            if (processedPeriod == null) {
                return new DuePeriod(usageFrom, periodEnd, periodEnd, null);
            }
            if (processedPeriod.getProcessingResult() == BillingPeriodResult.PENDING) {
                if (!processedPeriod.getUsageFrom().equals(usageFrom)
                    || !processedPeriod.getUsageTo().equals(periodEnd)) {
                    throw new IllegalStateException("A pending billing period does not match the contract's personal billing cycle");
                }
                return new DuePeriod(
                    processedPeriod.getUsageFrom(),
                    processedPeriod.getUsageTo(),
                    processedPeriod.getBillingPeriodEndDate(),
                    processedPeriod
                );
            }
            usageFrom = periodEnd;
        }
        return null;
    }

    
    private LocalDate nextAnniversary(LocalDate startDate, LocalDate previousPeriodEnd) {
        YearMonth month = YearMonth.from(previousPeriodEnd);
        LocalDate candidate = anniversaryInMonth(startDate, month);

        while (!candidate.isAfter(previousPeriodEnd)) {
            month = month.plusMonths(1);
            candidate = anniversaryInMonth(startDate, month);
        }
        return candidate;
    }

    private LocalDate anniversaryInMonth(LocalDate startDate, YearMonth month) {
        int day = Math.min(startDate.getDayOfMonth(), month.lengthOfMonth());
        return month.atDay(day);
    }

    private RentAmount calculateRent(LocalDate usageFrom, LocalDate usageTo, ContractBillingResponse contract) {
        if (!usageTo.isAfter(usageFrom)) {
            throw new IllegalArgumentException("Kỳ sử dụng phải có ít nhất một ngày.");
        }

        long usedDays = ChronoUnit.DAYS.between(usageFrom, usageTo);
        if (usedDays < 14) {
            BigDecimal proratedRent = contract.agreedMonthlyRent()
                .multiply(BigDecimal.valueOf(usedDays))
                .divide(BigDecimal.valueOf(30), 0, RoundingMode.HALF_UP);
            RentCalculationRule rule = usageFrom.equals(contract.startDate())
                ? RentCalculationRule.FIRST_PRORATED
                : RentCalculationRule.PERIOD_PRORATED;
            return new RentAmount(proratedRent, rule, Math.toIntExact(usedDays));
        }

        return new RentAmount(contract.agreedMonthlyRent(), RentCalculationRule.MONTHLY_FULL, null);
    }

    private void validateAmount(BigDecimal amount, String label) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Số tiền " + label + " không được âm.");
        }
    }

    private record DuePeriod(LocalDate usageFrom, LocalDate usageTo, LocalDate periodEnd, BillingPeriod pendingPeriod) {
    }

    private record RentAmount(BigDecimal amount, RentCalculationRule rule, Integer proratedDays) {
    }
}
