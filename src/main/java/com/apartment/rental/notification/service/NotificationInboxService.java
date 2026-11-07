package com.apartment.rental.notification.service;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.notification.dto.NotificationView;
import com.apartment.rental.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationInboxService {

    private final NotificationRepository notificationRepository;

    public NotificationInboxService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** The actor must come from the server's authenticated context, never from a request field. */
    @Transactional(readOnly = true)
    public Page<NotificationView> listMine(User actor, Pageable pageable) {
        Long actorId = requireActorId(actor);
        if (pageable == null || pageable.isUnpaged() || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("A page size from 1 to 100 is required");
        }
        return notificationRepository.findByRecipientUser_IdOrderByCreatedAtDescIdDesc(actorId, pageable)
                .map(NotificationView::from);
    }

    @Transactional(readOnly = true)
    public NotificationView getMine(User actor, Long notificationId) {
        Long actorId = requireActorId(actor);
        if (notificationId == null || notificationId <= 0) {
            throw new NoSuchElementException("Notification not found");
        }
        return notificationRepository.findByIdAndRecipientUser_Id(notificationId, actorId)
                .map(NotificationView::from)
                .orElseThrow(() -> new NoSuchElementException("Notification not found"));
    }

    @Transactional
    public NotificationView markRead(User actor, Long notificationId) {
        Long actorId = requireActorId(actor);
        if (notificationId == null || notificationId <= 0) {
            throw new NoSuchElementException("Notification not found");
        }
        var notification = notificationRepository.findForRecipientAndLock(notificationId, actorId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found"));
        notification.markRead(Instant.now());
        return NotificationView.from(notification);
    }

    private static Long requireActorId(User actor) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Authenticated user is required");
        }
        return actor.getId();
    }
}
