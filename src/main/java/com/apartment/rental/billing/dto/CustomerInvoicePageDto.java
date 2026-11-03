package com.apartment.rental.billing.dto;

import com.apartment.rental.notification.dto.InvoiceNotificationDto;
import java.util.List;

public record CustomerInvoicePageDto(
    String customerName,
    List<CustomerInvoiceDto> invoices,
    List<InvoiceNotificationDto> notifications
) {
}
