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
import com.apartment.rental.contract.service.ContractService;
import com.apartment.rental.invoice.dto.InvoiceIssuedEvent;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import com.apartment.rental.invoice.repository.InvoiceRepository;
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
    private final ContractService contractService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public RegularInvoiceIssuanceService(
        BillingPeriodRepository periodRepository,
        InvoiceRepository invoiceRepository,
        UserRepository userRepository,
        ContractService contractService,
        ApplicationEventPublisher eventPublisher,
        EntityManager entityManager
    ) {
        this.periodRepository = periodRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.contractService = contractService;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }


    @Transactional(readOnly = true)
    public List<BillingPeriodRowDto> loadDuePeriods() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        List<BillingPeriodRowDto> rows = new ArrayList<>();
        for (ContractBillingResponse contract : contractService.findEligibleContractsForRegularBilling(today)) {
            DuePeriod duePeriod = findDuePeriod(contract, today);
            if (duePeriod != null) {
                rows.add(toBillingPeriodRow(contract, duePeriod));
            }
        }
        return rows;
    }

    /** Empty means the period was closed with no charge. */
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

    private User requireAdmin(String email) {
        User admin = userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản quản trị."));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chỉ Admin được phát hành hóa đơn.");
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
        return contractService.lockEligibleContractForBilling(contractId, today)
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
        BillingPeriod latest = periodRepository
            .findTopByContract_IdOrderByBillingPeriodEndDateDescIdDesc(contract.contractId())
            .orElse(null);

        if (latest != null && latest.getProcessingResult() == BillingPeriodResult.PENDING) {
            return latest.getBillingPeriodEndDate().isAfter(today)
                ? null
                : new DuePeriod(latest.getUsageFrom(), latest.getUsageTo(), latest.getBillingPeriodEndDate(), latest);
        }

        LocalDate usageFrom = latest == null ? contract.startDate() : latest.getBillingPeriodEndDate();
        if (!usageFrom.isBefore(contract.contractEndDate())) {
            return null;
        }

        LocalDate anniversary = nextAnniversary(contract.startDate(), usageFrom);
        LocalDate periodEnd = anniversary.isBefore(contract.contractEndDate()) ? anniversary : contract.contractEndDate();
        if (periodEnd.isAfter(today) || !periodEnd.isAfter(usageFrom)) {
            return null;
        }
        return new DuePeriod(usageFrom, periodEnd, periodEnd, null);
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
