package com.apartment.rental.notification.repository;

import com.apartment.rental.notification.entity.Notification;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUser_IdOrderByCreatedAtDescIdDesc(Long recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientUser_Id(Long id, Long recipientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :id and n.recipientUser.id = :recipientId")
    Optional<Notification> findForRecipientAndLock(@Param("id") Long id, @Param("recipientId") Long recipientId);

    boolean existsByEventKeyAndRecipientUser_Id(String eventKey, Long recipientUserId);

    boolean existsByEventKey(String eventKey);

    boolean existsByInvoice_IdAndEventKeyStartingWith(Long invoiceId, String eventKeyPrefix);

    @EntityGraph(attributePaths = "invoice")
    List<Notification> findAllByRecipientUser_IdAndInvoiceIsNotNullOrderByCreatedAtDesc(Long recipientUserId);
}
