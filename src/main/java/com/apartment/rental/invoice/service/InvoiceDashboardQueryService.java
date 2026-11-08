package com.apartment.rental.invoice.service;

import com.apartment.rental.invoice.dto.InvoiceDashboardSummary;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.port.InvoiceDashboardQueryPort;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceDashboardQueryService implements InvoiceDashboardQueryPort {

    private final InvoiceRepository invoiceRepository;

    public InvoiceDashboardQueryService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDashboardSummary getDashboardSummary() {
        BigDecimal paidTotal = invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.PAID);
        return new InvoiceDashboardSummary(
            paidTotal == null ? BigDecimal.ZERO : paidTotal,
            invoiceRepository.countByStatus(InvoiceStatus.UNPAID),
            invoiceRepository.countByStatus(InvoiceStatus.OVERDUE)
        );
    }
}
