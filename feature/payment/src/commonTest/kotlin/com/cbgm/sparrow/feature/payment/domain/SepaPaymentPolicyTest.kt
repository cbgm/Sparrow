package com.cbgm.sparrow.feature.payment.domain

import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.model.SepaPayment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SepaPaymentPolicyTest {
    private val bank = BankDetails("Test Recipient", "DE89370400440532013000")

    @Test fun createsExactEpcPayloadInMinorUnits() {
        assertEquals(
            "BCD\n002\n1\nSCT\n\nTest Recipient\nDE89370400440532013000\nEUR12.05\n\n\nSPARROW-test",
            SepaPaymentPolicy.epcPayload(SepaPayment(bank, 1205L, "SPARROW-test"))
        )
        assertEquals("0.01", SepaPaymentPolicy.amount(1L))
        assertEquals("999999999.99", SepaPaymentPolicy.amount(99_999_999_999L))
    }

    @Test fun normalizesSpacesAndCaseAndSupportsEeaAccounts() {
        assertEquals(bank, SepaPaymentPolicy.normalize(BankDetails(" Test Recipient ", "de89 3704 0044 0532 0130 00")))
        assertEquals("FR1420041010050500013M02606", SepaPaymentPolicy.normalize(BankDetails("Test", "FR1420041010050500013M02606")).iban)
    }

    @Test fun rejectsInvalidChecksumAndUnsupportedCountry() {
        assertFailsWith<IllegalArgumentException> { SepaPaymentPolicy.normalize(bank.copy(iban = "DE89370400440532013001")) }
        assertFailsWith<IllegalArgumentException> { SepaPaymentPolicy.normalize(bank.copy(iban = "CH9300762011623852957")) }
    }

    @Test fun rejectsQrFieldInjectionAndInvalidAmounts() {
        for (name in listOf("", "Test\nEUR10000", "Test\rName", "A".repeat(71))) {
            assertFailsWith<IllegalArgumentException> { SepaPaymentPolicy.normalize(bank.copy(accountHolder = name)) }
        }
        for (amount in listOf(-1L, 0L, 100_000_000_000L)) {
            assertFailsWith<IllegalArgumentException> { SepaPaymentPolicy.epcPayload(SepaPayment(bank, amount, "SPARROW-test")) }
        }
        assertFailsWith<IllegalArgumentException> { SepaPaymentPolicy.epcPayload(SepaPayment(bank, 100L, "SPARROW-test\nother")) }
    }

    @Test fun enforcesUtf8ByteLimit() {
        assertFailsWith<IllegalArgumentException> {
            SepaPaymentPolicy.epcPayload(SepaPayment(bank.copy(accountHolder = "界".repeat(70)), 99_999_999_999L, "SPARROW-" + "A".repeat(60)))
        }
    }

    @Test fun sensitiveModelsDoNotPrintBankDetails() {
        assertFalse(bank.toString().contains(bank.iban))
        assertFalse(SepaPayment(bank, 1L, "SPARROW-test").toString().contains(bank.accountHolder))
    }
}
