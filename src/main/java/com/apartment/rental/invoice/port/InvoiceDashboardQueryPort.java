package com.apartment.rental.invoice.port;

import com.apartment.rental.invoice.dto.InvoiceDashboardSummary;

/** Read-only invoice figures provided by M05 for the M06 admin dashboard. */
public interface InvoiceDashboardQueryPort {

    InvoiceDashboardSummary getDashboardSummary();
}
