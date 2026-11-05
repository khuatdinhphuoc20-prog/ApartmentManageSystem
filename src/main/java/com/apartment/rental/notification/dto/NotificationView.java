package com.apartment.rental.notification.dto;

import com.apartment.rental.notification.entity.Notification;
import com.apartment.rental.notification.entity.NotificationSourceType;
import com.apartment.rental.notification.entity.NotificationStatus;
import java.time.Instant;

public record NotificationView(
        Long id,
        NotificationSourceType sourceType,
        String title,
        String body,
        NotificationStatus status,
        Instant createdAt,
        Instant readAt) {

    public static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getSourceType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
