package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.LoanStatus
import com.example.data.local.PersonEntity
import com.example.data.local.TransactionType
import com.example.domain.FinancialEngine
import com.example.domain.FinancialSnapshot
import com.example.domain.LoanSummary
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.iconForName
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceLoan
import com.example.ui.theme.FinancePayable
import com.example.ui.theme.FinanceReceivable
import com.example.ui.theme.FinanceShopDue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.MoreSubScreen
import com.example.ui.viewmodel.QuickEntryRequest
import com.example.util.MoneyUtils

@Composable
fun MoreHubScreen(
    snapshot: FinancialSnapshot,
    activeNotesCount: Int = 0,
    onSelectSubScreen: (MoreSubScreen) -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"

    data class MoreMenuItem(
        val subScreen: MoreSubScreen,
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val color: Color,
        val badgeText: String? = null,
        val testTag: String
    )

    val items = listOf(
        MoreMenuItem(
            MoreSubScreen.NOTES,
            if (isBn) "স্মার্ট নোট, চেকলিস্ট ও রিমাইন্ডার" else "Smart Notes, To-Do & Reminders",
            if (isBn) "বাজেট চেকলিস্ট, স্ট্যাটাস বার পিন ও রিয়েল-টাইম অ্যালার্ট" else "Financial checklists, status-bar pin & real-time alerts",
            Icons.Default.Edit,
            Color(0xFF06B6D4),
            "$activeNotesCount Notes",
            "more_item_notes"
        ),
        MoreMenuItem(
            MoreSubScreen.LOANS,
            strings.loansSection,
            if (isBn) "ঋণ গ্রহণ, প্রদান, কিস্তি ও বকেয়া ট্র্যাকিং" else "Track money borrowed & lent with installments",
            Icons.Default.AccountBalance,
            FinanceLoan,
            MoneyUtils.formatPaisa(snapshot.totalActiveLoansPaisa, theme.currencySymbol),
            "more_item_loans"
        ),
        MoreMenuItem(
            MoreSubScreen.REPORTS,
            strings.reportsSection,
            if (isBn) "দৈনিক, মাসিক ও বাৎসরিক আয়-ব্যয় বিশ্লেষণ" else "Daily, weekly, monthly & category breakdowns",
            Icons.Default.BarChart,
            FinanceIncome,
            null,
            "more_item_reports"
        ),
        MoreMenuItem(
            MoreSubScreen.CATEGORIES,
            strings.categoriesSection,
            if (isBn) "আয় ও ব্যয়ের ক্যাটাগরি যোগ, পরিবর্তন ও সাজানো" else "Add, rename, reorder & customize icons",
            Icons.Default.Category,
            FinanceShopDue,
            null,
            "more_item_categories"
        ),
        MoreMenuItem(
            MoreSubScreen.BACKUP,
            strings.backupSection,
            if (isBn) "JSON ব্যাকআপ এক্সপোর্ট, ইমপোর্ট এবং CSV" else "Export JSON/CSV, validate & restore backups",
            Icons.Default.Backup,
            FinanceReceivable,
            null,
            "more_item_backup"
        ),
        MoreMenuItem(
            MoreSubScreen.CUSTOMIZATION,
            strings.customizationSection,
            if (isBn) "থিম, গ্লাস ইফেক্ট, রং এবং মুদ্রা পরিবর্তন" else "Theme, glass blur, opacity, density & currency",
            Icons.Default.Palette,
            theme.accentColor,
            null,
            "more_item_customization"
        ),
        MoreMenuItem(
            MoreSubScreen.SECURITY,
            strings.securitySection,
            if (isBn) "পিন লক এবং বায়োমেট্রিক নিরাপত্তা" else "Optional local PIN & Biometric app lock",
            Icons.Default.Lock,
            FinancePayable,
            null,
            "more_item_security"
        ),
        MoreMenuItem(
            MoreSubScreen.SETTINGS,
            strings.settingsSection,
            if (isBn) "ভাষা (বাংলা/English), নোটিফিকেশন ও ডাটা ব্যবস্থাপনা" else "Language (English/বাংলা), reminders & data tools",
            Icons.Default.Settings,
            Color(0xFF64748B),
            null,
            "more_item_settings"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_hub_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "more_header") {
            Column {
                Text(
                    text = strings.navMore,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isBn) "ঋণ, রিপোর্ট, ব্যাকআপ এবং কাস্টমাইজেশন" else "Loans, reports, backups, customization & settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(items = items, key = { it.subScreen.name }) { item ->
            GlassCard(
                onClick = { onSelectSubScreen(item.subScreen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(item.testTag)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(item.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = item.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = item.color.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = item.badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = item.color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LoansSubScreen(
    loanSummaries: List<LoanSummary>,
    people: List<PersonEntity>,
    totalLoansGivenRemainingPaisa: Long,
    totalLoansTakenRemainingPaisa: Long,
    availableCashPaisa: Long = Long.MAX_VALUE,
    showCreateDialogInitially: Boolean = false,
    onBack: () -> Unit,
    onCreateLoan: (Boolean, String?, String, Long, Long, String, Int, Long, Long?, String) -> Unit,
    onDeleteLoan: (String) -> Unit,
    onOpenRepaymentEntry: (QuickEntryRequest) -> Unit
) {
    BackHandler { onBack() }
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"

    var showCreateDialog by remember(showCreateDialogInitially) { mutableStateOf(showCreateDialogInitially) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loans_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "loans_topbar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("loans_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = strings.loansSection,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("create_loan_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "নতুন ঋণ" else "New Loan")
                }
            }
        }

        // Summary Row: Loans I Lent vs Loans I Borrowed
        item(key = "loans_summary_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    tintColor = FinanceReceivable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.iLentMoney,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyUtils.formatPaisa(totalLoansGivenRemainingPaisa, theme.currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FinanceReceivable
                    )
                }

                GlassCard(
                    tintColor = FinancePayable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.iBorrowedMoney,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyUtils.formatPaisa(totalLoansTakenRemainingPaisa, theme.currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FinancePayable
                    )
                }
            }
        }

        if (loanSummaries.isEmpty()) {
            item(key = "loans_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.AccountBalance,
                    title = strings.noLoansYet,
                    subtitle = strings.youreAllClear,
                    actionLabel = "+ Add Loan",
                    actionTestTag = "empty_add_loan_button",
                    onAction = { showCreateDialog = true }
                )
            }
        } else {
            items(items = loanSummaries, key = { it.loan.id }) { summary ->
                val isLent = summary.loan.isLentByMe
                val accent = if (isLent) FinanceReceivable else FinancePayable
                val statusColor = when (summary.status) {
                    LoanStatus.PAID -> FinanceIncome
                    LoanStatus.OVERDUE -> FinanceExpense
                    LoanStatus.PARTIALLY_PAID -> FinanceShopDue
                    LoanStatus.ACTIVE -> FinanceLoan
                }
                val progress = if (summary.totalObligationPaisa > 0L) {
                    (summary.repaidPaisa.toFloat() / summary.totalObligationPaisa.toFloat()).coerceIn(0f, 1f)
                } else 0f

                GlassCard(
                    tintColor = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_card_${summary.loan.personName.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLent) "Loan to ${summary.loan.personName}" else "Loan from ${summary.loan.personName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Started: ${MoneyUtils.formatDateFull(summary.loan.startDateMillis)}" +
                                    (summary.loan.dueDateMillis?.let { " • Due: ${MoneyUtils.formatDateFull(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isBn) summary.status.labelBn else summary.status.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = MoneyUtils.formatPaisa(summary.loan.principalPaisa, theme.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Repaid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = MoneyUtils.formatPaisa(summary.repaidPaisa, theme.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = FinanceIncome,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = MoneyUtils.formatPaisa(summary.remainingPaisa, theme.currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("loan_remaining_${summary.loan.personName.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = FinanceIncome,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    if (summary.repayments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Repayment History (${summary.repayments.size} installments):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        for (rep in summary.repayments.take(3)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${MoneyUtils.formatDateShort(rep.timestamp)} — ${rep.productOrDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyUtils.formatPaisa(rep.amountPaisa, theme.currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = FinanceIncome
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (summary.remainingPaisa > 0L) {
                            Button(
                                onClick = {
                                    val txType = if (isLent) TransactionType.LOAN_REPAYMENT else TransactionType.LOAN_PAYMENT
                                    onOpenRepaymentEntry(
                                        QuickEntryRequest(
                                            initialType = txType,
                                            preselectedPersonId = summary.loan.personId,
                                            preselectedLoanId = summary.loan.id
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("record_loan_repayment_${summary.loan.personName.lowercase()}")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isLent) "Receive Repayment" else "Pay Installment")
                            }
                        } else {
                            Text(
                                text = "✓ Fully Settled",
                                style = MaterialTheme.typography.labelLarge,
                                color = FinanceIncome,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { onDeleteLoan(summary.loan.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = FinanceExpense)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateLoanDialog(
            people = people,
            currencySymbol = theme.currencySymbol,
            availableCashPaisa = availableCashPaisa,
            onDismiss = { showCreateDialog = false },
            onCreate = { isLent, pId, pName, principal, interest, rateStr, installments, dueMillis, note ->
                onCreateLoan(
                    isLent,
                    pId,
                    pName,
                    principal,
                    interest,
                    rateStr,
                    installments,
                    System.currentTimeMillis(),
                    dueMillis,
                    note
                )
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateLoanDialog(
    people: List<PersonEntity>,
    currencySymbol: String,
    availableCashPaisa: Long = Long.MAX_VALUE,
    onDismiss: () -> Unit,
    onCreate: (Boolean, String?, String, Long, Long, String, Int, Long?, String) -> Unit
) {
    var isLentByMe by remember { mutableStateOf(false) } // Default: I borrowed money or toggle
    var personName by remember { mutableStateOf("") }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var principalInput by remember { mutableStateOf("") }
    var interestInput by remember { mutableStateOf("0") }
    var installmentsInput by remember { mutableStateOf("1") }
    var dueDays by remember { mutableStateOf<Int?>(30) }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedPrincipal = MoneyUtils.parseToPaisa(principalInput) ?: 0L
    val safeCash = availableCashPaisa.coerceAtLeast(0L)
    val exceedsCash = isLentByMe && availableCashPaisa != Long.MAX_VALUE && parsedPrincipal > safeCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Loan Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isLentByMe,
                        onClick = {
                            isLentByMe = false
                            errorText = null
                        },
                        label = { Text("I Borrowed") },
                        modifier = Modifier.testTag("loan_type_borrowed")
                    )
                    FilterChip(
                        selected = isLentByMe,
                        onClick = {
                            isLentByMe = true
                            errorText = null
                        },
                        label = { Text("I Lent") },
                        modifier = Modifier.testTag("loan_type_lent")
                    )
                }

                if (isLentByMe && availableCashPaisa != Long.MAX_VALUE) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (exceedsCash) FinanceExpense else FinanceIncome).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, (if (exceedsCash) FinanceExpense else FinanceIncome).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Available Cash to Lend",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyUtils.formatPaisa(safeCash, currencySymbol),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (exceedsCash) FinanceExpense else FinanceIncome,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (safeCash > 0L) {
                                TextButton(
                                    onClick = {
                                        principalInput = MoneyUtils.paisaToEditableString(safeCash)
                                        errorText = null
                                    }
                                ) {
                                    Text("Max")
                                }
                            }
                        }
                    }
                }

                if (people.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (p in people.take(5)) {
                            FilterChip(
                                selected = selectedPersonId == p.id,
                                onClick = {
                                    selectedPersonId = p.id
                                    personName = p.name
                                    errorText = null
                                },
                                label = { Text(p.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        selectedPersonId = null
                        errorText = null
                    },
                    label = { Text("Person Name * (e.g. Karim / Rahim)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_loan_person_input")
                )

                OutlinedTextField(
                    value = principalInput,
                    onValueChange = {
                        principalInput = it
                        errorText = null
                    },
                    label = { Text("Loan Amount ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_loan_amount_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interestInput,
                        onValueChange = { interestInput = it },
                        label = { Text("Interest ($currencySymbol, Opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = installmentsInput,
                        onValueChange = { installmentsInput = it },
                        label = { Text("Installments") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val shownErr = errorText ?: if (exceedsCash) {
                    "Insufficient Cash Balance! You only have ${MoneyUtils.formatPaisa(safeCash, currencySymbol)} available, so you cannot lend ${MoneyUtils.formatPaisa(parsedPrincipal, currencySymbol)}."
                } else null

                if (shownErr != null) {
                    Text(
                        text = shownErr,
                        color = FinanceExpense,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("dialog_loan_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val principalPaisa = MoneyUtils.parseToPaisa(principalInput) ?: 0L
                    val interestPaisa = MoneyUtils.parseToPaisa(interestInput) ?: 0L
                    val inst = installmentsInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val dueMillis = dueDays?.let { System.currentTimeMillis() + it * 24L * 3600_000L }
                    if (personName.isBlank()) {
                        errorText = "Please enter a person's name."
                        return@Button
                    }
                    if (principalPaisa <= 0L) {
                        errorText = "Please enter a loan amount greater than 0."
                        return@Button
                    }
                    if (isLentByMe && availableCashPaisa != Long.MAX_VALUE && principalPaisa > safeCash) {
                        errorText = "Insufficient Cash Balance! You only have ${MoneyUtils.formatPaisa(safeCash, currencySymbol)} available, so you cannot lend ${MoneyUtils.formatPaisa(principalPaisa, currencySymbol)}."
                        return@Button
                    }
                    onCreate(
                        isLentByMe,
                        selectedPersonId,
                        personName.trim(),
                        principalPaisa,
                        interestPaisa,
                        "",
                        inst,
                        dueMillis,
                        note
                    )
                },
                modifier = Modifier.testTag("dialog_save_loan_button")
            ) {
                Text("Save Loan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReportsSubScreen(
    snapshot: FinancialSnapshot,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val reports = snapshot.reports

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "reports_topbar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = strings.reportsSection,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. Period Income vs Expense Breakdown (Daily, Weekly, Monthly, Yearly)
        item(key = "reports_periods") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Income & Expense by Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                PeriodComparisonRow("Today (Daily)", reports.dailyIncomePaisa, reports.dailyExpensePaisa, theme.currencySymbol)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                PeriodComparisonRow("This Week", reports.weeklyIncomePaisa, reports.weeklyExpensePaisa, theme.currencySymbol)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                PeriodComparisonRow("This Month", reports.monthlyIncomePaisa, reports.monthlyExpensePaisa, theme.currencySymbol)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                PeriodComparisonRow("This Year", reports.yearlyIncomePaisa, reports.yearlyExpensePaisa, theme.currencySymbol)
            }
        }

        // 2. Current Position Summary (Receivable, Payable, Shop Due, Loans)
        item(key = "reports_position") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Net Financial Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                PositionReportRow("Cash Balance", snapshot.cashBalancePaisa, theme.currencySymbol, theme.accentColor)
                PositionReportRow("Total Receivable (Others owe me)", snapshot.totalReceivablePaisa, theme.currencySymbol, FinanceReceivable)
                PositionReportRow("Total Payable (I owe others)", snapshot.totalPayablePaisa, theme.currencySymbol, FinancePayable)
                PositionReportRow("Total Shop Dues", snapshot.totalShopDuePaisa, theme.currencySymbol, FinanceShopDue)
                PositionReportRow("Total Active Loans", snapshot.totalActiveLoansPaisa, theme.currencySymbol, FinanceLoan)
            }
        }

        // 3. Expense by Category Chart
        item(key = "reports_expense_by_category") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Expenses by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (reports.expenseByCategory.isEmpty()) {
                    Text(
                        text = "No expenses recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (item in reports.expenseByCategory) {
                            val catColor = parseHexColor(item.colorHex, FinanceExpense)
                            val pctWhole = item.percentageBasisPoints / 100
                            val progress = (item.percentageBasisPoints / 10000f).coerceIn(0.02f, 1f)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = iconForName(item.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${item.categoryName} ($pctWhole%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = MoneyUtils.formatPaisa(item.totalPaisa, theme.currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = FinanceExpense,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(catColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodComparisonRow(
    periodTitle: String,
    incomePaisa: Long,
    expensePaisa: Long,
    currencySymbol: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = periodTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "↑ ${MoneyUtils.formatPaisa(incomePaisa, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = FinanceIncome,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "↓ ${MoneyUtils.formatPaisa(expensePaisa, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = FinanceExpense,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PositionReportRow(
    label: String,
    amountPaisa: Long,
    currencySymbol: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = MoneyUtils.formatPaisa(amountPaisa, currencySymbol),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoriesSubScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSaveCategory: (CategoryEntity?, String, String, String, String, String) -> Unit,
    onMoveCategory: (CategoryEntity, Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    BackHandler { onBack() }
    val strings = LocalHisabStrings.current
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val filtered = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }.sortedBy { it.sortOrder }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("categories_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "cat_topbar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = strings.categoriesSection,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Button(
                    onClick = {
                        editingCategory = null
                        showEditDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_category_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Category")
                }
            }
        }

        item(key = "cat_type_tabs") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedType == "EXPENSE",
                    onClick = { selectedType = "EXPENSE" },
                    label = { Text("Expense Categories") }
                )
                FilterChip(
                    selected = selectedType == "INCOME",
                    onClick = { selectedType = "INCOME" },
                    label = { Text("Income Categories") }
                )
            }
        }

        items(items = filtered, key = { it.id }) { cat ->
            val catColor = parseHexColor(cat.colorHex)
            GlassCard(
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconForName(cat.iconName),
                                contentDescription = cat.name,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (cat.nameBn.isNotBlank()) {
                                Text(
                                    text = cat.nameBn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onMoveCategory(cat, true) }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onMoveCategory(cat, false) }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                editingCategory = cat
                                showEditDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FinanceExpense, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
        var nameBn by remember(editingCategory) { mutableStateOf(editingCategory?.nameBn ?: "") }
        var iconName by remember(editingCategory) { mutableStateOf(editingCategory?.iconName ?: "Restaurant") }
        var colorHex by remember(editingCategory) { mutableStateOf(editingCategory?.colorHex ?: "#10B981") }

        val icons = listOf(
            "Restaurant", "DirectionsBus", "ShoppingBag", "ReceiptLong",
            "School", "LocalHospital", "Movie", "FamilyRestroom", "Wifi",
            "Work", "Store", "Laptop", "CardGiftcard", "Savings", "MoreHoriz"
        )

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingCategory == null) "Add Category" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name (English)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_category_name_input")
                    )
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text("Category Name (বাংলা, Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Icon:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (ic in icons.take(6)) {
                            val active = iconName == ic
                            Surface(
                                onClick = { iconName = ic },
                                shape = CircleShape,
                                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(iconForName(ic), contentDescription = ic, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveCategory(editingCategory, name, nameBn, selectedType, iconName, colorHex)
                            showEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_save_category_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
