package com.apartment.rental.invoice.entity;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.billing.entity.BillingCycleItem;
import com.apartment.rental.common.persistence.AuditableEntity;
import com.apartment.rental.contract.entity.Contract;
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
@Table(name = "invoices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "billing_cycle_item_id", nullable = true)
    private BillingCycleItem billingCycleItem;

    @Column(name = "billing_period_end_date", nullable = false)
    private LocalDate billingPeriodEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_kind", nullable = false, length = 24)
    private InvoiceKind invoiceKind;

    @Column(name = "usage_from", nullable = false)
    private LocalDate usageFrom;

    @Column(name = "usage_to", nullable = false)
    private LocalDate usageTo;

    @Column(name = "monthly_rent_snapshot", nullable = false, precision = 19, scale = 0)
    private BigDecimal monthlyRentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "rent_calculation_rule", nullable = false, length = 32)
    private RentCalculationRule rentCalculationRule;

    @Column(name = "prorated_days", nullable = true)
    private Integer proratedDays;

    @Column(name = "rent_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal rentAmount;

    @Column(name = "electricity_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal electricityAmount;

    @Column(name = "water_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal waterAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "issued_at", nullable = false, columnDefinition = "datetime2(3)")
    private Instant issuedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "due_at", nullable = false, columnDefinition = "datetime2(3)")
    private Instant dueAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "paid_at", nullable = true, columnDefinition = "datetime2(3)")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = true, length = 24)
    private PaymentMethod paymentMethod;

    @Nationalized
    @Column(name = "payment_reference", nullable = true, length = 128)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "confirmed_by_user_id", nullable = true)
    private User confirmedByUser;
}

