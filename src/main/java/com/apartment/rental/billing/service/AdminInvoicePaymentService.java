package com.apartment.rental.billing.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.invoice.dto.OutstandingInvoiceDto;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.entity.PaymentMethod;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminInvoicePaymentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerCashPaymentRequestService cashPaymentRequestService;

    public AdminInvoicePaymentService(
        UserRepository userRepository,
        InvoiceRepository invoiceRepository,
        CustomerCashPaymentRequestService cashPaymentRequestService
    ) {
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.cashPaymentRequestService = cashPaymentRequestService;
    }

    @Transactional(readOnly = true)
    public List<OutstandingInvoiceDto> findOutstandingInvoices(String adminEmail) {
        requireAdmin(adminEmail);
        return invoiceRepository.findAllByStatusInOrderByDueAtAscIdAsc(
                List.of(InvoiceStatus.UNPAID, InvoiceStatus.OVERDUE)
            ).stream()
            .map(invoice -> new OutstandingInvoiceDto(
                invoice.getId(),
                invoice.getContract().getRoom().getRoomCode(),
                invoice.getContract().getTenant().getFullName(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                DATE_TIME_FORMAT.format(invoice.getDueAt().atZone(BUSINESS_ZONE)),
                cashPaymentRequestService.hasCashPaymentRequest(invoice.getId())
            ))
            .toList();
    }

    @Transactional
    public void recordReceivedPayment(
        Long invoiceId,
        String adminEmail,
        BigDecimal receivedAmount,
        PaymentMethod paymentMethod,
        String paymentReference,
        LocalDateTime paidAt
    ) {
        User admin = requireAdmin(adminEmail);
        if (receivedAmount == null || receivedAmount.signum() <= 0
            || paymentMethod == null
            || (paymentMethod != PaymentMethod.CASH && paymentMethod != PaymentMethod.BANK_TRANSFER)
            || paidAt == null) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền, chọn phương thức thanh toán và chọn thời điểm nhận tiền.");
        }
        if (paymentReference != null && paymentReference.isBlank()) {
            paymentReference = null;
        }
        if (paymentReference != null && paymentReference.length() > 128) {
            throw new IllegalArgumentException("Mã tham chiếu không được dài quá 128 ký tự.");
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn cần ghi nhận."));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán trước đó.");
        }
        if (invoice.getStatus() != InvoiceStatus.UNPAID && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new IllegalStateException("Hóa đơn hiện không thể ghi nhận thanh toán.");
        }
        if (invoice.getTotalAmount().compareTo(receivedAmount) != 0) {
            throw new IllegalArgumentException("Số tiền đã nhận cần khớp với tổng tiền trên hóa đơn.");
        }

        Instant receivedAt = paidAt.atZone(BUSINESS_ZONE).toInstant();
        if (receivedAt.isBefore(invoice.getIssuedAt()) || receivedAt.isAfter(Instant.now().plusSeconds(300))) {
            throw new IllegalArgumentException("Thời điểm nhận tiền không hợp lệ. Vui lòng kiểm tra lại.");
        }
        if (paymentReference != null && invoiceRepository.existsByPaymentReference(paymentReference)) {
            throw new IllegalArgumentException("Mã tham chiếu này đã được dùng cho một hóa đơn khác.");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(receivedAt);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setPaymentReference(paymentReference);
        invoice.setConfirmedByUser(admin);
    }

    private User requireAdmin(String email) {
        User admin = userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản quản trị."));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền ghi nhận thanh toán.");
        }
        return admin;
    }
}
