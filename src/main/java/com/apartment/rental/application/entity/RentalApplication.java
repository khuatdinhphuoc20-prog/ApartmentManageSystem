package com.apartment.rental.application.entity;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.common.persistence.AuditableEntity;
import com.apartment.rental.room.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "rental_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalApplication extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "requested_start_date", nullable = false)
    private LocalDate requestedStartDate;

    @Column(name = "requested_end_date", nullable = false)
    private LocalDate requestedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RentalApplicationStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "processed_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant processedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "processed_by_user_id", nullable = true)
    private User processedByUser;

    @Nationalized
    @Column(name = "processing_note", nullable = true, length = 1000)
    private String processingNote;
}

