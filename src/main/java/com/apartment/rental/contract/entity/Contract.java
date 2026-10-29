package com.apartment.rental.contract.entity;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.common.persistence.AuditableEntity;
import com.apartment.rental.room.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "contracts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "application_id", nullable = true)
    private RentalApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(name = "agreed_monthly_rent", nullable = false, precision = 19, scale = 0)
    private BigDecimal agreedMonthlyRent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "planned_end_date", nullable = false)
    private LocalDate plannedEndDate;

    @Column(name = "effective_end_date", nullable = true)
    private LocalDate effectiveEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ContractStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", nullable = true, length = 32)
    private ContractEndReason endReason;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "activated_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant activatedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "ended_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "cancelled_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "closed_by_user_id", nullable = true)
    private User closedByUser;

    @Nationalized
    @Column(name = "closing_note", nullable = true, length = 1000)
    private String closingNote;

    @Column(name = "final_billing_period_end_date", nullable = true)
    private LocalDate finalBillingPeriodEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_settlement_result", nullable = true, length = 16)
    private FinalSettlementResult finalSettlementResult;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "settled_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant settledAt;
}

