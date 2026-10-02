package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType(
    val labelEn: String,
    val labelBn: String,
    val defaultDirection: TransactionDirection
) {
    INCOME("Income", "আয়", TransactionDirection.INFLOW),
    EXPENSE("Expense", "ব্যয়", TransactionDirection.OUTFLOW),
    LEND("Money Given (Lent)", "টাকা দেওয়া (ধার)", TransactionDirection.OUTFLOW),
    BORROW("Money Taken (Borrowed)", "টাকা নেওয়া (ধার)", TransactionDirection.INFLOW),
    RECEIVE_PAYMENT("Payment Received", "পাওনা আদায়", TransactionDirection.INFLOW),
    MAKE_PAYMENT("Payment Made", "দেনা পরিশোধ", TransactionDirection.OUTFLOW),
    SHOP_DUE("Shop Due", "দোকানের বাকি", TransactionDirection.CREDIT_EXTENDED),
    SHOP_PAYMENT("Shop Payment Received", "দোকানের টাকা আদায়", TransactionDirection.INFLOW),
    LOAN_GIVEN("Loan Given", "ঋণ প্রদান", TransactionDirection.OUTFLOW),
    LOAN_REPAYMENT("Loan Repayment Received", "ঋণ আদায়", TransactionDirection.INFLOW),
    LOAN_RECEIVED("Loan Taken", "ঋণ গ্রহণ", TransactionDirection.INFLOW),
    LOAN_PAYMENT("Loan Installment Paid", "ঋণের কিস্তি পরিশোধ", TransactionDirection.OUTFLOW),
    ADJUSTMENT("Balance Adjustment", "ব্যালেন্স সমন্বয়", TransactionDirection.NEUTRAL);

    companion object {
        fun fromString(value: String): TransactionType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
    }
}

enum class TransactionDirection {
    INFLOW,
    OUTFLOW,
    CREDIT_EXTENDED,
    DEBT_INCURRED,
    NEUTRAL
}

enum class LoanStatus(val labelEn: String, val labelBn: String) {
    ACTIVE("Active", "চলমান"),
    PARTIALLY_PAID("Partially Paid", "আংশিক পরিশোধিত"),
    PAID("Paid", "পরিশোধিত"),
    OVERDUE("Overdue", "মেয়াদোত্তীর্ণ")
}

enum class DateFilterPreset(val labelEn: String, val labelBn: String) {
    ALL("All Time", "সব সময়"),
    TODAY("Today", "আজ"),
    YESTERDAY("Yesterday", "গতকাল"),
    THIS_WEEK("This Week", "এই সপ্তাহ"),
    THIS_MONTH("This Month", "এই মাস"),
    LAST_MONTH("Last Month", "গত মাস"),
    CUSTOM("Custom Range", "কাস্টম তারিখ")
}

enum class TransactionFilterGroup(val labelEn: String, val labelBn: String) {
    ALL("All", "সব"),
    INCOME("Income", "আয়"),
    EXPENSE("Expense", "ব্যয়"),
    LENDING("Lending", "পাওনা/দেওয়া"),
    BORROWING("Borrowing", "দেনা/নেওয়া"),
    SHOP_DUE("Shop Due", "দোকান বাকি"),
    PAYMENTS("Payments", "আদায়/পরিশোধ"),
    LOANS("Loans", "ঋণ")
}

enum class SortOption(val labelEn: String, val labelBn: String) {
    NEWEST("Newest First", "নতুন আগে"),
    OLDEST("Oldest First", "পুরাতন আগে"),
    HIGHEST_AMOUNT("Highest Amount", "সর্বোচ্চ টাকা"),
    LOWEST_AMOUNT("Lowest Amount", "সর্বনিম্ন টাকা")
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["shopId"]),
        Index(value = ["personId"]),
        Index(value = ["loanId"]),
        Index(value = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // TransactionType.name
    val direction: String, // TransactionDirection.name
    val amountPaisa: Long,
    val timestamp: Long,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val personId: String? = null,
    val personName: String? = null,
    val shopId: String? = null,
    val shopName: String? = null,
    val loanId: String? = null,
    val productOrDescription: String = "",
    val note: String = "",
    val dueDateMillis: Long? = null,
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type", "sortOrder"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameBn: String = "",
    val type: String, // "INCOME" or "EXPENSE"
    val iconName: String = "Category",
    val colorHex: String = "#10B981",
    val sortOrder: Int = 0,
    val usageCount: Int = 0
)

