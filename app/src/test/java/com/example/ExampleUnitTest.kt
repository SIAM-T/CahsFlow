package com.example

import com.example.data.local.AppSettingsEntity
import com.example.data.local.HisabDatabase
import com.example.data.local.LoanEntity
import com.example.data.local.LoanStatus
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinancialEngine
import com.example.util.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun moneyUtils_parsesAndFormatsExactPaisaWithoutFloatingPointErrors() {
        assertEquals(500_00L, MoneyUtils.parseToPaisa("500"))
        assertEquals(1250_50L, MoneyUtils.parseToPaisa("1,250.50"))
        assertEquals(500_00L, MoneyUtils.parseToPaisa("৫০০")) // Bangla digits
        assertNull(MoneyUtils.parseToPaisa("-100"))
        assertNull(MoneyUtils.parseToPaisa("abc"))

        assertEquals("৳500", MoneyUtils.formatPaisa(500_00L, "৳"))
        assertEquals("৳12,500", MoneyUtils.formatPaisa(12500_00L, "৳"))
        assertEquals("৳1,250.50", MoneyUtils.formatPaisa(1250_50L, "৳"))
    }

    @Test
    fun financialEngine_calculatesShopDuePersonBalancesLoansAndCashCorrectly() {
        val now = 1790867000000L
        val settings = AppSettingsEntity(
            openingCashBalancePaisa = 10000_00L // Opening ৳10,000
        )

        val rahmanShop = ShopEntity(id = "shop_1", name = "Rahman Store")
        val rahim = PersonEntity(id = "person_rahim", name = "Rahim")
        val karim = PersonEntity(id = "person_karim", name = "Karim")

        val loanKarim = LoanEntity(
            id = "loan_karim",
            isLentByMe = false, // I borrowed ৳10,000 from Karim
            personId = karim.id,
            personName = karim.name,
            principalPaisa = 10000_00L,
            startDateMillis = now - 10000L
        )
        val loanRahim = LoanEntity(
            id = "loan_rahim",
            isLentByMe = true, // I lent ৳15,000 to Rahim
            personId = rahim.id,
            personName = rahim.name,
            principalPaisa = 15000_00L,
            startDateMillis = now - 10000L
        )

        val txs = listOf(
            // Income +৳5,000
            TransactionEntity(
                id = "tx_1",
                type = TransactionType.INCOME.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 5000_00L,
                timestamp = now,
                categoryId = "cat_inc_salary",
                categoryName = "Salary"
            ),
            // Expense -৳200
            TransactionEntity(
                id = "tx_2",
                type = TransactionType.EXPENSE.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 200_00L,
                timestamp = now,
                categoryId = "cat_exp_food",
                categoryName = "Food",
                productOrDescription = "Lunch"
            ),
            // Rahman Store: Rice ৳500 + Oil ৳300 - Payment ৳300 = Remaining Due ৳500
            TransactionEntity(
                id = "tx_3",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 500_00L,
                timestamp = now,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Rice"
            ),
            TransactionEntity(
                id = "tx_4",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 300_00L,
                timestamp = now,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Oil"
            ),
            TransactionEntity(
                id = "tx_5",
                type = TransactionType.SHOP_PAYMENT.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 300_00L,
                timestamp = now,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Payment received"
            ),
            // Rahim: Lent ৳1,000, Received ৳500 -> Remaining Receivable ৳500
            TransactionEntity(
                id = "tx_6",
                type = TransactionType.LEND.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 1000_00L,
                timestamp = now,
                personId = rahim.id,
                personName = rahim.name
            ),
            TransactionEntity(
                id = "tx_7",
                type = TransactionType.RECEIVE_PAYMENT.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 500_00L,
                timestamp = now,
                personId = rahim.id,
                personName = rahim.name
            ),
            // Karim: Borrowed ৳5,000, Repaid ৳2,000 -> Remaining Payable ৳3,000
            TransactionEntity(
                id = "tx_8",
                type = TransactionType.BORROW.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 5000_00L,
                timestamp = now,
                personId = karim.id,
                personName = karim.name
            ),
            TransactionEntity(
                id = "tx_9",
                type = TransactionType.MAKE_PAYMENT.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 2000_00L,
                timestamp = now,
                personId = karim.id,
                personName = karim.name
            ),
            // Loan repayments: Karim loan repaid ৳3,000 -> remaining ৳7,000; Rahim loan repaid ৳5,000 -> remaining ৳10,000
            TransactionEntity(
                id = "tx_10",
                type = TransactionType.LOAN_PAYMENT.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 3000_00L,
                timestamp = now,
                personId = karim.id,
                personName = karim.name,
                loanId = loanKarim.id
            ),
            TransactionEntity(
                id = "tx_11",
                type = TransactionType.LOAN_REPAYMENT.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 5000_00L,
                timestamp = now,
                personId = rahim.id,
                personName = rahim.name,
                loanId = loanRahim.id
            )
        )

        val snapshot = FinancialEngine.calculateSnapshot(
            settings = settings,
            transactions = txs,
            shops = listOf(rahmanShop),
            people = listOf(rahim, karim),
            loans = listOf(loanKarim, loanRahim),
            categories = HisabDatabase.defaultCategories(),
            nowMillis = now
        )

        // Verify Rahman Store due = 500 + 300 - 300 = 500
        assertEquals(500_00L, snapshot.totalShopDuePaisa)
        assertEquals(500_00L, snapshot.shopSummaries.first().currentDuePaisa)

        // Verify Rahim direct receivable = 1000 - 500 = 500
        val rahimSummary = snapshot.personSummaries.first { it.person.id == rahim.id }
        assertEquals(500_00L, rahimSummary.directReceivablePaisa)
        assertEquals(10000_00L, rahimSummary.loanReceivablePaisa)

        // Verify Karim direct payable = 5000 - 2000 = 3000
        val karimSummary = snapshot.personSummaries.first { it.person.id == karim.id }
        assertEquals(3000_00L, karimSummary.directPayablePaisa)
        assertEquals(7000_00L, karimSummary.loanPayablePaisa)

        // Verify Loan statuses & remaining
        val karimLoanSummary = snapshot.loanSummaries.first { it.loan.id == loanKarim.id }
        assertEquals(7000_00L, karimLoanSummary.remainingPaisa)
        assertEquals(LoanStatus.PARTIALLY_PAID, karimLoanSummary.status)

        val rahimLoanSummary = snapshot.loanSummaries.first { it.loan.id == loanRahim.id }
        assertEquals(10000_00L, rahimLoanSummary.remainingPaisa)
        assertEquals(LoanStatus.PARTIALLY_PAID, rahimLoanSummary.status)

        // Total Receivable = Rahim(500) + Shop(500) + RahimLoan(10,000) = 11,000
        assertEquals(11000_00L, snapshot.totalReceivablePaisa)

        // Total Payable = Karim(3,000) + KarimLoan(7,000) = 10,000
        assertEquals(10000_00L, snapshot.totalPayablePaisa)
    }

    @Test
    fun financialEngine_blocksGivingOrSpendingMoreThanAvailableCashAndBlocksOverpayments() {
        val now = 1790867000000L
        // User has only 200 in cash
        val settings = AppSettingsEntity(
            currencySymbol = "$",
            currencyCode = "USD",
            openingCashBalancePaisa = 200_00L
        )
        val rahmanShop = ShopEntity(id = "shop_1", name = "Rahman Store")
        val rahim = PersonEntity(id = "person_rahim", name = "Rahim")
        val karim = PersonEntity(id = "person_karim", name = "Karim")

        val txs = listOf(
            // Shop due of $150
            TransactionEntity(
                id = "tx_due_150",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 150_00L,
                timestamp = now,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Rice"
            ),
            // Borrowed $100 from Karim (increases cash to $300, payable to Karim = $100)
            TransactionEntity(
                id = "tx_borrow_100",
                type = TransactionType.BORROW.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 100_00L,
                timestamp = now,
                personId = karim.id,
                personName = karim.name
            )
        )

        val snapshot = FinancialEngine.calculateSnapshot(
            settings = settings,
            transactions = txs,
            shops = listOf(rahmanShop),
            people = listOf(rahim, karim),
            loans = emptyList(),
            categories = HisabDatabase.defaultCategories(),
            nowMillis = now
        )

        // Available cash is $200 + $100 = $300
        assertEquals(300_00L, snapshot.cashBalancePaisa)

        // 1. Attempting to Give/Lend $1,000 when having only $300 MUST BE REJECTED
        val lend1000Error = FinancialEngine.validateTransactionProposal(
            snapshot = snapshot,
            existingTx = null,
            type = TransactionType.LEND,
            amountPaisa = 1000_00L,
            shopId = null,
            shopName = null,
            personId = rahim.id,
            personName = rahim.name,
            loanId = null,
            currencySymbol = "$"
        )
        org.junit.Assert.assertNotNull(lend1000Error)
        val maxLendAllowed = FinancialEngine.computeMaxAllowedAmountPaisa(
            snapshot = snapshot,
            existingTx = null,
            type = TransactionType.LEND,
            shopId = null,
            personId = rahim.id,
            loanId = null
        )
        assertEquals(300_00L, maxLendAllowed)

        // 2. Attempting to Spend $500 when having only $300 MUST BE REJECTED
        val expense500Error = FinancialEngine.validateTransactionProposal(
            snapshot = snapshot,
            existingTx = null,
            type = TransactionType.EXPENSE,
            amountPaisa = 500_00L,
            shopId = null,
            shopName = null,
            personId = null,
            personName = null,
            loanId = null,
            currencySymbol = "$"
        )
        org.junit.Assert.assertNotNull(expense500Error)

        // 3. Attempting to receive $250 from Rahman Store when due is only $150 MUST BE REJECTED
        val shopOverpayError = FinancialEngine.validateTransactionProposal(
            snapshot = snapshot,
            existingTx = null,
            type = TransactionType.SHOP_PAYMENT,
            amountPaisa = 250_00L,
            shopId = rahmanShop.id,
            shopName = rahmanShop.name,
            personId = null,
            personName = null,
            loanId = null,
            currencySymbol = "$"
        )
        org.junit.Assert.assertNotNull(shopOverpayError)
        assertEquals(
            150_00L,
            FinancialEngine.computeMaxAllowedAmountPaisa(
                snapshot = snapshot,
                existingTx = null,
                type = TransactionType.SHOP_PAYMENT,
                shopId = rahmanShop.id,
                personId = null,
                loanId = null
            )
        )

        // 4. Attempting to repay Karim $180 when we only owe $100 MUST BE REJECTED
        val repayOverpayError = FinancialEngine.validateTransactionProposal(
            snapshot = snapshot,
            existingTx = null,
            type = TransactionType.MAKE_PAYMENT,
            amountPaisa = 180_00L,
            shopId = null,
            shopName = null,
            personId = karim.id,
            personName = karim.name,
            loanId = null,
            currencySymbol = "$"
        )
        org.junit.Assert.assertNotNull(repayOverpayError)
        assertEquals(
            100_00L,
            FinancialEngine.computeMaxAllowedAmountPaisa(
                snapshot = snapshot,
                existingTx = null,
                type = TransactionType.MAKE_PAYMENT,
                shopId = null,
                personId = karim.id,
                loanId = null
            )
        )

        // 5. Creating a formal loan lent by me for $1,000 when cash is $300 MUST BE REJECTED
        val loanCreateError = FinancialEngine.validateLoanCreation(
            snapshot = snapshot,
            isLentByMe = true,
            principalPaisa = 1000_00L,
            personName = "Rahim",
            currencySymbol = "$"
        )
        org.junit.Assert.assertNotNull(loanCreateError)
    }
}
