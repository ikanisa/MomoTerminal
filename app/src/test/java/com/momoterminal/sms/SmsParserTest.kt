package com.momoterminal.sms

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Parameterized tests for SmsParser covering all providers.
 */
@RunWith(Parameterized::class)
class SmsParserTest(
    private val testName: String,
    private val sender: String,
    private val body: String,
    private val expectedProvider: String?,
    private val expectedType: SmsParser.TransactionType?,
    private val expectedAmount: Double?,
    private val expectedParty: String?,
    private val expectedTransactionId: String?
) {

    @Test
    fun `parse SMS message`() {
        val result = SmsParser.parseSms(sender, body)
        
        if (expectedProvider == null) {
            // Expected to fail parsing
            assertThat(result).isNull()
        } else {
            assertThat(result).isNotNull()
            assertThat(result!!.provider).isEqualTo(expectedProvider)
            assertThat(result.transactionType).isEqualTo(expectedType)
            assertThat(result.amount).isWithin(0.01).of(expectedAmount!!)
            
            if (expectedParty != null) {
                assertThat(result.senderOrRecipient).isEqualTo(expectedParty)
            }
            
            if (expectedTransactionId != null) {
                assertThat(result.transactionId).isEqualTo(expectedTransactionId)
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testCases() = listOf(
            // MTN MoMo - Received
            arrayOf(
                "MTN received payment",
                "MTN MoMo",
                "You have received GHS 50.00 from John Doe. Trans ID: MP123456789",
                "MTN",
                SmsParser.TransactionType.RECEIVED,
                50.00,
                "John Doe",
                "MP123456789"
            ),
            arrayOf(
                "MTN received with balance",
                "MTN",
                "Received GHS 100.50 from 0244123456. Trans ID: MP999888777. Balance is GHS 500.00",
                "MTN",
                SmsParser.TransactionType.RECEIVED,
                100.50,
                "0244123456",
                "MP999888777"
            ),
            arrayOf(
                "MTN received with comma amount",
                "MTN MoMo",
                "You have received GHS 1,500.00 from Merchant ABC. Transaction ID: TX12345",
                "MTN",
                SmsParser.TransactionType.RECEIVED,
                1500.00,
                "Merchant ABC",
                "TX12345"
            ),
            // MTN MoMo - Sent
            arrayOf(
                "MTN sent payment",
                "MTN MoMo",
                "You have sent GHS 200.00 to Jane Smith. Trans ID: MP987654321",
                "MTN",
                SmsParser.TransactionType.SENT,
                200.00,
                "Jane Smith",
                "MP987654321"
            ),
            arrayOf(
                "MTN transfer",
                "MTN",
                "Transfer of GHS 75.00 to 0201234567. Transaction ID: TF1234",
                "MTN",
                SmsParser.TransactionType.SENT,
                75.00,
                "0201234567",
                "TF1234"
            ),
            // MTN MoMo - Payment
            arrayOf(
                "MTN payment to merchant",
                "MTN MoMo",
                "Payment of GHS 30.00 to ShopRite. Trans ID: PAY123",
                "MTN",
                SmsParser.TransactionType.PAYMENT,
                30.00,
                "ShopRite",
                "PAY123"
            ),
            // Vodafone Cash - Received
            arrayOf(
                "Vodafone received payment",
                "VodaCash",
                "Received GHS 80.00 from 0201234567. Ref: VF123456",
                "VODAFONE",
                SmsParser.TransactionType.RECEIVED,
                80.00,
                "0201234567",
                "VF123456"
            ),
            arrayOf(
                "Vodafone received with balance",
                "Vodafone Cash",
                "You have received GHS 250.00 from Customer ABC. Reference: REF999. Available balance GHS 1000.00",
                "VODAFONE",
                SmsParser.TransactionType.RECEIVED,
                250.00,
                "Customer ABC",
                "REF999"
            ),
            // Vodafone Cash - Sent
            arrayOf(
                "Vodafone sent payment",
                "Vodafone",
                "Sent GHS 45.00 to John Doe. Ref No: VF789",
                "VODAFONE",
                SmsParser.TransactionType.SENT,
                45.00,
                "John Doe",
                "VF789"
            ),
            arrayOf(
                "Vodafone transferred",
                "VCash",
                "Transferred GHS 500.00 to 0271234567. Reference: T123",
                "VODAFONE",
                SmsParser.TransactionType.SENT,
                500.00,
                "0271234567",
                "T123"
            ),
            // AirtelTigo Money - Received
            arrayOf(
                "AirtelTigo credited",
                "AirtelTigo",
                "Credited GHS 60.00 from 0262345678. Trans ID: AT123",
                "AIRTELTIGO",
                SmsParser.TransactionType.RECEIVED,
                60.00,
                "0262345678",
                null
            ),
            arrayOf(
                "AirtelTigo received",
                "AT Money",
                "Received GHS 150.00 from Customer XYZ. Bal GHS 800.00",
                "AIRTELTIGO",
                SmsParser.TransactionType.RECEIVED,
                150.00,
                "Customer XYZ",
                null
            ),
            // AirtelTigo Money - Sent
            arrayOf(
                "AirtelTigo debited",
                "AirtelTigo",
                "Debited GHS 35.00 to 0571234567. Balance is GHS 200.00",
                "AIRTELTIGO",
                SmsParser.TransactionType.SENT,
                35.00,
                "0571234567",
                null
            ),
            // Unknown/Invalid messages
            arrayOf(
                "Unknown sender",
                "Unknown Bank",
                "You received 100.00 USD",
                null,
                null,
                null,
                null,
                null
            ),
            arrayOf(
                "Non-payment message",
                "MTN",
                "Hello, your airtime balance is low",
                null,
                null,
                null,
                null,
                null
            )
        )
    }
}

/**
 * Additional non-parameterized tests for SmsParser.
 */
class SmsParserAdditionalTest {

    @Test
    fun `isMobileMoneyMessage returns true for MTN message`() {
        assertThat(SmsParser.isMobileMoneyMessage("MTN", "Payment received")).isTrue()
    }

    @Test
    fun `isMobileMoneyMessage returns true for Vodafone message`() {
        assertThat(SmsParser.isMobileMoneyMessage("VCash", "Amount credited")).isTrue()
    }

    @Test
    fun `isMobileMoneyMessage returns true for AirtelTigo message`() {
        assertThat(SmsParser.isMobileMoneyMessage("AT Money", "Transfer complete")).isTrue()
    }

    @Test
    fun `isMobileMoneyMessage returns true for GHS amount`() {
        assertThat(SmsParser.isMobileMoneyMessage("Bank", "You received GHS 50")).isTrue()
    }

    @Test
    fun `isMobileMoneyMessage returns true for GHC amount`() {
        assertThat(SmsParser.isMobileMoneyMessage("Bank", "Payment of GHC 100")).isTrue()
    }

    @Test
    fun `isMobileMoneyMessage returns false for unrelated message`() {
        assertThat(SmsParser.isMobileMoneyMessage("Friend", "Hello, how are you?")).isFalse()
    }

    @Test
    fun `parseSms returns correct currency`() {
        val result = SmsParser.parseSms("MTN", "Received GHS 50.00 from John")
        assertThat(result?.currency).isEqualTo("GHS")
    }

    @Test
    fun `parseSms extracts balance correctly`() {
        val result = SmsParser.parseSms(
            "MTN MoMo",
            "Received GHS 50.00 from John. Balance is GHS 1500.00"
        )
        assertThat(result?.balance).isWithin(0.01).of(1500.00)
    }

    @Test
    fun `parseSms handles amount with no decimals`() {
        val result = SmsParser.parseSms(
            "MTN",
            "Received GHS 100 from John. Trans ID: T1"
        )
        assertThat(result?.amount).isWithin(0.01).of(100.0)
    }

    @Test
    fun `parseSms handles large amounts with commas`() {
        val result = SmsParser.parseSms(
            "MTN MoMo",
            "Received GHS 10,000.50 from Merchant. Trans ID: T1"
        )
        assertThat(result?.amount).isWithin(0.01).of(10000.50)
    }

    @Test
    fun `parseSms rawMessage contains original body`() {
        val body = "Received GHS 50.00 from John"
        val result = SmsParser.parseSms("MTN", body)
        assertThat(result?.rawMessage).isEqualTo(body)
    }

    @Test
    fun `parseSms timestamp is set`() {
        val beforeTime = System.currentTimeMillis()
        val result = SmsParser.parseSms("MTN", "Received GHS 50.00 from John")
        val afterTime = System.currentTimeMillis()
        
        assertThat(result?.timestamp).isAtLeast(beforeTime)
        assertThat(result?.timestamp).isAtMost(afterTime)
    }
}
