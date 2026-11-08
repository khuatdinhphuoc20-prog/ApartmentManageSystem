package com.apartment.rental.dashboard.service;

import com.apartment.rental.dashboard.dto.AdminDashboardSummary;
import com.apartment.rental.invoice.dto.InvoiceDashboardSummary;
import com.apartment.rental.invoice.port.InvoiceDashboardQueryPort;
import com.apartment.rental.issue.service.IssueService;
import com.apartment.rental.room.entity.RoomStatus;
import com.apartment.rental.room.port.RoomQueryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final RoomQueryPort roomQueryPort;
    private final InvoiceDashboardQueryPort invoiceDashboardQueryPort;
    private final IssueService issueService;

    public AdminDashboardService(
        RoomQueryPort roomQueryPort,
        InvoiceDashboardQueryPort invoiceDashboardQueryPort,
        IssueService issueService
    ) {
        this.roomQueryPort = roomQueryPort;
        this.invoiceDashboardQueryPort = invoiceDashboardQueryPort;
        this.issueService = issueService;
    }

    public AdminDashboardSummary loadSummary() {
        Map<RoomStatus, Long> roomCounts = roomQueryPort.countRoomsByStatus();
        InvoiceDashboardSummary invoices = invoiceDashboardQueryPort.getDashboardSummary();
        long unresolvedIssues = issueService.countUnresolvedIssues();

        long available = roomCounts.getOrDefault(RoomStatus.AVAILABLE, 0L);
        long rented = roomCounts.getOrDefault(RoomStatus.RENTED, 0L);
        long inactive = roomCounts.getOrDefault(RoomStatus.INACTIVE, 0L);
        long totalRooms = Math.addExact(Math.addExact(available, rented), inactive);
        long outstanding = Math.addExact(invoices.unpaidCount(), invoices.overdueCount());
        BigDecimal availablePercent = percentage(available, totalRooms);
        BigDecimal rentedPercent = percentage(rented, totalRooms);
        BigDecimal inactivePercent = totalRooms == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(100).subtract(availablePercent).subtract(rentedPercent);

        return new AdminDashboardSummary(
            totalRooms,
            available,
            rented,
            inactive,
            availablePercent,
            rentedPercent,
            inactivePercent,
            invoices.paidTotal(),
            invoices.unpaidCount(),
            invoices.overdueCount(),
            outstanding,
            unresolvedIssues
        );
    }

    private static BigDecimal percentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