@Entity(
    tableName = "people",
    indices = [Index(value = ["name"]), Index(value = ["lastInteractionAt"])]
)
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val avatarColorHex: String = "#06B6D4",
    val iconName: String = "Person",
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shops",
    indices = [Index(value = ["name"]), Index(value = ["lastInteractionAt"])]
)
data class ShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val colorHex: String = "#F59E0B",
    val recentProductsCsv: String = "Rice,Oil,Sugar,Lentils,Flour,Biscuits",
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun recentProductList(): List<String> =
        recentProductsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(8)
}

@Entity(
    tableName = "loans",
    indices = [Index(value = ["personId"]), Index(value = ["startDateMillis"])]
)
data class LoanEntity(
    @PrimaryKey val id: String,
    val isLentByMe: Boolean, // true = I lent money, false = I borrowed money
    val personId: String? = null,
    val personName: String,
    val principalPaisa: Long,
    val interestPaisa: Long = 0L,
    val interestRatePercent: String = "",
    val installmentCount: Int = 1,
    val startDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isFirstLaunchCompleted: Boolean = false,
    val currencySymbol: String = "৳",
    val currencyCode: String = "BDT",
    val openingCashBalancePaisa: Long = 0L,
    val themeMode: String = "DARK", // LIGHT, DARK, SYSTEM, AMOLED
    val accentColorHex: String = "#10B981",
    val backgroundStyle: String = "HERO_ART", // HERO_ART, GRADIENT, SOLID
    val glassTransparency: Int = 88, // 0..100
    val glassBlurAmount: Int = 75, // 0..100
    val cardOpacity: Int = 48, // 0..100 (lower = more translucent glass)
    val uiDensity: String = "COMFORTABLE", // COMFORTABLE, COMPACT
    val languageCode: String = "en", // "en" or "bn"
    val dashboardCardsOrder: String = "CASH_HERO,TODAY_SUMMARY,CURRENT_POSITION,QUICK_ACTIONS,RECENT_ACTIVITY",
    val hiddenDashboardCards: String = "",
    val dashboardLayoutStyle: String = "GRID", // GRID or COMPACT_LIST
    val notificationsEnabled: Boolean = false,
    val loanReminderEnabled: Boolean = true,
    val personDueReminderEnabled: Boolean = true,
    val shopDueReminderEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val biometricEnabled: Boolean = false
) {
    fun visibleDashboardCards(): List<String> {
        val hidden = hiddenDashboardCards.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        return dashboardCardsOrder.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in hidden }
    }

    fun allDashboardCardsOrdered(): List<String> {
        val defaults = listOf("CASH_HERO", "TODAY_SUMMARY", "CURRENT_POSITION", "QUICK_ACTIONS", "RECENT_ACTIVITY")
        val ordered = dashboardCardsOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        for (d in defaults) {
            if (d !in ordered) ordered.add(d)
        }
        return ordered
    }

    fun isCardVisible(cardId: String): Boolean {
        val hidden = hiddenDashboardCards.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        return cardId !in hidden
    }
}

@Entity(tableName = "backup_metadata")
data class BackupMetadataEntity(
    @PrimaryKey val id: Int = 1,
    val lastBackupTimestamp: Long? = null,
    val lastBackupFileName: String? = null,
    val lastBackupTransactionCount: Int = 0,
    val lastBackupSizeBytes: Long = 0L
)
