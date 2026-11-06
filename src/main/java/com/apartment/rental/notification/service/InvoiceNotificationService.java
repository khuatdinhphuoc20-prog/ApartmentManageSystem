package com.apartment.rental.notification.service;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.invoice.dto.InvoiceIssuedEvent;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.notification.dto.InvoiceNotificationDto;
import com.apartment.rental.notification.entity.Notification;
import com.apartment.rental.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceNotificationService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    public InvoiceNotificationService(NotificationRepository notificationRepository, EntityManager entityManager) {
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInvoiceIssuedNotification(InvoiceIssuedEvent event) {
        String eventKey = "INVOICE_ISSUED:" + event.invoiceId();
        if (notificationRepository.existsByEventKeyAndRecipientUser_Id(eventKey, event.recipientUserId())) {
            return;
        }

        String total = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
            .format(event.totalAmount().longValueExact());
        String dueAt = DEADLINE_FORMAT.format(event.dueAt().atZone(BUSINESS_ZONE));
        String title = "Hóa đơn tiền phòng đã được phát hành";
        String body = "Hóa đơn #" + event.invoiceId() + " của phòng " + event.roomCode()
            + ", kỳ đến ngày " + event.billingPeriodEndDate() + ", tổng tiền " + total
            + " đồng. Hạn thanh toán: " + dueAt + " (UTC+7).";

        User recipient = entityManager.getReference(User.class, event.recipientUserId());
        Invoice invoice = entityManager.getReference(Invoice.class, event.invoiceId());
        notificationRepository.save(Notification.invoiceIssued(
            recipient,
            invoice,
            eventKey,
            title,
            body
        ));
    }

    @Transactional(readOnly = true)
    public List<InvoiceNotificationDto> findInvoiceNotifications(Long recipientUserId) {
        return notificationRepository
            .findAllByRecipientUser_IdAndInvoiceIsNotNullOrderByCreatedAtDesc(recipientUserId)
            .stream()
            .map(notification -> new InvoiceNotificationDto(
                notification.getId(),
                notification.getInvoice().getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStatus(),
                format(notification.getCreatedAt())
            ))
            .toList();
    }

    private String format(Instant value) {
        return DEADLINE_FORMAT.format(value.atZone(BUSINESS_ZONE));
    }
}
