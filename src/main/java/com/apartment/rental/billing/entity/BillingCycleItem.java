package com.apartment.rental.billing.entity;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.common.persistence.AuditableEntity;
import com.apartment.rental.contract.entity.Contract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "billing_cycle_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingCycleItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_cycle_id", nullable = false)
    private BillingCycle billingCycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "usage_from", nullable = false)
    private LocalDate usageFrom;

    @Column(name = "usage_to", nullable = false)
    private LocalDate usageTo;

    @Column(name = "electricity_amount", nullable = true, precision = 19, scale = 0)
    private BigDecimal electricityAmount;

    @Column(name = "water_amount", nullable = true, precision = 19, scale = 0)
    private BigDecimal waterAmount;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "input_completed_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant inputCompletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "entered_by_user_id", nullable = true)
    private User enteredByUser;

    @Column(name = "rent_amount", nullable = true, precision = 19, scale = 0)
    private BigDecimal rentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_result", nullable = false, length = 16)
    private BillingCycleItemResult processingResult;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "processed_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant processedAt;
}

