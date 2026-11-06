package com.apartment.rental.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Display data and editable amounts for one contract row in the billing form. */
@Getter
@Setter
public class BillingCycleItemForm {

    private Long itemId;
    private Long contractId;
    private String roomCode;
    private String tenantName;
    private String tenantEmail;
    private LocalDate usageFrom;
    private LocalDate usageTo;
    private BigDecimal agreedMonthlyRent;
    private BigDecimal electricityAmount;
    private BigDecimal waterAmount;
    private String processingResult;
    private boolean inputComplete;
    private boolean editable;
}
