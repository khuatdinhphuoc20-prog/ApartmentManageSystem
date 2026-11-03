package com.apartment.rental.billing.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.billing.dto.InvoiceIssuanceResult;
import com.apartment.rental.billing.entity.BillingCycle;
import com.apartment.rental.billing.entity.BillingCycleItem;
import com.apartment.rental.billing.entity.BillingCycleItemResult;
import com.apartment.rental.billing.entity.BillingCycleStatus;
import com.apartment.rental.billing.repository.BillingCycleItemRepository;
import com.apartment.rental.billing.repository.BillingCycleRepository;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.invoice.dto.InvoiceIssuedEvent;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegularInvoiceIssuanceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BillingCycleRepository cycleRepository;
    private final BillingCycleItemRepository itemRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RegularInvoiceIssuanceService(
        BillingCycleRepository cycleRepository,
        BillingCycleItemRepository itemRepository,
        InvoiceRepository invoiceRepository,
        UserRepository userRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.cycleRepository = cycleRepository;
        this.itemRepository = itemRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InvoiceIssuanceResult issueCycle(Long cycleId, String currentEmail) {
        BillingCycle cycle = cycleRepository.findByIdForUpdate(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỳ thu."));
        User admin = userRepository.findByEmail(currentEmail)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đang đăng nhập."));
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ Admin được phát hành hóa đơn.");
        }
        if (cycle.getStatus() == BillingCycleStatus.COMPLETED) {
            return new InvoiceIssuanceResult(0, 0, true, true);
        }
        if (cycle.getCollectionDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new IllegalArgumentException("Chưa đến ngày thu của kỳ này nên chưa thể phát hành Invoice.");
        }

        List<BillingCycleItem> items = itemRepository.findAllByBillingCycle_IdOrderByIdAsc(cycleId);
        for (BillingCycleItem item : items) {
            if (item.getProcessingResult() != BillingCycleItemResult.PENDING) {
                continue;
            }
            if (item.getElectricityAmount() == null || item.getWaterAmount() == null
                || item.getInputCompletedAt() == null || item.getEnteredByUser() == null) {
                throw new IllegalArgumentException(
                    "Chưa nhập đủ tiền điện và tiền nước cho tất cả các nghĩa vụ của kỳ."
                );
            }
            if (invoiceRepository.existsByBillingCycleItem_Id(item.getId())) {
                throw new IllegalArgumentException("Đã có Invoice cho một nghĩa vụ đang ở trạng thái PENDING.");
            }
            if (invoiceRepository.existsByContract_IdAndBillingPeriodEndDate(
                item.getContract().getId(), cycle.getCollectionDate())) {
                throw new IllegalArgumentException("Contract đã có Invoice cho mốc kỳ này; hệ thống dừng để tránh thu trùng.");
            }
        }

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        int invoicesIssued = 0;
        int noChargeItems = 0;

        for (BillingCycleItem item : items) {
            if (item.getProcessingResult() != BillingCycleItemResult.PENDING) {
                continue;
            }

            Contract contract = item.getContract();
            RentAmount rent = calculateRent(item, contract);
            BigDecimal electricity = item.getElectricityAmount();
            BigDecimal water = item.getWaterAmount();
            BigDecimal total = rent.amount().add(electricity).add(water);

            item.setRentAmount(rent.amount());
            item.setProcessedAt(now);
            if (total.signum() == 0) {
                item.setProcessingResult(BillingCycleItemResult.NO_CHARGE);
                noChargeItems++;
            } else {
                Invoice invoice = Invoice.regular(
                    contract,
                    item,
                    cycle.getCollectionDate(),
                    item.getUsageFrom(),
                    item.getUsageTo(),
                    contract.getAgreedMonthlyRent(),
                    rent.rule(),
                    rent.proratedDays(),
                    rent.amount(),
                    electricity,
                    water,
                    total,
                    now
                );
                Invoice savedInvoice = invoiceRepository.save(invoice);
                eventPublisher.publishEvent(new InvoiceIssuedEvent(
                    savedInvoice.getId(),
                    contract.getTenant().getId(),
                    contract.getId(),
                    contract.getRoom().getRoomCode(),
                    cycle.getCollectionDate(),
                    total,
                    savedInvoice.getDueAt()
                ));
                item.setProcessingResult(BillingCycleItemResult.INVOICED);
                invoicesIssued++;
            }
        }

        itemRepository.saveAll(items);
        boolean completed = items.stream()
            .allMatch(item -> item.getProcessingResult() != BillingCycleItemResult.PENDING);
        if (completed) {
            cycle.setStatus(BillingCycleStatus.COMPLETED);
            cycle.setCompletedAt(now);
            cycleRepository.save(cycle);
        }
        return new InvoiceIssuanceResult(invoicesIssued, noChargeItems, completed, false);
    }

    private RentAmount calculateRent(BillingCycleItem item, Contract contract) {
        long usedDays = ChronoUnit.DAYS.between(item.getUsageFrom(), item.getUsageTo());
        boolean firstUsePeriod = item.getUsageFrom().equals(contract.getStartDate());

        if (firstUsePeriod && usedDays < 14) {
            if (usedDays < 1) {
                throw new IllegalArgumentException("Kỳ đầu có 0 ngày sử dụng; cần đối chiếu Contract trước khi phát hành.");
            }
            int proratedDays = Math.toIntExact(usedDays);
            BigDecimal amount = contract.getAgreedMonthlyRent()
                .multiply(BigDecimal.valueOf(proratedDays))
                .divide(BigDecimal.valueOf(30), 0, RoundingMode.HALF_UP);
            return new RentAmount(amount, RentCalculationRule.FIRST_PRORATED, proratedDays);
        }

        return new RentAmount(contract.getAgreedMonthlyRent(), RentCalculationRule.MONTHLY_FULL, null);
    }

    private record RentAmount(BigDecimal amount, RentCalculationRule rule, Integer proratedDays) {
    }
}
