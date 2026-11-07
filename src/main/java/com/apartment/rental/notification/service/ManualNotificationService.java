package com.apartment.rental.notification.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.notification.entity.Notification;
import com.apartment.rental.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualNotificationService {

    private final EntityManager entityManager;
    private final NotificationRepository notificationRepository;

    public ManualNotificationService(EntityManager entityManager, NotificationRepository notificationRepository) {
        this.entityManager = entityManager;
        this.notificationRepository = notificationRepository;
    }

    /** The actor must come from the server's authenticated context, never from a request field. */
    @Transactional
    public List<Long> sendManual(User actor, String title, String body, Collection<Long> recipientIds) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Authenticated admin is required");
        }

        User sender = entityManager.find(User.class, actor.getId());
        if (sender == null || sender.getRole() != Role.ADMIN) {
            throw new SecurityException("Only an admin can send manual notifications");
        }

        requireContent(title, 200, "title");
        requireContent(body, 4000, "body");

        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        Set<Long> distinctIds = new LinkedHashSet<>(recipientIds);
        if (distinctIds.contains(null) || distinctIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalArgumentException("Recipient IDs must be positive");
        }

        List<Notification> notifications = new ArrayList<>(distinctIds.size());
        for (Long recipientId : distinctIds) {
            User recipient = entityManager.find(User.class, recipientId);
            if (recipient == null || recipient.getRole() != Role.USER) {
                throw new IllegalArgumentException("Recipient does not exist or is not a user: " + recipientId);
            }
            notifications.add(Notification.manual(sender, recipient, title, body));
        }

        return notificationRepository.saveAllAndFlush(notifications).stream()
                .map(Notification::getId)
                .toList();
    }

    private static void requireContent(String value, int maximumLength, String field) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " must be nonblank and at most " + maximumLength + " characters");
        }
    }
}
