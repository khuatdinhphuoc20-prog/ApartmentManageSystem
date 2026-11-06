package com.apartment.rental.notification.repository;

import com.apartment.rental.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventKeyAndRecipientUser_Id(String eventKey, Long recipientUserId);

    boolean existsByEventKey(String eventKey);

    boolean existsByInvoice_IdAndEventKeyStartingWith(Long invoiceId, String eventKeyPrefix);


    @EntityGraph(attributePaths = "invoice")
    List<Notification> findAllByRecipientUser_IdAndInvoiceIsNotNullOrderByCreatedAtDesc(Long recipientUserId);
}
