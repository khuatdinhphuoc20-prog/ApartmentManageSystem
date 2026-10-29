package com.apartment.rental.billing.entity;

import com.apartment.rental.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "billing_cycles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingCycle extends AuditableEntity {

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillingCycleStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "completed_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant completedAt;
}

