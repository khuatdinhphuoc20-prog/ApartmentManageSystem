package com.apartment.rental.notification.entity;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.contract.entity.Contract;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.issue.entity.Issue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime2(3)")
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime2(3)")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static Notification invoiceIssued(
        User recipient,
        Invoice invoice,
        String eventKey,
        String title,
        String body
    ) {
        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setSourceType(NotificationSourceType.SYSTEM);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setEventKey(eventKey);
        notification.setInvoice(invoice);
        return notification;
    }

    public static Notification cashPaymentRequested(
        User admin,
        Invoice invoice,
        String eventKey,
        String title,
        String body
    ) {
        Notification notification = new Notification();
        notification.setRecipientUser(admin);
        notification.setSourceType(NotificationSourceType.SYSTEM);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setEventKey(eventKey);
        notification.setInvoice(invoice);
        return notification;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private NotificationSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sender_user_id", nullable = true)
    private User senderUser;

    @Nationalized
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Nationalized
    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 8)
    private NotificationStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "read_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant readAt;

    @Column(name = "event_key", nullable = true, length = 200)
    private String eventKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "application_id", nullable = true)
    private RentalApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contract_id", nullable = true)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "invoice_id", nullable = true)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "issue_id", nullable = true)
    private Issue issue;
}

