package io.pleo.antaeus.models

enum class InvoiceStatus {
    /** This invoice is not yet paid, although it may not be due yet */
    PENDING,
    /** This invoice has been paid*/
    PAID,
    /** The payment processor has rejected the charge, e.g. because of missing funds */
    PAYMENT_REJECTED,
    /** The billing service ran into an irrecoverable error */
    ERROR
}
