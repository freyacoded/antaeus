package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class BillingServiceTest {

    private val normalInvoice = createInvoice()
    private val rejectingInvoice = createInvoice()
    private val unknownCustomerInvoice = createInvoice()
    private val currencyErrorInvoice = createInvoice()
    private val networkErrorInvoice = createInvoice()

    private var counter = 0;
    private fun createInvoice(): Invoice {
        return Invoice(counter++, counter++, Money(BigDecimal.valueOf(100), Currency.USD), InvoiceStatus.PENDING, Date.from(Instant.now()))
    }

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(normalInvoice) } returns true
        every { charge(rejectingInvoice) } returns false
        every { charge(unknownCustomerInvoice) } throws CustomerNotFoundException(unknownCustomerInvoice.customerId)
        every { charge(currencyErrorInvoice) } throws CurrencyMismatchException(currencyErrorInvoice.id, currencyErrorInvoice.customerId)
        every { charge(networkErrorInvoice) } throws NetworkException()
    }
    private val invoiceService = mockk<InvoiceService> {
        // For whatever reason MockK requires these answers even though they return Unit
        every { markErrored(unknownCustomerInvoice) } returns Unit
        every { markErrored(currencyErrorInvoice) } returns Unit
    }

    private val billingService = BillingService(paymentProvider, invoiceService)

    @Test
    fun `can charge invoice`() {
        billingService.processInvoice(normalInvoice)
        verify {
            paymentProvider.charge(normalInvoice)
            invoiceService.markPaid(normalInvoice)
        }
    }

    @Test
    fun `can handle rejected charge`() {
        billingService.processInvoice(rejectingInvoice)
        verify {
            paymentProvider.charge(rejectingInvoice)
            invoiceService.markRejected(rejectingInvoice)
        }
        verify(exactly = 0) { invoiceService.markPaid(rejectingInvoice) }
    }

    @Test
    fun `cannot charge unknown customer`() {
        billingService.processInvoice(unknownCustomerInvoice)
        verify {
            paymentProvider.charge(unknownCustomerInvoice)
            invoiceService.markErrored(unknownCustomerInvoice)
        }
        verify(exactly = 0) { invoiceService.markPaid(unknownCustomerInvoice) }
    }

    @Test
    fun `cannot charge mismatching currency`() {
        billingService.processInvoice(currencyErrorInvoice)
        verify {
            paymentProvider.charge(currencyErrorInvoice)
            invoiceService.markErrored(currencyErrorInvoice)
        }
        verify(exactly = 0) { invoiceService.markPaid(currencyErrorInvoice) }
    }

    @Test
    fun `can handle network error`() {
        billingService.processInvoice(networkErrorInvoice)
        verify {
            paymentProvider.charge(networkErrorInvoice)
        }
        verify(exactly = 0) { invoiceService.markPaid(networkErrorInvoice) }
    }

}
