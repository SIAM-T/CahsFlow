package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinancialSnapshot
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.QuickActionGlassButton
import com.example.ui.components.colorForTransactionType
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.iconForTransactionType
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceLoan
import com.example.ui.theme.FinancePayable
import com.example.ui.theme.FinanceReceivable
import com.example.ui.theme.FinanceShopDue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.MoreSubScreen
import com.example.ui.viewmodel.QuickEntryRequest
import com.example.util.MoneyUtils
import java.util.Calendar

@Composable
fun HomeScreen(
    settings: AppSettingsEntity,
    snapshot: FinancialSnapshot,
    recentTransactions: List<TransactionEntity>,
    onOpenQuickEntry: (QuickEntryRequest) -> Unit,
    onOpenLoanCreator: () -> Unit,
    onSelectTab: (MainNavTab) -> Unit,
    onOpenMoreSubScreen: (MoreSubScreen) -> Unit,
    onOpenSearch: () -> Unit,
    onToggleDashboardCard: (String) -> Unit,
    onMoveDashboardCard: (String, Boolean) -> Unit,
    onChangeDashboardLayout: (String) -> Unit,
    onCompleteFirstLaunch: (String, String, Long, Boolean) -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"
    var showCustomizeModal by remember { mutableStateOf(false) }

    val greeting = remember(strings) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> strings.greetingMorning
            hour < 17 -> strings.greetingAfternoon
            else -> strings.greetingEvening
        }
    }

    val visibleCards = remember(settings) { settings.visibleDashboardCards() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Greeting & Translucent Glass Action Bar
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.moneyOverview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search",
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("home_search_button")
                    )
                    GlassIconButton(
                        icon = Icons.Default.DashboardCustomize,
                        contentDescription = "Customize Dashboard",
                        onClick = { showCustomizeModal = true },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("home_customize_button")
                    )
                }
            }
        }

        // 2. First Launch Setup Card (if not yet completed)
        if (!settings.isFirstLaunchCompleted) {
            item(key = "first_launch_setup") {
                FirstLaunchSetupCard(
                    onComplete = onCompleteFirstLaunch
                )
            }
        }

        // 3. Render Customizable Dashboard Cards in User's Chosen Order
        items(items = visibleCards, key = { it }) { cardId ->
            when (cardId) {
                "CASH_HERO" -> CashBalanceHeroCard(
                    snapshot = snapshot,
                    currencySymbol = theme.currencySymbol
                )

                "QUICK_ACTIONS" -> QuickActionsDashboardCard(
                    onOpenQuickEntry = onOpenQuickEntry,
                    onOpenLoanCreator = onOpenLoanCreator
                )

                "TODAY_SUMMARY" -> TodaySummaryCard(
                    snapshot = snapshot,
                    currencySymbol = theme.currencySymbol,
                    isCompactList = settings.dashboardLayoutStyle == "COMPACT_LIST"
                )

                "CURRENT_POSITION" -> CurrentPositionCard(
                    snapshot = snapshot,
                    currencySymbol = theme.currencySymbol,
                    isCompactList = settings.dashboardLayoutStyle == "COMPACT_LIST",
                    onNavigatePeople = { onSelectTab(MainNavTab.PEOPLE) },
                    onNavigateShops = { onSelectTab(MainNavTab.SHOPS) },
                    onNavigateLoans = { onOpenMoreSubScreen(MoreSubScreen.LOANS) }
                )

                "RECENT_ACTIVITY" -> RecentActivityDashboardSection(
                    recentTransactions = recentTransactions,
                    currencySymbol = theme.currencySymbol,
                    onEditTransaction = { tx ->
                        onOpenQuickEntry(
                            QuickEntryRequest(
                                initialType = TransactionType.fromString(tx.type),
                                editingTransaction = tx
                            )
                        )
                    },
                    onViewAll = { onSelectTab(MainNavTab.TRANSACTIONS) },
                    onAddFirst = {
                        onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.EXPENSE))
                    }
                )
            }
        }
    }

    if (showCustomizeModal) {
        DashboardCustomizerDialog(
            settings = settings,
            isBn = isBn,
            onDismiss = { showCustomizeModal = false },
            onToggleCard = onToggleDashboardCard,
            onMoveCard = onMoveDashboardCard,
            onChangeLayout = onChangeDashboardLayout
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstLaunchSetupCard(
    onComplete: (String, String, Long, Boolean) -> Unit
) {
    val strings = LocalHisabStrings.current
    var selectedCurrency by remember { mutableStateOf("৳" to "BDT") }
    var startingBalanceInput by remember { mutableStateOf("10000") }
    var includeDemoData by remember { mutableStateOf(false) }

    val currencies = listOf(
        "৳" to "BDT",
        "$" to "USD",
        "₹" to "INR",
        "€" to "EUR",
        "£" to "GBP",
        "SAR" to "SAR"
    )

    GlassCard(
        tintColor = MaterialTheme.colorScheme.primary,
        cornerRadius = 28.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("first_launch_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.welcomeToHisab,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = {
                    onComplete(selectedCurrency.first, selectedCurrency.second, 0L, false)
                },
                modifier = Modifier.testTag("skip_first_launch_button")
            ) {
                Text(strings.skipSetup)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.chooseCurrency,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (pair in currencies) {
                GlassChip(
                    selected = selectedCurrency == pair,
                    label = "${pair.first} ${pair.second}",
                    onClick = { selectedCurrency = pair },
                    modifier = Modifier.testTag("currency_chip_${pair.second}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = startingBalanceInput,
            onValueChange = { startingBalanceInput = it },
            label = { Text("${strings.startingCashBalance} (${selectedCurrency.first})") },
            singleLine = true,
            colors = glassTextFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_starting_cash_balance")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { includeDemoData = !includeDemoData },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeDemoData,
                onCheckedChange = { includeDemoData = it },
                modifier = Modifier.testTag("checkbox_demo_data")
            )
            Text(
                text = "Load sample হিসাব entries (Rahman Store, Rahim, Karim)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val paisa = MoneyUtils.parseToPaisa(startingBalanceInput) ?: 0L
                onComplete(selectedCurrency.first, selectedCurrency.second, paisa, includeDemoData)
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("get_started_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.getStarted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CashBalanceHeroCard(
    snapshot: FinancialSnapshot,
    currencySymbol: String
) {
    val strings = LocalHisabStrings.current
    val theme = LocalHisabTheme.current

    val totalInOut = snapshot.reports.totalIncomePaisa + snapshot.reports.totalExpensePaisa
    val incomeRatio = if (totalInOut > 0L) {
        (snapshot.reports.totalIncomePaisa.toFloat() / totalInOut.toFloat()).coerceIn(0.08f, 0.92f)
    } else 0.5f

    GlassCard(
        tintColor = theme.accentColor,
        cornerRadius = 28.dp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Decorative glowing refraction rings inside the Hero Vault Card
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.accentColor.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.18f),
                        radius = size.width * 0.48f
                    ),
                    radius = size.width * 0.48f,
                    center = Offset(size.width * 0.88f, size.height * 0.18f)
                )
            }
            .testTag("cash_balance_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    theme.accentColor.copy(alpha = 0.35f),
                                    theme.accentColor.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(1.dp, theme.accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = strings.cashBalance,
                        tint = theme.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = strings.cashBalance,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Net Cash Position",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (snapshot.openingCashBalancePaisa > 0L) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = if (theme.isDark) 0.09f else 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${strings.openingBalance}: ${MoneyUtils.formatPaisa(snapshot.openingCashBalancePaisa, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = MoneyUtils.formatPaisa(snapshot.cashBalancePaisa, currencySymbol),
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("cash_balance_value")
        )

        if (totalInOut > 0L) {
            Spacer(modifier = Modifier.height(12.dp))
            // Visual Cash Flow Ratio Bar (Income vs Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FinanceExpense.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(incomeRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(FinanceIncome, Color(0xFF34D399))
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Income Glass Pill
            GlassMetricPill(
                label = strings.income,
                amountText = MoneyUtils.formatPaisa(snapshot.reports.totalIncomePaisa, currencySymbol),
                icon = Icons.Default.ArrowUpward,
                color = FinanceIncome,
                modifier = Modifier.weight(1f)
            )

            // Total Expense Glass Pill
            GlassMetricPill(
                label = strings.expense,
                amountText = MoneyUtils.formatPaisa(snapshot.reports.totalExpensePaisa, currencySymbol),
                icon = Icons.Default.ArrowDownward,
                color = FinanceExpense,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GlassMetricPill(
    label: String,
    amountText: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalHisabTheme.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = if (theme.isDark) 0.20f else 0.14f),
                        Color.White.copy(alpha = if (theme.isDark) 0.03f else 0.45f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            color.copy(alpha = 0.40f)
                        )
                    )
                ),
                shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickActionsDashboardCard(
    onOpenQuickEntry: (QuickEntryRequest) -> Unit,
    onOpenLoanCreator: () -> Unit
) {
    val strings = LocalHisabStrings.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_actions_card")
    ) {
        Text(
            text = strings.quickActionsTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionGlassButton(
                    label = "+ ${strings.income}",
                    icon = Icons.Default.Add,
                    color = FinanceIncome,
                    testTag = "quick_action_income",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.INCOME)) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionGlassButton(
                    label = "− ${strings.expense}",
                    icon = Icons.Default.Remove,
                    color = FinanceExpense,
                    testTag = "quick_action_expense",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.EXPENSE)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionGlassButton(
                    label = strings.shopDue,
                    icon = Icons.Default.Storefront,
                    color = FinanceShopDue,
                    testTag = "quick_action_shop_due",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.SHOP_DUE)) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionGlassButton(
                    label = strings.receivePayment,
                    icon = Icons.Default.Payments,
                    color = FinanceIncome,
                    testTag = "quick_action_receive",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.SHOP_PAYMENT)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionGlassButton(
                    label = strings.giveMoney,
                    icon = Icons.AutoMirrored.Filled.CallMade,
                    color = FinanceReceivable,
                    testTag = "quick_action_give",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.LEND)) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionGlassButton(
                    label = strings.takeMoney,
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    color = FinancePayable,
                    testTag = "quick_action_take",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.BORROW)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionGlassButton(
                    label = strings.payMoney,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    color = FinanceExpense,
                    testTag = "quick_action_pay",
                    onClick = { onOpenQuickEntry(QuickEntryRequest(initialType = TransactionType.MAKE_PAYMENT)) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionGlassButton(
                    label = strings.loan,
                    icon = Icons.Default.AccountBalance,
                    color = FinanceLoan,
                    testTag = "quick_action_loan",
                    onClick = onOpenLoanCreator,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(
    snapshot: FinancialSnapshot,
    currencySymbol: String,
    isCompactList: Boolean
) {
    val strings = LocalHisabStrings.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_summary_card")
    ) {
        Text(
            text = strings.todayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isCompactList) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricListRow(strings.income, snapshot.todayIncomePaisa, currencySymbol, FinanceIncome)
                MetricListRow(strings.expense, snapshot.todayExpensePaisa, currencySymbol, FinanceExpense)
                MetricListRow(strings.moneyReceived, snapshot.todayMoneyReceivedPaisa, currencySymbol, FinanceReceivable)
                MetricListRow(strings.moneyGiven, snapshot.todayMoneyGivenPaisa, currencySymbol, FinancePayable)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatTile(
                    label = strings.income,
                    amountPaisa = snapshot.todayIncomePaisa,
                    currencySymbol = currencySymbol,
                    color = FinanceIncome,
                    modifier = Modifier.weight(1f)
                )
                MiniStatTile(
                    label = strings.expense,
                    amountPaisa = snapshot.todayExpensePaisa,
                    currencySymbol = currencySymbol,
                    color = FinanceExpense,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatTile(
                    label = strings.moneyReceived,
                    amountPaisa = snapshot.todayMoneyReceivedPaisa,
                    currencySymbol = currencySymbol,
                    color = FinanceReceivable,
                    modifier = Modifier.weight(1f)
                )
                MiniStatTile(
                    label = strings.moneyGiven,
                    amountPaisa = snapshot.todayMoneyGivenPaisa,
                    currencySymbol = currencySymbol,
                    color = FinancePayable,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CurrentPositionCard(
    snapshot: FinancialSnapshot,
    currencySymbol: String,
    isCompactList: Boolean,
    onNavigatePeople: () -> Unit,
    onNavigateShops: () -> Unit,
    onNavigateLoans: () -> Unit
) {
    val strings = LocalHisabStrings.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("current_position_card")
    ) {
        Text(
            text = strings.currentPositionTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isCompactList) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricListRow(strings.receivable, snapshot.totalReceivablePaisa, currencySymbol, FinanceReceivable, onNavigatePeople)
                MetricListRow(strings.payable, snapshot.totalPayablePaisa, currencySymbol, FinancePayable, onNavigatePeople)
                MetricListRow(strings.shopDue, snapshot.totalShopDuePaisa, currencySymbol, FinanceShopDue, onNavigateShops)
                MetricListRow(strings.activeLoans, snapshot.totalActiveLoansPaisa, currencySymbol, FinanceLoan, onNavigateLoans)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatTile(
                    label = strings.receivable,
                    amountPaisa = snapshot.totalReceivablePaisa,
                    currencySymbol = currencySymbol,
                    color = FinanceReceivable,
                    onClick = onNavigatePeople,
                    testTag = "stat_tile_receivable",
                    modifier = Modifier.weight(1f)
                )
                MiniStatTile(
                    label = strings.payable,
                    amountPaisa = snapshot.totalPayablePaisa,
                    currencySymbol = currencySymbol,
                    color = FinancePayable,
                    onClick = onNavigatePeople,
                    testTag = "stat_tile_payable",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatTile(
                    label = strings.shopDue,
                    amountPaisa = snapshot.totalShopDuePaisa,
                    currencySymbol = currencySymbol,
                    color = FinanceShopDue,
                    onClick = onNavigateShops,
                    testTag = "stat_tile_shop_due",
                    modifier = Modifier.weight(1f)
                )
                MiniStatTile(
                    label = strings.activeLoans,
                    amountPaisa = snapshot.totalActiveLoansPaisa,
                    currencySymbol = currencySymbol,
                    color = FinanceLoan,
                    onClick = onNavigateLoans,
                    testTag = "stat_tile_active_loans",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniStatTile(
    label: String,
    amountPaisa: Long,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    val theme = LocalHisabTheme.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = if (theme.isDark) 0.22f else 0.14f),
                        Color.White.copy(alpha = if (theme.isDark) 0.03f else 0.50f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (theme.isDark) 0.30f else 0.85f),
                            color.copy(alpha = 0.40f)
                        )
                    )
                ),
                shape
            )
            .let { if (testTag != null) it.testTag(testTag) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = MoneyUtils.formatPaisa(amountPaisa, currencySymbol),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricListRow(
    label: String,
    amountPaisa: Long,
    currencySymbol: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
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
private fun RecentActivityDashboardSection(
    recentTransactions: List<TransactionEntity>,
    currencySymbol: String,
    onEditTransaction: (TransactionEntity) -> Unit,
    onViewAll: () -> Unit,
    onAddFirst: () -> Unit
) {
    val strings = LocalHisabStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.recentActivityTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (recentTransactions.isNotEmpty()) {
                TextButton(
                    onClick = onViewAll,
                    modifier = Modifier.testTag("view_all_transactions_button")
                ) {
                    Text("View All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            FriendlyEmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = strings.noTransactionsYet,
                subtitle = strings.addFirstTransaction,
                actionLabel = "+ ${strings.addTransaction}",
                actionTestTag = "home_empty_add_tx_button",
                onAction = onAddFirst
            )
        } else {
            GlassCard(
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 14.dp)
            ) {
                recentTransactions.take(6).forEachIndexed { index, tx ->
                    TransactionRowItem(
                        tx = tx,
                        currencySymbol = currencySymbol,
                        onClick = { onEditTransaction(tx) }
                    )
                    if (index < recentTransactions.take(6).lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tType = TransactionType.fromString(tx.type)
    val color = colorForTransactionType(tType)
    val primaryTitle = when {
        !tx.shopName.isNullOrBlank() -> tx.shopName
        !tx.personName.isNullOrBlank() -> tx.personName
        !tx.categoryName.isNullOrBlank() -> tx.categoryName
        else -> tType.labelEn
    }
    val secondarySubtitle = buildString {
        if (tx.productOrDescription.isNotBlank()) {
            append(tx.productOrDescription)
        } else {
            append(tType.labelEn)
        }
        if (tx.note.isNotBlank()) {
            append(" • ")
            append(tx.note)
        }
    }

    val prefix = when {
        tType == TransactionType.EXPENSE ||
            tType == TransactionType.LEND ||
            tType == TransactionType.MAKE_PAYMENT ||
            tType == TransactionType.LOAN_GIVEN ||
            tType == TransactionType.LOAN_PAYMENT -> "−"
        else -> "+"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("tx_row_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.30f),
                            color.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.dp, color.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForTransactionType(tType),
                contentDescription = tType.labelEn,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primaryTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = secondarySubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$prefix${MoneyUtils.formatPaisa(tx.amountPaisa, currencySymbol)}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Text(
                text = MoneyUtils.formatDateShort(tx.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardCustomizerDialog(
    settings: AppSettingsEntity,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onToggleCard: (String) -> Unit,
    onMoveCard: (String, Boolean) -> Unit,
    onChangeLayout: (String) -> Unit
) {
    val allCards = settings.allDashboardCardsOrdered()
    val cardNames = mapOf(
        "CASH_HERO" to (if (isBn) "নগদ ব্যালেন্স কার্ড" else "Cash Balance Overview"),
        "QUICK_ACTIONS" to (if (isBn) "দ্রুত এন্ট্রি বাটন" else "Quick Actions Bar"),
        "TODAY_SUMMARY" to (if (isBn) "আজকের হিসাব" else "Today's Summary"),
        "CURRENT_POSITION" to (if (isBn) "বর্তমান অবস্থা (পাওনা/দেনা)" else "Current Position (Receivable/Payable)"),
        "RECENT_ACTIVITY" to (if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Activity")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        title = {
            Text(if (isBn) "ড্যাশবোর্ড কাস্টমাইজ করুন" else "Customize Dashboard")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBn) "কার্ড লুকান, দেখান বা উপরে-নিচে সাজান:" else "Show, hide, or reorder dashboard cards:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassChip(
                        selected = settings.dashboardLayoutStyle == "GRID",
                        label = "2x2 Stat Grid",
                        onClick = { onChangeLayout("GRID") }
                    )
                    GlassChip(
                        selected = settings.dashboardLayoutStyle == "COMPACT_LIST",
                        label = "Compact List",
                        onClick = { onChangeLayout("COMPACT_LIST") }
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                allCards.forEachIndexed { index, cardId ->
                    val visible = settings.isCardVisible(cardId)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { onToggleCard(cardId) }) {
                                Icon(
                                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = cardNames[cardId] ?: cardId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { onMoveCard(cardId, true) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onMoveCard(cardId, false) },
                                enabled = index < allCards.lastIndex
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
