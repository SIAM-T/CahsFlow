package com.example.domain

import androidx.compose.runtime.Immutable
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.LoanEntity
import com.example.data.local.LoanStatus
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.MoneyUtils

@Immutable
data class ShopBalanceSummary(
    val shop: ShopEntity,
    val totalDueGivenPaisa: Long,
    val totalPaymentReceivedPaisa: Long,
    val currentDuePaisa: Long,
    val transactionCount: Int,
    val transactions: List<TransactionEntity>
)

@Immutable
data class PersonBalanceSummary(
    val person: PersonEntity,
    val lentPaisa: Long,
    val receivedPaymentPaisa: Long,
    val directReceivablePaisa: Long,
    val borrowedPaisa: Long,
    val madePaymentPaisa: Long,
    val directPayablePaisa: Long,
    val loanReceivablePaisa: Long,
    val loanPayablePaisa: Long,
    val totalReceivablePaisa: Long, // directReceivable + loanReceivable
    val totalPayablePaisa: Long,    // directPayable + loanPayable
    val transactions: List<TransactionEntity>
)

@Immutable
data class LoanSummary(
    val loan: LoanEntity,
    val totalObligationPaisa: Long, // principal + interest
    val repaidPaisa: Long,
    val remainingPaisa: Long,
    val status: LoanStatus,
    val repayments: List<TransactionEntity>
)

@Immutable
data class CategoryBreakdown(
    val categoryId: String,
    val categoryName: String,
    val categoryNameBn: String,
    val colorHex: String,
    val iconName: String,
    val totalPaisa: Long,
    val percentageBasisPoints: Int, // 0..10000 (100.00% = 10000)
    val transactionCount: Int
)

@Immutable
data class PeriodIncomeExpenseReport(
    val dailyIncomePaisa: Long,
    val dailyExpensePaisa: Long,
    val weeklyIncomePaisa: Long,
    val weeklyExpensePaisa: Long,
    val monthlyIncomePaisa: Long,
    val monthlyExpensePaisa: Long,
    val yearlyIncomePaisa: Long,
    val yearlyExpensePaisa: Long,
    val totalIncomePaisa: Long,
    val totalExpensePaisa: Long,
    val expenseByCategory: List<CategoryBreakdown>,
    val incomeByCategory: List<CategoryBreakdown>
)

@Immutable
data class FinancialSnapshot(
    val openingCashBalancePaisa: Long = 0L,
    val cashBalancePaisa: Long = 0L,
    // Today stats
    val todayIncomePaisa: Long = 0L,
    val todayExpensePaisa: Long = 0L,
    val todayMoneyReceivedPaisa: Long = 0L,
    val todayMoneyGivenPaisa: Long = 0L,
    val todayShopDuePaisa: Long = 0L,
    // Current Position
    val totalReceivablePaisa: Long = 0L, // People Receivable + Shop Due + Active Loans Given
    val totalPayablePaisa: Long = 0L,    // People Payable + Active Loans Taken
    val totalShopDuePaisa: Long = 0L,
    val totalActiveLoansPaisa: Long = 0L,
    val totalLoansGivenRemainingPaisa: Long = 0L,
    val totalLoansTakenRemainingPaisa: Long = 0L,
    val totalPeopleReceivablePaisa: Long = 0L,
    val totalPeoplePayablePaisa: Long = 0L,
    // Detailed summaries
    val shopSummaries: List<ShopBalanceSummary> = emptyList(),
    val personSummaries: List<PersonBalanceSummary> = emptyList(),
    val loanSummaries: List<LoanSummary> = emptyList(),
    val reports: PeriodIncomeExpenseReport = PeriodIncomeExpenseReport(
        0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, emptyList(), emptyList()
    )
)

/**
 * Central deterministic financial engine.
 * Computes all balances, receivables, payables, shop dues, loans, and reports
 * strictly from transaction records using 64-bit integer paisa arithmetic.
 */
object FinancialEngine {

