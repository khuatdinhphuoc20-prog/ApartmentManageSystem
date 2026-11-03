package com.apartment.rental.billing.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.billing.entity.BillingCycle;
import com.apartment.rental.billing.entity.BillingCycleItem;
import com.apartment.rental.billing.entity.BillingCycleItemResult;
import com.apartment.rental.billing.entity.BillingCycleStatus;
import com.apartment.rental.billing.dto.BillingCycleItemForm;
import com.apartment.rental.billing.dto.BillingCycleItemsForm;
import com.apartment.rental.billing.dto.BillingCycleOption;
import com.apartment.rental.billing.dto.BillingCyclePage;
import com.apartment.rental.contract.dto.ContractBillingSnapshotDto;
import com.apartment.rental.contract.service.ContractBillingQueryService;
import com.apartment.rental.room.dto.RoomSummaryDto;
import com.apartment.rental.room.service.RoomSummaryService;
import com.apartment.rental.billing.repository.BillingCycleItemRepository;
import com.apartment.rental.billing.repository.BillingCycleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualBillingCycleService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BillingCycleRepository cycleRepository;
    private final BillingCycleItemRepository itemRepository;
    private final ContractBillingQueryService contractQuery;
    private final RoomSummaryService roomQuery;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public ManualBillingCycleService(
        BillingCycleRepository cycleRepository,
        BillingCycleItemRepository itemRepository,
        ContractBillingQueryService contractQuery,
        RoomSummaryService roomQuery,
        UserRepository userRepository,
        EntityManager entityManager
    ) {
        this.cycleRepository = cycleRepository;
        this.itemRepository = itemRepository;
        this.contractQuery = contractQuery;
        this.roomQuery = roomQuery;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public BillingCyclePage loadPage(Long requestedCycleId) {
        List<BillingCycleOption> cycles = cycleRepository.findAllByOrderByCollectionDateDesc().stream()
            .map(this::toOption)
            .toList();

        BillingCycleOption selected = null;
        if (requestedCycleId != null) {
            selected = cycles.stream()
                .filter(cycle -> cycle.id().equals(requestedCycleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỳ thu đã chọn."));
        } else {
            selected = cycles.stream()
                .filter(cycle -> cycle.status() == BillingCycleStatus.PENDING_INPUT)
                .findFirst()
                .orElse(null);
            if (selected == null && !cycles.isEmpty()) {
                selected = cycles.getFirst();
            }
        }

        BillingCycleItemsForm form = selected == null
            ? new BillingCycleItemsForm()
            : toForm(selected.id());
        boolean canIssue = selected != null
            && selected.status() == BillingCycleStatus.PENDING_INPUT
            && !form.getItems().isEmpty()
            && form.getItems().stream().allMatch(BillingCycleItemForm::isInputComplete);
        return new BillingCyclePage(cycles, selected, form, canIssue);
    }

    @Transactional
    public Long createOrOpenCycle(LocalDate collectionDate) {
        if (collectionDate == null || collectionDate.getDayOfMonth() != 10) {
            throw new IllegalArgumentException("Ngày thu phải là ngày 10 của tháng.");
        }
        if (collectionDate.isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new IllegalArgumentException("Chỉ được tạo kỳ đã đến ngày thu, không tạo kỳ tương lai.");
        }

        BillingCycle cycle = cycleRepository.findByCollectionDate(collectionDate).orElseGet(() -> {
            return cycleRepository.save(BillingCycle.pendingInput(collectionDate));
        });

        // Completed cycles are historical records; opening them must never change their contents.
        if (cycle.getStatus() == BillingCycleStatus.COMPLETED) {
            return cycle.getId();
        }

        LocalDate periodStart = collectionDate.minusMonths(1);
        List<ContractBillingSnapshotDto> obligations = contractQuery.findBillingObligations(periodStart, collectionDate);
        List<BillingCycleItem> existingItems = itemRepository.findAllByBillingCycle_IdOrderByIdAsc(cycle.getId());
        Set<Long> existingContractIds = new HashSet<>();
        existingItems.forEach(item -> existingContractIds.add(item.getContract().getId()));

        for (ContractBillingSnapshotDto obligation : obligations) {
            if (existingContractIds.contains(obligation.contractId())) {
                continue;
            }
            itemRepository.save(BillingCycleItem.pending(
                cycle,
                entityManager.getReference(com.apartment.rental.contract.entity.Contract.class, obligation.contractId()),
                obligation.usageFrom(),
                obligation.usageTo()
            ));
        }

        List<BillingCycleItem> allItems = itemRepository.findAllByBillingCycle_IdOrderByIdAsc(cycle.getId());
        if (allItems.isEmpty()) {
            cycle.setStatus(BillingCycleStatus.COMPLETED);
            cycle.setCompletedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
            cycleRepository.save(cycle);
        }
        return cycle.getId();
    }

    @Transactional
    public void saveUtilityAmounts(Long cycleId, BillingCycleItemsForm form, String currentEmail) {
        BillingCycle cycle = cycleRepository.findById(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỳ thu."));
        if (cycle.getStatus() != BillingCycleStatus.PENDING_INPUT) {
            throw new IllegalArgumentException("Kỳ thu đã hoàn tất, không thể sửa dữ liệu.");
        }

        User admin = userRepository.findByEmail(currentEmail)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đang đăng nhập."));
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ Admin được nhập tiền điện nước.");
        }

        List<BillingCycleItem> databaseItems = itemRepository.findAllByBillingCycle_IdOrderByIdAsc(cycleId);
        Map<Long, BillingCycleItem> itemsById = new HashMap<>();
        databaseItems.forEach(item -> itemsById.put(item.getId(), item));

        List<BillingCycleItemForm> submittedRows = form == null ? null : form.getItems();
        if (submittedRows == null || submittedRows.size() != databaseItems.size()) {
            throw new IllegalArgumentException("Danh sách gửi lên không khớp với các nghĩa vụ của kỳ thu.");
        }

        Set<Long> submittedIds = new HashSet<>();
        for (BillingCycleItemForm row : submittedRows) {
            if (row.getItemId() == null || !submittedIds.add(row.getItemId())) {
                throw new IllegalArgumentException("Có dòng nghĩa vụ bị thiếu hoặc lặp.");
            }
            BillingCycleItem item = itemsById.get(row.getItemId());
            if (item == null) {
                throw new IllegalArgumentException("Dòng nghĩa vụ không thuộc kỳ thu này.");
            }
            if (item.getProcessingResult() != BillingCycleItemResult.PENDING) {
                throw new IllegalArgumentException("Nghĩa vụ đã xử lý thì không thể sửa bằng form này.");
            }
            validateAmount(row.getElectricityAmount(), "tiền điện");
            validateAmount(row.getWaterAmount(), "tiền nước");
        }

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        for (BillingCycleItemForm row : submittedRows) {
            BillingCycleItem item = itemsById.get(row.getItemId());
            item.setElectricityAmount(row.getElectricityAmount());
            item.setWaterAmount(row.getWaterAmount());
            if (row.getElectricityAmount() != null && row.getWaterAmount() != null) {
                item.setInputCompletedAt(now);
                item.setEnteredByUser(admin);
            } else {
                item.setInputCompletedAt(null);
                item.setEnteredByUser(null);
            }
        }
        itemRepository.saveAll(databaseItems);
    }

    private BillingCycleItemsForm toForm(Long cycleId) {
        BillingCycle cycle = cycleRepository.findById(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỳ thu."));
        List<BillingCycleItem> items = itemRepository.findAllByBillingCycle_IdOrderByIdAsc(cycleId);
        Set<Long> roomIds = new HashSet<>();
        items.forEach(item -> roomIds.add(item.getContract().getRoom().getId()));
        Map<Long, RoomSummaryDto> rooms = roomQuery.findByRoomIds(roomIds);

        BillingCycleItemsForm form = new BillingCycleItemsForm();
        List<BillingCycleItemForm> rows = new ArrayList<>();
        for (BillingCycleItem item : items) {
            BillingCycleItemForm row = new BillingCycleItemForm();
            row.setItemId(item.getId());
            row.setContractId(item.getContract().getId());
            RoomSummaryDto room = rooms.get(item.getContract().getRoom().getId());
            row.setRoomCode(room == null ? "Phòng #" + item.getContract().getRoom().getId() : room.roomCode());
            row.setTenantName(item.getContract().getTenant().getFullName());
            row.setTenantEmail(item.getContract().getTenant().getEmail());
            row.setUsageFrom(item.getUsageFrom());
            row.setUsageTo(item.getUsageTo());
            row.setAgreedMonthlyRent(item.getContract().getAgreedMonthlyRent());
            row.setElectricityAmount(item.getElectricityAmount());
            row.setWaterAmount(item.getWaterAmount());
            row.setProcessingResult(item.getProcessingResult().name());
            row.setInputComplete(item.getInputCompletedAt() != null);
            row.setEditable(cycle.getStatus() == BillingCycleStatus.PENDING_INPUT
                && item.getProcessingResult() == BillingCycleItemResult.PENDING);
            rows.add(row);
        }
        form.setItems(rows);
        return form;
    }

    private BillingCycleOption toOption(BillingCycle cycle) {
        return new BillingCycleOption(cycle.getId(), cycle.getCollectionDate(), cycle.getStatus());
    }

    private void validateAmount(BigDecimal amount, String label) {
        if (amount == null) {
            return;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Số tiền " + label + " không được âm.");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Số tiền " + label + " phải là số nguyên đồng.");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.precision() - normalized.scale() > 19) {
            throw new IllegalArgumentException("Số tiền " + label + " vượt giới hạn lưu trữ.");
        }
    }

}
