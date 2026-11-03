package com.apartment.rental.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getTenantEmail() { return tenantEmail; }
    public void setTenantEmail(String tenantEmail) { this.tenantEmail = tenantEmail; }
    public LocalDate getUsageFrom() { return usageFrom; }
    public void setUsageFrom(LocalDate usageFrom) { this.usageFrom = usageFrom; }
    public LocalDate getUsageTo() { return usageTo; }
    public void setUsageTo(LocalDate usageTo) { this.usageTo = usageTo; }
    public BigDecimal getAgreedMonthlyRent() { return agreedMonthlyRent; }
    public void setAgreedMonthlyRent(BigDecimal agreedMonthlyRent) { this.agreedMonthlyRent = agreedMonthlyRent; }
    public BigDecimal getElectricityAmount() { return electricityAmount; }
    public void setElectricityAmount(BigDecimal electricityAmount) { this.electricityAmount = electricityAmount; }
    public BigDecimal getWaterAmount() { return waterAmount; }
    public void setWaterAmount(BigDecimal waterAmount) { this.waterAmount = waterAmount; }
    public String getProcessingResult() { return processingResult; }
    public void setProcessingResult(String processingResult) { this.processingResult = processingResult; }
    public boolean isInputComplete() { return inputComplete; }
    public void setInputComplete(boolean inputComplete) { this.inputComplete = inputComplete; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
}