    fun calculateSnapshot(
        settings: AppSettingsEntity,
        transactions: List<TransactionEntity>,
        shops: List<ShopEntity>,
        people: List<PersonEntity>,
        loans: List<LoanEntity>,
        categories: List<CategoryEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): FinancialSnapshot {
        val startOfToday = MoneyUtils.startOfTodayMillis(nowMillis)
        val startOfWeek = MoneyUtils.startOfWeekMillis(nowMillis)
        val startOfMonth = MoneyUtils.startOfMonthMillis(nowMillis)
        val startOfYear = MoneyUtils.startOfYearMillis(nowMillis)

        var cashDeltaPaisa = 0L

        var todayIncome = 0L
        var todayExpense = 0L
        var todayReceived = 0L
        var todayGiven = 0L
        var todayShopDue = 0L

        var weeklyIncome = 0L
        var weeklyExpense = 0L
        var monthlyIncome = 0L
        var monthlyExpense = 0L
        var yearlyIncome = 0L
        var yearlyExpense = 0L
        var totalIncome = 0L
        var totalExpense = 0L

        val txByShop = HashMap<String, MutableList<TransactionEntity>>()
        val txByPerson = HashMap<String, MutableList<TransactionEntity>>()
        val txByLoan = HashMap<String, MutableList<TransactionEntity>>()
        val expenseByCatMap = HashMap<String, Pair<Long, Int>>()
        val incomeByCatMap = HashMap<String, Pair<Long, Int>>()

        for (tx in transactions) {
            val type = TransactionType.fromString(tx.type)
            val amt = tx.amountPaisa
            val isToday = tx.timestamp >= startOfToday
            val isWeek = tx.timestamp >= startOfWeek
            val isMonth = tx.timestamp >= startOfMonth
            val isYear = tx.timestamp >= startOfYear

            if (tx.shopId != null) {
                txByShop.getOrPut(tx.shopId) { ArrayList() }.add(tx)
            }
            if (tx.personId != null) {
                txByPerson.getOrPut(tx.personId) { ArrayList() }.add(tx)
            }
            if (tx.loanId != null) {
                txByLoan.getOrPut(tx.loanId) { ArrayList() }.add(tx)
            }

            when (type) {
                TransactionType.INCOME -> {
                    cashDeltaPaisa += amt
                    totalIncome += amt
                    if (isToday) todayIncome += amt
                    if (isWeek) weeklyIncome += amt
                    if (isMonth) monthlyIncome += amt
                    if (isYear) yearlyIncome += amt

                    val catKey = tx.categoryId ?: tx.categoryName ?: "cat_inc_other"
                    val prev = incomeByCatMap[catKey] ?: Pair(0L, 0)
                    incomeByCatMap[catKey] = Pair(prev.first + amt, prev.second + 1)
                }

                TransactionType.EXPENSE -> {
                    cashDeltaPaisa -= amt
                    totalExpense += amt
                    if (isToday) todayExpense += amt
                    if (isWeek) weeklyExpense += amt
                    if (isMonth) monthlyExpense += amt
                    if (isYear) yearlyExpense += amt

                    val catKey = tx.categoryId ?: tx.categoryName ?: "cat_exp_other"
                    val prev = expenseByCatMap[catKey] ?: Pair(0L, 0)
                    expenseByCatMap[catKey] = Pair(prev.first + amt, prev.second + 1)
                }

                TransactionType.LEND -> {
                    cashDeltaPaisa -= amt
                    if (isToday) todayGiven += amt
                }

                TransactionType.BORROW -> {
                    cashDeltaPaisa += amt
                    if (isToday) todayReceived += amt
                }

                TransactionType.RECEIVE_PAYMENT -> {
                    cashDeltaPaisa += amt
                    if (isToday) todayReceived += amt
                }

                TransactionType.MAKE_PAYMENT -> {
                    cashDeltaPaisa -= amt
                    if (isToday) todayGiven += amt
                }

                TransactionType.SHOP_DUE -> {
                    // Product/credit given to shop increases shop due (receivable), not immediate cash
                    if (isToday) todayShopDue += amt
                }

                TransactionType.SHOP_PAYMENT -> {
                    cashDeltaPaisa += amt
                    if (isToday) todayReceived += amt
                }

                TransactionType.LOAN_GIVEN -> {
                    cashDeltaPaisa -= amt
                    if (isToday) todayGiven += amt
                }

                TransactionType.LOAN_REPAYMENT -> {
                    cashDeltaPaisa += amt
                    if (isToday) todayReceived += amt
                }

                TransactionType.LOAN_RECEIVED -> {
                    cashDeltaPaisa += amt
                    if (isToday) todayReceived += amt
                }

                TransactionType.LOAN_PAYMENT -> {
                    cashDeltaPaisa -= amt
                    if (isToday) todayGiven += amt
                }

                TransactionType.ADJUSTMENT -> {
                    if (tx.direction == TransactionDirection.OUTFLOW.name) {
                        cashDeltaPaisa -= amt
                    } else {
                        cashDeltaPaisa += amt
                    }
                }
            }
        }

        // 1. Calculate Shop Summaries
        var totalShopDuePaisa = 0L
        val shopSummaries = shops.map { shop ->
            val shopTxs = txByShop[shop.id] ?: emptyList()
            var dueGiven = 0L
            var paymentReceived = 0L
            for (tx in shopTxs) {
                when (TransactionType.fromString(tx.type)) {
                    TransactionType.SHOP_DUE -> dueGiven += tx.amountPaisa
                    TransactionType.SHOP_PAYMENT -> paymentReceived += tx.amountPaisa
                    else -> {}
                }
            }
            val currentDue = (dueGiven - paymentReceived).coerceAtLeast(0L)
            totalShopDuePaisa += currentDue
            ShopBalanceSummary(
                shop = shop,
                totalDueGivenPaisa = dueGiven,
                totalPaymentReceivedPaisa = paymentReceived,
                currentDuePaisa = currentDue,
                transactionCount = shopTxs.size,
                transactions = shopTxs
            )
        }

        // 2. Calculate Loan Summaries
        var totalLoansGivenRemaining = 0L
        var totalLoansTakenRemaining = 0L
        val loanReceivableByPerson = HashMap<String, Long>()
        val loanPayableByPerson = HashMap<String, Long>()

        val loanSummaries = loans.map { loan ->
            val loanTxs = txByLoan[loan.id] ?: emptyList()
            val totalObligation = loan.principalPaisa + loan.interestPaisa
            var repaid = 0L
            for (tx in loanTxs) {
                val t = TransactionType.fromString(tx.type)
                if (t == TransactionType.LOAN_REPAYMENT || t == TransactionType.LOAN_PAYMENT ||
                    t == TransactionType.RECEIVE_PAYMENT || t == TransactionType.MAKE_PAYMENT
                ) {
                    repaid += tx.amountPaisa
                }
            }
            val remaining = (totalObligation - repaid).coerceAtLeast(0L)
            val status = when {
                remaining <= 0L -> LoanStatus.PAID
                loan.dueDateMillis != null && loan.dueDateMillis < nowMillis -> LoanStatus.OVERDUE
                repaid > 0L -> LoanStatus.PARTIALLY_PAID
                else -> LoanStatus.ACTIVE
            }

            if (loan.isLentByMe) {
                totalLoansGivenRemaining += remaining
                if (loan.personId != null) {
                    loanReceivableByPerson[loan.personId] =
                        (loanReceivableByPerson[loan.personId] ?: 0L) + remaining
                }
            } else {
                totalLoansTakenRemaining += remaining
                if (loan.personId != null) {
                    loanPayableByPerson[loan.personId] =
                        (loanPayableByPerson[loan.personId] ?: 0L) + remaining
                }
            }

            LoanSummary(
                loan = loan,
                totalObligationPaisa = totalObligation,
                repaidPaisa = repaid,
                remainingPaisa = remaining,
                status = status,
                repayments = loanTxs.filter {
                    val t = TransactionType.fromString(it.type)
                    t == TransactionType.LOAN_REPAYMENT || t == TransactionType.LOAN_PAYMENT
                }
            )
        }

        // 3. Calculate Person Summaries (supporting both Receivable and Payable per person)
        var totalPeopleReceivable = 0L
        var totalPeoplePayable = 0L

        val personSummaries = people.map { person ->
            val personTxs = txByPerson[person.id] ?: emptyList()
            var lent = 0L
            var received = 0L
            var borrowed = 0L
            var repaid = 0L

            for (tx in personTxs) {
                // Skip loan-linked transactions in direct lending/borrowing so we don't double-count
                if (tx.loanId != null) continue
                when (TransactionType.fromString(tx.type)) {
                    TransactionType.LEND -> lent += tx.amountPaisa
                    TransactionType.RECEIVE_PAYMENT -> received += tx.amountPaisa
                    TransactionType.BORROW -> borrowed += tx.amountPaisa
                    TransactionType.MAKE_PAYMENT -> repaid += tx.amountPaisa
                    else -> {}
                }
            }

            val directReceivable = (lent - received).coerceAtLeast(0L)
            val directPayable = (borrowed - repaid).coerceAtLeast(0L)
            val loanRec = loanReceivableByPerson[person.id] ?: 0L
            val loanPay = loanPayableByPerson[person.id] ?: 0L

            totalPeopleReceivable += directReceivable
            totalPeoplePayable += directPayable

            PersonBalanceSummary(
                person = person,
                lentPaisa = lent,
                receivedPaymentPaisa = received,
                directReceivablePaisa = directReceivable,
                borrowedPaisa = borrowed,
                madePaymentPaisa = repaid,
                directPayablePaisa = directPayable,
                loanReceivablePaisa = loanRec,
                loanPayablePaisa = loanPay,
                totalReceivablePaisa = directReceivable + loanRec,
                totalPayablePaisa = directPayable + loanPay,
                transactions = personTxs
            )
        }

        // 4. Category breakdowns for Reports
        val catMap = categories.associateBy { it.id }
        val catByName = categories.associateBy { it.name.lowercase() }

        val expenseBreakdowns = expenseByCatMap.entries.map { (key, pair) ->
            val cat = catMap[key] ?: catByName[key.lowercase()]
            val pctBasisPoints = if (totalExpense > 0L) {
                ((pair.first * 10000L) / totalExpense).toInt()
            } else 0
            CategoryBreakdown(
                categoryId = cat?.id ?: key,
                categoryName = cat?.name ?: key,
                categoryNameBn = cat?.nameBn?.ifEmpty { cat.name } ?: key,
                colorHex = cat?.colorHex ?: "#F97316",
                iconName = cat?.iconName ?: "Category",
                totalPaisa = pair.first,
                percentageBasisPoints = pctBasisPoints,
                transactionCount = pair.second
            )
        }.sortedByDescending { it.totalPaisa }

        val incomeBreakdowns = incomeByCatMap.entries.map { (key, pair) ->
            val cat = catMap[key] ?: catByName[key.lowercase()]
            val pctBasisPoints = if (totalIncome > 0L) {
                ((pair.first * 10000L) / totalIncome).toInt()
            } else 0
            CategoryBreakdown(
                categoryId = cat?.id ?: key,
                categoryName = cat?.name ?: key,
                categoryNameBn = cat?.nameBn?.ifEmpty { cat.name } ?: key,
                colorHex = cat?.colorHex ?: "#10B981",
                iconName = cat?.iconName ?: "Savings",
                totalPaisa = pair.first,
                percentageBasisPoints = pctBasisPoints,
                transactionCount = pair.second
            )
        }.sortedByDescending { it.totalPaisa }

        val totalReceivable = totalPeopleReceivable + totalShopDuePaisa + totalLoansGivenRemaining
        val totalPayable = totalPeoplePayable + totalLoansTakenRemaining
        val totalActiveLoans = totalLoansGivenRemaining + totalLoansTakenRemaining

        return FinancialSnapshot(
            openingCashBalancePaisa = settings.openingCashBalancePaisa,
            cashBalancePaisa = settings.openingCashBalancePaisa + cashDeltaPaisa,
            todayIncomePaisa = todayIncome,
            todayExpensePaisa = todayExpense,
            todayMoneyReceivedPaisa = todayReceived,
            todayMoneyGivenPaisa = todayGiven,
            todayShopDuePaisa = todayShopDue,
            totalReceivablePaisa = totalReceivable,
            totalPayablePaisa = totalPayable,
            totalShopDuePaisa = totalShopDuePaisa,
            totalActiveLoansPaisa = totalActiveLoans,
            totalLoansGivenRemainingPaisa = totalLoansGivenRemaining,
            totalLoansTakenRemainingPaisa = totalLoansTakenRemaining,
            totalPeopleReceivablePaisa = totalPeopleReceivable,
            totalPeoplePayablePaisa = totalPeoplePayable,
            shopSummaries = shopSummaries,
            personSummaries = personSummaries,
            loanSummaries = loanSummaries,
            reports = PeriodIncomeExpenseReport(
                dailyIncomePaisa = todayIncome,
                dailyExpensePaisa = todayExpense,
                weeklyIncomePaisa = weeklyIncome,
                weeklyExpensePaisa = weeklyExpense,
                monthlyIncomePaisa = monthlyIncome,
                monthlyExpensePaisa = monthlyExpense,
                yearlyIncomePaisa = yearlyIncome,
                yearlyExpensePaisa = yearlyExpense,
                totalIncomePaisa = totalIncome,
                totalExpensePaisa = totalExpense,
                expenseByCategory = expenseBreakdowns,
                incomeByCategory = incomeBreakdowns
            )
        )
    }
}
