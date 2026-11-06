package com.apartment.rental.billing.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import com.apartment.rental.notification.entity.Notification;
import com.apartment.rental.notification.repository.NotificationRepository;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerCashPaymentRequestService {

    private static final String EVENT_PREFIX = "CASH_PAYMENT_REQUEST:";

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationRepository notificationRepository;

    public CustomerCashPaymentRequestService(
        UserRepository userRepository,
        InvoiceRepository invoiceRepository,
        NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public boolean requestCashPayment(Long invoiceId, String customerEmail) {
        User tenant = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản của bạn. Vui lòng đăng nhập lại."));
        if (tenant.getRole() != Role.USER) {
            throw new AccessDeniedException("Chỉ tài khoản người thuê mới có thể yêu cầu thanh toán tiền mặt.");
        }

        Invoice invoice = invoiceRepository.findByIdAndContract_Tenant_IdForUpdate(invoiceId, tenant.getId())
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hóa đơn trong tài khoản của bạn."));
        if (invoice.getStatus() != InvoiceStatus.UNPAID && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán nên không thể gửi yêu cầu thu tiền mặt.");
        }

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        String eventKey = EVENT_PREFIX + invoiceId + ":" + tenant.getId();
        String title = "Khách thuê đề nghị thanh toán tiền mặt";
        String total = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
            .format(invoice.getTotalAmount().longValueExact());
        String body = tenant.getFullName() + " (" + tenant.getEmail() + ") đề nghị thanh toán tiền mặt hóa đơn #"
            + invoiceId + " phòng " + invoice.getContract().getRoom().getRoomCode()
            + ", số tiền " + total + " đồng. Hóa đơn chỉ được cập nhật sau khi Admin xác nhận đã nhận đủ tiền.";

        boolean created = false;
        for (User admin : admins) {
            if (notificationRepository.existsByEventKeyAndRecipientUser_Id(eventKey, admin.getId())) {
                continue;
            }
            notificationRepository.save(Notification.cashPaymentRequested(
                admin,
                invoice,
                eventKey,
                title,
                body
            ));
            created = true;
        }
        return created;
    }

    @Transactional(readOnly = true)
    public boolean hasCustomerRequestedCashPayment(Long invoiceId, Long tenantId) {
        return notificationRepository.existsByEventKey(EVENT_PREFIX + invoiceId + ":" + tenantId);
    }

    @Transactional(readOnly = true)
    public boolean hasCashPaymentRequest(Long invoiceId) {
        return notificationRepository.existsByInvoice_IdAndEventKeyStartingWith(invoiceId, EVENT_PREFIX);
    }
}
