package com.apartment.rental.application.repository;

import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.application.entity.RentalApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentalApplicationRepository  extends JpaRepository<RentalApplication, Long>, JpaSpecificationExecutor<RentalApplication> {
    List<RentalApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    boolean existsByApplicantIdAndRoomIdAndStatus(Long applicantId, Long roomId, RentalApplicationStatus status);

    List<RentalApplication> findByRoomIdAndStatus(Long roomId, RentalApplicationStatus status);

    List<RentalApplication> findByStatusAndRequestedStartDateLessThan(RentalApplicationStatus status, LocalDate today);

    @Query("""
            Select a from RentalApplication a 
            where (:status is null or a.status = :status) 
            and (:roomId is null or a.room.id = :roomId) 
            order by a.createdAt desc """)
    Page<RentalApplication> findApplicationsForAdmin(
            @Param("status") RentalApplicationStatus status,
            @Param("roomId") Long roomId,
            Pageable pageable);
}
