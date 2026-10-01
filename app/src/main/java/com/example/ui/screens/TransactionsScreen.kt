package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DateFilterPreset
import com.example.data.local.SortOption
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionFilterGroup
import com.example.data.local.TransactionType
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.viewmodel.QuickEntryRequest
import com.example.ui.viewmodel.TransactionsFilterState
import com.example.util.MoneyUtils

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    totalTransactionsCount: Int,
    hasMore: Boolean,
    filterState: TransactionsFilterState,
    onSearchQueryChange: (String) -> Unit,
    onDatePresetChange: (DateFilterPreset, Long?, Long?) -> Unit,
    onFilterGroupChange: (TransactionFilterGroup) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onResetFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onAddTransaction: () -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"

    var showSortMenu by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    val filteredSumPaisa = remember(transactions) {
        transactions.sumOf { it.amountPaisa }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header + Sort Menu
        item(key = "tx_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.navTransactions,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${transactions.size} shown of $totalTransactionsCount total • Sum: ${
                            MoneyUtils.formatPaisa(filteredSumPaisa, theme.currencySymbol)
                        }",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("sort_dropdown_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) filterState.sortOption.labelBn else filterState.sortOption.labelEn,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        for (opt in SortOption.entries) {
                            DropdownMenuItem(
                                text = { Text(if (isBn) opt.labelBn else opt.labelEn) },
                                onClick = {
                                    onSortOptionChange(opt)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Global Search Bar
        item(key = "tx_search_bar") {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )
        }

        // 3. Date Filter Presets Row (Today, Yesterday, This Week, This Month, Last Month, Custom)
        item(key = "tx_date_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                for (preset in DateFilterPreset.entries) {
                    FilterChip(
                        selected = filterState.datePreset == preset,
                        onClick = {
                            if (preset == DateFilterPreset.CUSTOM) {
                                showCustomDateDialog = true
                            } else {
                                onDatePresetChange(preset, null, null)
                            }
                        },
                        label = { Text(if (isBn) preset.labelBn else preset.labelEn) },
                        modifier = Modifier.testTag("date_filter_${preset.name.lowercase()}")
                    )
                }
            }
        }

        // 4. Category/Type Group Filters Row (Income, Expense, Lending, Borrowing, Shop Due, Payments, Loans)
        item(key = "tx_type_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                for (group in TransactionFilterGroup.entries) {
                    FilterChip(
                        selected = filterState.filterGroup == group,
                        onClick = { onFilterGroupChange(group) },
                        label = { Text(if (isBn) group.labelBn else group.labelEn) },
                        modifier = Modifier.testTag("group_filter_${group.name.lowercase()}")
                    )
                }
            }
        }

        // 5. Transactions List or Empty State
        if (transactions.isEmpty()) {
            item(key = "tx_empty") {
                val hasActiveFilters = filterState.searchQuery.isNotEmpty() ||
                    filterState.datePreset != DateFilterPreset.ALL ||
                    filterState.filterGroup != TransactionFilterGroup.ALL

                if (hasActiveFilters) {
                    FriendlyEmptyState(
                        icon = Icons.Default.Search,
                        title = if (isBn) "কোনো ফলাফল পাওয়া যায়নি" else "No matching transactions",
                        subtitle = if (isBn) "অন্য কোনো শব্দ বা ফিল্টার চেষ্টা করুন" else "Try clearing your search or changing the active date/type filters.",
                        actionLabel = if (isBn) "ফিল্টার রিসেট করুন" else "Reset Filters",
                        actionTestTag = "reset_filters_button",
                        onAction = onResetFilters
                    )
                } else {
                    FriendlyEmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = strings.noTransactionsYet,
                        subtitle = strings.addFirstTransaction,
                        actionLabel = "+ ${strings.addTransaction}",
                        actionTestTag = "tx_empty_add_button",
                        onAction = onAddTransaction
                    )
                }
            }
        } else {
            items(items = transactions, key = { it.id }) { tx ->
                GlassCard(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    onClick = { onEditTransaction(tx) }
                ) {
                    TransactionRowItem(
                        tx = tx,
                        currencySymbol = theme.currencySymbol,
                        onClick = { onEditTransaction(tx) }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ID: ${tx.id}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Text(
                            text = MoneyUtils.formatDateTime(tx.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            if (hasMore) {
                item(key = "load_more") {
                    OutlinedButton(
                        onClick = onLoadMore,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_more_transactions_button")
                    ) {
                        Text("Load More Transactions")
                    }
                }
            }
        }
    }

    if (showCustomDateDialog) {
        var daysBackText by remember { mutableStateOf("14") }
        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = { Text("Custom Date Range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Show transactions from the last N days:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = daysBackText,
                        onValueChange = { daysBackText = it },
                        label = { Text("Number of days (e.g. 7, 14, 30, 90)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = daysBackText.toIntOrNull()?.coerceAtLeast(1) ?: 14
                        val now = System.currentTimeMillis()
                        val start = now - days * 24L * 3600_000L
                        onDatePresetChange(DateFilterPreset.CUSTOM, start, now)
                        showCustomDateDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
