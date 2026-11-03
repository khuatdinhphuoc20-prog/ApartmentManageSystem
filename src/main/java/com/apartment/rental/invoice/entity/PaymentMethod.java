package com.apartment.rental.invoice.entity;

public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    /** Legacy database value. Kept so JPA can read historical invoices; never write for new payments. */
    SANDBOX
}

