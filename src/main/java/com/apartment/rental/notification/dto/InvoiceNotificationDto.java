package com.apartment.rental.notification.dto;

import com.apartment.rental.notification.entity.NotificationStatus;

public record InvoiceNotificationDto(
    Long id,
    Long invoiceId,
    String title,
    String body,
    NotificationStatus status,
    String createdAt
) {
}
