package com.apartment.rental.invoice.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.billing.service.CustomerCashPaymentRequestService;
import com.apartment.rental.invoice.dto.CustomerInvoiceDto;
import com.apartment.rental.invoice.dto.CustomerInvoicePageDto;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.entity.PaymentMethod;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import com.apartment.rental.notification.dto.InvoiceNotificationDto;
import com.apartment.rental.notification.service.InvoiceNotificationService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerInvoiceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceNotificationService notificationService;
    private final CustomerCashPaymentRequestService cashPaymentRequestService;

    public CustomerInvoiceService(
        UserRepository userRepository,
        InvoiceRepository invoiceRepository,
        InvoiceNotificationService notificationService,
        CustomerCashPaymentRequestService cashPaymentRequestService
    ) {
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationService = notificationService;
        this.cashPaymentRequestService = cashPaymentRequestService;
    }

    @Transactional(readOnly = true)
    public CustomerInvoicePageDto getMyInvoices(String email) {
        User customer = userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản khách thuê."));
        if (customer.getRole() != Role.USER) {
            throw new AccessDeniedException("Chỉ tài khoản khách thuê được xem trang này.");
        }

        Instant now = Instant.now();
        List<CustomerInvoiceDto> invoices = invoiceRepository
            .findAllByContract_Tenant_IdOrderByBillingPeriodEndDateDescIdDesc(customer.getId())
            .stream()
            .map(invoice -> toDto(invoice, now, customer.getId()))
            .toList();
        List<InvoiceNotificationDto> notifications = notificationService.findInvoiceNotifications(customer.getId());
        return new CustomerInvoicePageDto(customer.getFullName(), invoices, notifications);
    }

    private CustomerInvoiceDto toDto(Invoice invoice, Instant now, Long customerId) {
        return new CustomerInvoiceDto(
            invoice.getId(),
            invoice.getContract().getId(),
            invoice.getContract().getRoom().getRoomCode(),
            invoice.getContract().getStatus(),
            invoice.getInvoiceKind(),
            invoice.getBillingPeriodEndDate(),
            invoice.getUsageFrom(),
            invoice.getUsageTo(),
            invoice.getMonthlyRentSnapshot(),
            invoice.getRentCalculationRule(),
            invoice.getProratedDays(),
            invoice.getRentAmount(),
            invoice.getElectricityAmount(),
            invoice.getWaterAmount(),
            invoice.getTotalAmount(),
            invoice.getStatus(),
            format(invoice.getIssuedAt()),
            format(invoice.getDueAt()),
            invoice.getPaidAt() == null ? null : format(invoice.getPaidAt()),
            paymentMethodLabel(invoice),
            invoice.getPaymentReference(),
            invoice.getStatus() == InvoiceStatus.UNPAID && now.isBefore(invoice.getDueAt()),
            cashPaymentRequestService.hasCustomerRequestedCashPayment(invoice.getId(), customerId)
        );
    }

    private String format(Instant value) {
        return DATE_TIME_FORMAT.format(value.atZone(BUSINESS_ZONE));
    }

    private String paymentMethodLabel(Invoice invoice) {
        PaymentMethod method = invoice.getPaymentMethod();
        if (method == null) {
            return null;
        }
        return switch (method) {
            case CASH -> "Tiền mặt";
            case SANDBOX -> "Thanh toán online";
            case BANK_TRANSFER -> invoice.getConfirmedByUser() == null
                ? "Thanh toán online qua VNPay"
                : "Chuyển khoản ngân hàng";
        };
    }
}
