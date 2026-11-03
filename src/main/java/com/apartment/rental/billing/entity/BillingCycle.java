package com.apartment.rental.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
public class BillingCycle {

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

    public static BillingCycle pendingInput(LocalDate collectionDate) {
        BillingCycle cycle = new BillingCycle();
        cycle.setCollectionDate(collectionDate);
        cycle.setStatus(BillingCycleStatus.PENDING_INPUT);
        return cycle;
    }

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillingCycleStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "completed_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant completedAt;
}

