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
    val transactions: List<TransactionEntity>,
    val totalDueTakenPaisa: Long = 0L,
    val totalPaidToShopPaisa: Long = 0L,
    val netDuePaisa: Long = currentDuePaisa
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
                    // Goods bought on credit from shop increases user's due to shop (payable)
                    if (isToday) todayShopDue += amt
                }

                TransactionType.SHOP_PAYMENT -> {
                    // User pays shop for previous dues: cash decreases
                    cashDeltaPaisa -= amt
                    if (isToday) todayGiven += amt
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
            var dueTaken = 0L
            var paidToShop = 0L
            for (tx in shopTxs) {
                when (TransactionType.fromString(tx.type)) {
                    TransactionType.SHOP_DUE -> dueGiven += tx.amountPaisa
                    TransactionType.SHOP_PAYMENT -> paymentReceived += tx.amountPaisa
                    TransactionType.EXPENSE -> dueTaken += tx.amountPaisa
                    TransactionType.MAKE_PAYMENT -> paidToShop += tx.amountPaisa
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
                transactions = shopTxs,
                totalDueTakenPaisa = dueTaken,
                totalPaidToShopPaisa = paidToShop,
                netDuePaisa = currentDue
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

        val totalReceivable = totalPeopleReceivable + totalLoansGivenRemaining
        val totalPayable = totalPeoplePayable + totalShopDuePaisa + totalLoansTakenRemaining
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

    /**
     * Returns the signed cash delta caused by a single transaction (+ for cash inflow, - for cash outflow, 0 for credit).
     */
    fun cashEffectOf(tx: TransactionEntity): Long {
        val type = TransactionType.fromString(tx.type)
        val amt = tx.amountPaisa
        return when (type) {
            TransactionType.INCOME,
            TransactionType.BORROW,
            TransactionType.RECEIVE_PAYMENT,
            TransactionType.LOAN_REPAYMENT,
            TransactionType.LOAN_RECEIVED -> amt

            TransactionType.EXPENSE,
            TransactionType.LEND,
            TransactionType.MAKE_PAYMENT,
            TransactionType.SHOP_PAYMENT,
            TransactionType.LOAN_GIVEN,
            TransactionType.LOAN_PAYMENT -> -amt

            TransactionType.SHOP_DUE -> 0L

            TransactionType.ADJUSTMENT -> {
                if (tx.direction == TransactionDirection.OUTFLOW.name) -amt else amt
            }
        }
    }

    fun isCashOutflowType(type: TransactionType, adjustmentIsOutflow: Boolean = false): Boolean =
        when (type) {
            TransactionType.EXPENSE,
            TransactionType.LEND,
            TransactionType.MAKE_PAYMENT,
            TransactionType.SHOP_PAYMENT,
            TransactionType.LOAN_GIVEN,
            TransactionType.LOAN_PAYMENT -> true
            TransactionType.ADJUSTMENT -> adjustmentIsOutflow
            else -> false
        }

    /**
     * Computes the exact maximum allowed amount (in paisa) for a given transaction context,
     * or null if there is no upper cap (e.g. INCOME, BORROW, SHOP_DUE).
     */
    fun computeMaxAllowedAmountPaisa(
        snapshot: FinancialSnapshot,
        existingTx: TransactionEntity?,
        type: TransactionType,
        shopId: String?,
        personId: String?,
        loanId: String?,
        adjustmentIsOutflow: Boolean = false
    ): Long? {
        val effectiveCash = snapshot.cashBalancePaisa - (existingTx?.let { cashEffectOf(it) } ?: 0L)

        return when (type) {
            TransactionType.EXPENSE,
            TransactionType.LEND,
            TransactionType.LOAN_GIVEN -> effectiveCash.coerceAtLeast(0L)

            TransactionType.ADJUSTMENT -> if (adjustmentIsOutflow) effectiveCash.coerceAtLeast(0L) else null

            TransactionType.SHOP_PAYMENT -> {
                val shopSum = snapshot.shopSummaries.find { it.shop.id == shopId } ?: return 0L
                val oldRevert = if (existingTx != null &&
                    existingTx.shopId == shopId &&
                    TransactionType.fromString(existingTx.type) == TransactionType.SHOP_PAYMENT
                ) existingTx.amountPaisa else 0L
                val effectiveDue = (shopSum.currentDuePaisa + oldRevert).coerceAtLeast(0L)
                minOf(effectiveDue, effectiveCash.coerceAtLeast(0L))
            }

            TransactionType.RECEIVE_PAYMENT -> {
                val personSum = snapshot.personSummaries.find { it.person.id == personId } ?: return 0L
                val oldRevert = if (existingTx != null &&
                    existingTx.personId == personId &&
                    TransactionType.fromString(existingTx.type) == TransactionType.RECEIVE_PAYMENT
                ) existingTx.amountPaisa else 0L
                (personSum.directReceivablePaisa + oldRevert).coerceAtLeast(0L)
            }

            TransactionType.MAKE_PAYMENT -> {
                val personSum = snapshot.personSummaries.find { it.person.id == personId } ?: return 0L
                val oldRevert = if (existingTx != null &&
                    existingTx.personId == personId &&
                    TransactionType.fromString(existingTx.type) == TransactionType.MAKE_PAYMENT
                ) existingTx.amountPaisa else 0L
                val effectivePayable = (personSum.directPayablePaisa + oldRevert).coerceAtLeast(0L)
                minOf(effectivePayable, effectiveCash.coerceAtLeast(0L))
            }

            TransactionType.LOAN_REPAYMENT -> {
                val loanSum = snapshot.loanSummaries.find { it.loan.id == loanId } ?: return 0L
                val oldRevert = if (existingTx != null &&
                    existingTx.loanId == loanId &&
                    TransactionType.fromString(existingTx.type) == TransactionType.LOAN_REPAYMENT
                ) existingTx.amountPaisa else 0L
                (loanSum.remainingPaisa + oldRevert).coerceAtLeast(0L)
            }

            TransactionType.LOAN_PAYMENT -> {
                val loanSum = snapshot.loanSummaries.find { it.loan.id == loanId } ?: return 0L
                val oldRevert = if (existingTx != null &&
                    existingTx.loanId == loanId &&
                    TransactionType.fromString(existingTx.type) == TransactionType.LOAN_PAYMENT
                ) existingTx.amountPaisa else 0L
                val effectiveLoanRem = (loanSum.remainingPaisa + oldRevert).coerceAtLeast(0L)
                minOf(effectiveLoanRem, effectiveCash.coerceAtLeast(0L))
            }

            TransactionType.INCOME,
            TransactionType.BORROW,
            TransactionType.SHOP_DUE,
            TransactionType.LOAN_RECEIVED -> null
        }
    }

    /**
     * Strict, comprehensive business-logic validator for creating or editing any transaction.
     * Prevents:
     * 1. Spending/giving/repaying more cash than available in Cash Balance
     * 2. Receiving more Shop Payment than the shop's Current Due
     * 3. Receiving more Payment from a Person than their Receivable balance
     * 4. Paying more to a Person than their Payable balance (or available Cash)
     * 5. Paying/receiving more on a Loan than the loan's Remaining obligation
     * 6. Editing an inflow transaction down to an amount that would make current Cash Balance negative
     */
    fun validateTransactionProposal(
        snapshot: FinancialSnapshot,
        existingTx: TransactionEntity?,
        type: TransactionType,
        amountPaisa: Long,
        shopId: String?,
        shopName: String?,
        personId: String?,
        personName: String?,
        loanId: String?,
        currencySymbol: String = "৳",
        adjustmentIsOutflow: Boolean = false
    ): String? {
        if (amountPaisa <= 0L) {
            return "Please enter a valid amount greater than 0."
        }

        val effectiveCash = snapshot.cashBalancePaisa - (existingTx?.let { cashEffectOf(it) } ?: 0L)
        val fmtAmt = MoneyUtils.formatPaisa(amountPaisa, currencySymbol)
        val fmtCash = MoneyUtils.formatPaisa(effectiveCash.coerceAtLeast(0L), currencySymbol)

        // 1. Check Shop Payment constraints (Paying off shop due)
        if (type == TransactionType.SHOP_PAYMENT) {
            val shopSum = snapshot.shopSummaries.find { it.shop.id == shopId }
                ?: return if (currencySymbol == "৳") "দোকান নির্বাচন করুন।" else "Please select an existing shop."
            val oldRevert = if (existingTx != null &&
                existingTx.shopId == shopId &&
                TransactionType.fromString(existingTx.type) == TransactionType.SHOP_PAYMENT
            ) existingTx.amountPaisa else 0L
            val effectiveDue = (shopSum.currentDuePaisa + oldRevert).coerceAtLeast(0L)
            if (effectiveDue <= 0L) {
                return if (currencySymbol == "৳") "${shopSum.shop.name}-এ বর্তমানে আপনার কোনো বকেয়া বাকি নেই।" else "${shopSum.shop.name} currently has no due."
            }
            if (amountPaisa > effectiveDue) {
                return if (currencySymbol == "৳") "বকেয়ার চেয়ে বেশি পরিশোধ সম্ভব নয়! ${shopSum.shop.name}-এ বাকি: ${MoneyUtils.formatPaisa(effectiveDue, currencySymbol)}, কিন্তু আপনি দিচ্ছেন $fmtAmt।" else "Overpayment blocked! ${shopSum.shop.name} due is only ${MoneyUtils.formatPaisa(effectiveDue, currencySymbol)}, so you cannot pay $fmtAmt."
            }
            if (amountPaisa > effectiveCash) {
                return if (currencySymbol == "৳") "অপর্যাপ্ত নগদ ব্যালেন্স! আপনার কাছে $fmtCash আছে, তাই $fmtAmt পরিশোধ করা সম্ভব নয়।" else "Insufficient Cash Balance! You only have $fmtCash available, so you cannot pay $fmtAmt."
            }
        }

        // 2. Check Person Receive Payment constraints
        if (type == TransactionType.RECEIVE_PAYMENT) {
            val personSum = snapshot.personSummaries.find { it.person.id == personId }
                ?: return if (currencySymbol == "৳") "${personName ?: "এই ব্যক্তি"}-এর কাছে আপনার কোনো পাওনা বাকি নেই।" else "${personName ?: "This person"} does not owe you any money yet. Use 'Take Money (Borrow)' if you are borrowing from them."
            val oldRevert = if (existingTx != null &&
                existingTx.personId == personId &&
                TransactionType.fromString(existingTx.type) == TransactionType.RECEIVE_PAYMENT
            ) existingTx.amountPaisa else 0L
            val effectiveRec = (personSum.directReceivablePaisa + oldRevert).coerceAtLeast(0L)
            if (effectiveRec <= 0L) {
                return if (currencySymbol == "৳") "${personSum.person.name}-এর কাছে বর্তমানে কোনো পাওনা নেই।" else "${personSum.person.name} owes you ${MoneyUtils.formatPaisa(0L, currencySymbol)} right now. Use 'Take Money (Borrow)' if you are borrowing from them."
            }
            if (amountPaisa > effectiveRec) {
                return if (currencySymbol == "৳") "পাওনার চেয়ে বেশি আদায় সম্ভব নয়! ${personSum.person.name}-এর কাছে পাওনা মাত্র ${MoneyUtils.formatPaisa(effectiveRec, currencySymbol)}, কিন্তু আপনি নিচ্ছেন $fmtAmt।" else "Exceeds Receivable! ${personSum.person.name} only owes you ${MoneyUtils.formatPaisa(effectiveRec, currencySymbol)}, so you cannot receive $fmtAmt."
            }
        }

        // 3. Check Person Make Payment (Repay Debt) constraints
        if (type == TransactionType.MAKE_PAYMENT) {
            val personSum = snapshot.personSummaries.find { it.person.id == personId }
                ?: return if (currencySymbol == "৳") "${personName ?: "এই ব্যক্তি"}-এর কাছে আপনার কোনো দেনা নেই।" else "You do not owe ${personName ?: "this person"} any money yet. Use 'Give Money (Lend)' if you are lending to them."
            val oldRevert = if (existingTx != null &&
                existingTx.personId == personId &&
                TransactionType.fromString(existingTx.type) == TransactionType.MAKE_PAYMENT
            ) existingTx.amountPaisa else 0L
            val effectivePayable = (personSum.directPayablePaisa + oldRevert).coerceAtLeast(0L)
            if (effectivePayable <= 0L) {
                return if (currencySymbol == "৳") "${personSum.person.name}-এর কাছে আপনার কোনো দেনা নেই।" else "You do not owe ${personSum.person.name} any money (${MoneyUtils.formatPaisa(0L, currencySymbol)} payable). Use 'Give Money (Lend)' if you are lending to them."
            }
            if (amountPaisa > effectivePayable) {
                return if (currencySymbol == "৳") "দেনার চেয়ে বেশি পরিশোধ সম্ভব নয়! ${personSum.person.name}-কে দেওয়ার কথা ${MoneyUtils.formatPaisa(effectivePayable, currencySymbol)}, কিন্তু আপনি দিচ্ছেন $fmtAmt।" else "Overpayment blocked! You only owe ${personSum.person.name} ${MoneyUtils.formatPaisa(effectivePayable, currencySymbol)}, so you cannot repay $fmtAmt."
            }
            if (amountPaisa > effectiveCash) {
                return if (currencySymbol == "৳") "অপর্যাপ্ত নগদ ব্যালেন্স! আপনার কাছে $fmtCash আছে, তাই $fmtAmt পরিশোধ করা সম্ভব নয়।" else "Insufficient Cash Balance! You have $fmtCash available, so you cannot repay $fmtAmt."
            }
        }

        // 4. Check Loan Repayment / Installment constraints
        if (type == TransactionType.LOAN_REPAYMENT || type == TransactionType.LOAN_PAYMENT) {
            val loanSum = snapshot.loanSummaries.find { it.loan.id == loanId }
                ?: return if (currencySymbol == "৳") "পরিশোধের জন্য একটি চলমান ঋণ নির্বাচন করুন।" else "Please select an active loan to record a repayment."
            val oldRevert = if (existingTx != null &&
                existingTx.loanId == loanId &&
                (TransactionType.fromString(existingTx.type) == TransactionType.LOAN_REPAYMENT ||
                    TransactionType.fromString(existingTx.type) == TransactionType.LOAN_PAYMENT)
            ) existingTx.amountPaisa else 0L
            val effectiveLoanRem = (loanSum.remainingPaisa + oldRevert).coerceAtLeast(0L)
            if (effectiveLoanRem <= 0L) {
                return if (currencySymbol == "৳") "${loanSum.loan.personName}-এর সাথে এই ঋণটি ইতোমধ্যে সম্পূর্ণরূপে পরিশোধিত!" else "This loan with ${loanSum.loan.personName} is already fully settled!"
            }
            if (amountPaisa > effectiveLoanRem) {
                return if (currencySymbol == "৳") "বকেয়া ঋণের চেয়ে বেশি পরিশোধ সম্ভব নয়! মাত্র ${MoneyUtils.formatPaisa(effectiveLoanRem, currencySymbol)} বাকি আছে, কিন্তু আপনি দিচ্ছেন $fmtAmt।" else "Exceeds Remaining Loan! Only ${MoneyUtils.formatPaisa(effectiveLoanRem, currencySymbol)} remains on this loan, so you cannot record $fmtAmt."
            }
            if (type == TransactionType.LOAN_PAYMENT && amountPaisa > effectiveCash) {
                return if (currencySymbol == "৳") "অপর্যাপ্ত নগদ ব্যালেন্স! কিস্তি পরিশোধের জন্য আপনার কাছে $fmtCash আছে, যা $fmtAmt এর চেয়ে কম।" else "Insufficient Cash Balance! You only have $fmtCash available to pay this loan installment of $fmtAmt."
            }
        }

        // 5. General Cash Outflow Guard (EXPENSE, LEND, LOAN_GIVEN, ADJUSTMENT OUTFLOW)
        if (isCashOutflowType(type, adjustmentIsOutflow)) {
            if (amountPaisa > effectiveCash) {
                val actionDesc = when (type) {
                    TransactionType.EXPENSE -> if (currencySymbol == "৳") "খরচ" else "spend"
                    TransactionType.LEND -> if (currencySymbol == "৳") "ধার প্রদান" else "give"
                    TransactionType.LOAN_GIVEN -> if (currencySymbol == "৳") "ঋণ প্রদান" else "lend"
                    TransactionType.MAKE_PAYMENT, TransactionType.LOAN_PAYMENT -> if (currencySymbol == "৳") "পরিশোধ" else "pay"
                    else -> if (currencySymbol == "৳") "কর্তন" else "deduct"
                }
                return if (currencySymbol == "৳") {
                    "অপর্যাপ্ত নগদ ব্যালেন্স! আপনার কাছে মাত্র $fmtCash আছে, তাই $fmtAmt $actionDesc করা সম্ভব নয়।"
                } else {
                    "Insufficient Cash Balance! You only have $fmtCash available, so you cannot $actionDesc $fmtAmt."
                }
            }
        }

        // 6. Check if editing an inflow transaction down would cause current Cash Balance to go negative
        val proposedNewTx = TransactionEntity(
            id = existingTx?.id ?: "temp",
            type = type.name,
            direction = if (type == TransactionType.ADJUSTMENT && adjustmentIsOutflow) {
                TransactionDirection.OUTFLOW.name
            } else {
                type.defaultDirection.name
            },
            amountPaisa = amountPaisa,
            timestamp = System.currentTimeMillis()
        )
        val projectedCash = effectiveCash + cashEffectOf(proposedNewTx)
        if (projectedCash < 0L) {
            return "Cannot save this change because your Cash Balance would become negative (${MoneyUtils.formatPaisa(projectedCash, currencySymbol)})."
        }

        return null
    }

    /**
     * Validates creating a new formal Loan.
     * If `isLentByMe == true`, ensures `principalPaisa <= snapshot.cashBalancePaisa`.
     */
    fun validateLoanCreation(
        snapshot: FinancialSnapshot,
        isLentByMe: Boolean,
        principalPaisa: Long,
        personName: String,
        currencySymbol: String = "৳"
    ): String? {
        if (personName.isBlank()) {
            return "Please enter or select a person's name for this loan."
        }
        if (principalPaisa <= 0L) {
            return "Please enter a loan amount greater than 0."
        }
        if (isLentByMe && principalPaisa > snapshot.cashBalancePaisa) {
            val fmtPrin = MoneyUtils.formatPaisa(principalPaisa, currencySymbol)
            val fmtCash = MoneyUtils.formatPaisa(snapshot.cashBalancePaisa.coerceAtLeast(0L), currencySymbol)
            return "Insufficient Cash Balance! You only have $fmtCash available, so you cannot lend $fmtPrin."
        }
        return null
    }

    /**
     * Validates deleting a transaction so deletion never leaves negative Cash, negative Shop Due,
     * or orphaned repayments exceeding principal.
     */
    fun validateTransactionDeletion(
        snapshot: FinancialSnapshot,
        txToDelete: TransactionEntity,
        currencySymbol: String = "৳"
    ): String? {
        val projectedCash = snapshot.cashBalancePaisa - cashEffectOf(txToDelete)
        if (projectedCash < 0L) {
            return "Cannot delete this inflow of ${MoneyUtils.formatPaisa(txToDelete.amountPaisa, currencySymbol)} because that cash has already been spent or lent out (Cash Balance would drop to ${MoneyUtils.formatPaisa(projectedCash, currencySymbol)})."
        }

        val type = TransactionType.fromString(txToDelete.type)
        if (type == TransactionType.SHOP_DUE && txToDelete.shopId != null) {
            val shopSum = snapshot.shopSummaries.find { it.shop.id == txToDelete.shopId }
            if (shopSum != null && (shopSum.totalDueGivenPaisa - txToDelete.amountPaisa) < shopSum.totalPaymentReceivedPaisa) {
                return "Cannot delete this Shop Due because payments have already been received against it from ${shopSum.shop.name}."
            }
        }

        if (type == TransactionType.LEND && txToDelete.personId != null) {
            val personSum = snapshot.personSummaries.find { it.person.id == txToDelete.personId }
            if (personSum != null && (personSum.lentPaisa - txToDelete.amountPaisa) < personSum.receivedPaymentPaisa) {
                return "Cannot delete this Lending entry because repayments have already been received against it from ${personSum.person.name}."
            }
        }

        if (type == TransactionType.BORROW && txToDelete.personId != null) {
            val personSum = snapshot.personSummaries.find { it.person.id == txToDelete.personId }
            if (personSum != null && (personSum.borrowedPaisa - txToDelete.amountPaisa) < personSum.madePaymentPaisa) {
                return "Cannot delete this Borrowing entry because repayments have already been paid against it to ${personSum.person.name}."
            }
        }

        return null
    }
}
