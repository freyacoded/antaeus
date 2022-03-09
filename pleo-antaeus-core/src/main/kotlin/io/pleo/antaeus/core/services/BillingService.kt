package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "billing-executor") }
            .scheduleAtFixedRate(::processInvoices, 0, 1, TimeUnit.HOURS)
    }

    private fun processInvoices() {
        try {
            val invoices = invoiceService.fetchDue()
            if (invoices.isEmpty()) return
            logger.info { "Processing ${invoices.size} invoices" }

            invoices.forEach { processInvoice(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error processing invoices" }
        }
    }

    internal fun processInvoice(invoice: Invoice) {
        try {
            if (paymentProvider.charge(invoice)) {
                logger.info { "Invoice ${invoice.id} paid" }
                invoiceService.markPaid(invoice)
            } else {
                logger.info { "Invoice ${invoice.id} rejected by payment processor" }
                invoiceService.markRejected(invoice)
            }
        } catch (e: CustomerNotFoundException) {
            logger.error(e) { "Attempt to charge invoice ${invoice.id} with unknown customer ${invoice.customerId}" }
            invoiceService.markErrored(invoice)
        } catch (e: CurrencyMismatchException) {
            logger.error(e) { "Attempt to charge invoice ${invoice.id} of customer ${invoice.customerId} with a mismatching currency" }
            invoiceService.markErrored(invoice)
            println("test!")
        } catch (e: Exception) {
            logger.error(e) { "Encountered exception while processing invoice ${invoice.id}" }
        }
    }
}
