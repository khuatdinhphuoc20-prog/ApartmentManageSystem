package com.apartment.rental.billing.dto;

import com.apartment.rental.notification.dto.InvoiceNotificationDto;
import java.util.List;

/** All data needed to display the customer's invoice page. */
public record CustomerInvoicePageDto(
    String customerName,
    List<CustomerInvoiceDto> invoices,
    List<InvoiceNotificationDto> notifications
) {
}
