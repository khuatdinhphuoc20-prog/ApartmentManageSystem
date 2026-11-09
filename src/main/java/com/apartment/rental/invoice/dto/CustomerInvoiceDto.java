package com.apartment.rental.invoice.dto;

import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.invoice.entity.InvoiceKind;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.entity.RentCalculationRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public record CustomerInvoiceDto(
    Long id,
    Long contractId,
    String roomCode,
    ContractStatus contractStatus,
    InvoiceKind invoiceKind,
    LocalDate billingPeriodEndDate,
    LocalDate usageFrom,
    LocalDate usageTo,
    BigDecimal monthlyRentSnapshot,
    RentCalculationRule rentCalculationRule,
    Integer proratedDays,
    BigDecimal rentAmount,
    BigDecimal electricityAmount,
    BigDecimal waterAmount,
    BigDecimal totalAmount,
    InvoiceStatus status,
    String issuedAt,
    String dueAt,
    String paidAt,
    String paymentMethodLabel,
    String paymentReference,
    boolean paymentAvailable,
    boolean cashPaymentRequested
) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String getContractStatusLabel() {
        return switch (contractStatus) {
            case SCHEDULED -> "Sắp bắt đầu";
            case ACTIVE -> "Đang thuê";
            case ENDED -> "Đã kết thúc";
            case CANCELLED -> "Đã hủy";
        };
    }

    public String getInvoiceKindLabel() {
        return switch (invoiceKind) {
            case REGULAR -> "Hóa đơn định kỳ";
            case EARLY_SETTLEMENT -> "Thanh toán khi kết thúc hợp đồng";
        };
    }

    public String getRentCalculationRuleLabel() {
        return switch (rentCalculationRule) {
            case FIRST_PRORATED -> "Theo số ngày sử dụng trong tháng đầu";
            case PERIOD_PRORATED -> "Theo số ngày của kỳ sử dụng rút ngắn";
            case MONTHLY_FULL -> "Trọn tháng";
            case TENANT_EARLY_FULL -> "Trọn tháng khi kết thúc hợp đồng sớm";
            case ADMIN_STOP_PRORATED -> "Theo số ngày ở đến ngày kết thúc hợp đồng";
        };
    }

    public String getBillingPeriodEndDateLabel() {
        return billingPeriodEndDate.format(DATE_FORMAT);
    }

    public String getUsagePeriodLabel() {
        return usageFrom.format(DATE_FORMAT) + " đến trước " + usageTo.format(DATE_FORMAT);
    }
}
