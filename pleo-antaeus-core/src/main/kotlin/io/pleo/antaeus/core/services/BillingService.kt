package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "billing-executor") }
        .scheduleAtFixedRate(::processInvoices, 0, 1, TimeUnit.HOURS)

    private fun processInvoices() {
        val invoices = invoiceService.fetchDue()
        if (invoices.isEmpty()) return
        logger.info { "Processing ${invoices.size} invoices" }

        invoices.forEach { invoice ->
            try {
                if(paymentProvider.charge(invoice)) {
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
            } catch (e: Exception) {
                logger.error(e) { "Encountered exception while processing invoice ${invoice.id}" }
            }
        }
    }
}
