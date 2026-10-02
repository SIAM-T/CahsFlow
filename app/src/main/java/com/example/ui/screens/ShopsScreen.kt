package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.ShopBalanceSummary
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceShopDue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.QuickEntryRequest
import com.example.util.MoneyUtils

@Composable
fun ShopsScreen(
    shopSummaries: List<ShopBalanceSummary>,
    totalShopDuePaisa: Long,
    selectedShopId: String?,
    onSelectShop: (String?) -> Unit,
    onOpenQuickEntry: (QuickEntryRequest) -> Unit,
    onSaveShop: (ShopEntity?, String, String, String, String, String, String) -> Unit,
    onDeleteShop: (String) -> Unit
) {
    val selectedSummary = remember(shopSummaries, selectedShopId) {
        shopSummaries.find { it.shop.id == selectedShopId }
    }

    var showCreateOrEditShopDialog by remember { mutableStateOf(false) }
    var editingShop by remember { mutableStateOf<ShopEntity?>(null) }

    if (selectedSummary != null) {
        BackHandler {
            onSelectShop(null)
        }
        ShopDetailView(
            summary = selectedSummary,
            onBack = { onSelectShop(null) },
            onAddDue = {
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.SHOP_DUE,
                        preselectedShopId = selectedSummary.shop.id
                    )
                )
            },
            onReceivePayment = {
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.SHOP_PAYMENT,
                        preselectedShopId = selectedSummary.shop.id
                    )
                )
            },
            onEditTransaction = { tx ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.fromString(tx.type),
                        editingTransaction = tx
                    )
                )
            },
            onEditShop = {
                editingShop = selectedSummary.shop
                showCreateOrEditShopDialog = true
            },
            onDeleteShop = {
                onDeleteShop(selectedSummary.shop.id)
            }
        )
    } else {
        ShopListView(
            shopSummaries = shopSummaries,
            totalShopDuePaisa = totalShopDuePaisa,
            onSelectShop = { onSelectShop(it) },
            onCreateShop = {
                editingShop = null
                showCreateOrEditShopDialog = true
            },
            onQuickAddDue = { shopId ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.SHOP_DUE,
                        preselectedShopId = shopId
                    )
                )
            },
            onQuickReceivePayment = { shopId ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.SHOP_PAYMENT,
                        preselectedShopId = shopId
                    )
                )
            }
        )
    }

    if (showCreateOrEditShopDialog) {
        ShopFormDialog(
            existingShop = editingShop,
            onDismiss = { showCreateOrEditShopDialog = false },
            onSave = { name, owner, phone, address, note, color ->
                onSaveShop(editingShop, name, owner, phone, address, note, color)
                showCreateOrEditShopDialog = false
            }
        )
    }
}

@Composable
private fun ShopListView(
    shopSummaries: List<ShopBalanceSummary>,
    totalShopDuePaisa: Long,
    onSelectShop: (String) -> Unit,
    onCreateShop: () -> Unit,
    onQuickAddDue: (String) -> Unit,
    onQuickReceivePayment: (String) -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shops_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Create Shop button
        item(key = "shops_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = strings.navShops,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isBn) "দোকানে বাকি ও পরিশোধের সম্পূর্ণ ডিজিটাল খাতা" else "Manage shop dues, products & payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onCreateShop,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("create_shop_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "নতুন দোকান" else "New Shop", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Total Shop Due Banner Card
        item(key = "shops_total_due_banner") {
            GlassCard(
                tintColor = FinanceShopDue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBn) "দোকানে মোট বাকি (দোকানদার পাবেন)" else "Total Shop Due (Payable to Shops)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyUtils.formatPaisa(totalShopDuePaisa, theme.currencySymbol),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = FinanceShopDue,
                            modifier = Modifier.testTag("total_shop_due_amount")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(FinanceShopDue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = FinanceShopDue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Empty State or Shop Cards
        if (shopSummaries.isEmpty()) {
            item(key = "shops_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.Storefront,
                    title = strings.noShopsYet,
                    subtitle = strings.createFirstShop,
                    actionLabel = "+ ${strings.createFirstShop}",
                    actionTestTag = "empty_create_shop_button",
                    onAction = onCreateShop
                )
            }
        } else {
            items(items = shopSummaries, key = { it.shop.id }) { summary ->
                val shopColor = parseHexColor(summary.shop.colorHex, FinanceShopDue)
                GlassCard(
                    onClick = { onSelectShop(summary.shop.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_card_${summary.shop.name.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(shopColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = summary.shop.name,
                                    tint = shopColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = summary.shop.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                val meta = listOf(summary.shop.ownerName, summary.shop.phone, summary.shop.address)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                if (meta.isNotEmpty()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "${summary.transactionCount} transactions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.currentDue,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = MoneyUtils.formatPaisa(summary.currentDuePaisa, theme.currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (summary.currentDuePaisa > 0L) FinanceShopDue else FinanceIncome
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fast inline buttons on each shop card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onQuickAddDue(summary.shop.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinanceShopDue.copy(alpha = 0.2f),
                                contentColor = FinanceShopDue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shop_quick_due_${summary.shop.name.lowercase().replace(" ", "_")}")
                        ) {
                            Text(if (isBn) "+ বাকিতে ক্রয়" else "+ Add Due", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onQuickReceivePayment(summary.shop.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FinanceIncome.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shop_quick_receive_${summary.shop.name.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = if (isBn) "বাকি পরিশোধ" else "Pay Due",
                                color = FinanceIncome,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopDetailView(
    summary: ShopBalanceSummary,
    onBack: () -> Unit,
    onAddDue: () -> Unit,
    onReceivePayment: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onEditShop: () -> Unit,
    onDeleteShop: () -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Group transactions by formatted date (e.g. "01 Oct", "02 Oct", "03 Oct") as specified in Section 10
    val groupedByDate = remember(summary.transactions) {
        summary.transactions
            .sortedByDescending { it.timestamp }
            .groupBy { MoneyUtils.formatDateShort(it.timestamp, "en") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shop_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Bar
        item(key = "shop_detail_topbar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("shop_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = summary.shop.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (summary.shop.ownerName.isNotBlank() || summary.shop.phone.isNotBlank()) {
                            Text(
                                text = listOf(summary.shop.ownerName, summary.shop.phone)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onEditShop,
                        modifier = Modifier.testTag("edit_shop_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit)
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_shop_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = FinanceExpense)
                    }
                }
            }
        }

        // 2. Hero Due Balance & Quick Shop Entry Buttons (Section 42)
        item(key = "shop_detail_hero") {
            GlassCard(
                tintColor = FinanceShopDue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isBn) "দোকানদার পাবেন (বর্তমান বাকি)" else strings.currentDue,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MoneyUtils.formatPaisa(summary.currentDuePaisa, theme.currencySymbol),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FinanceShopDue,
                    modifier = Modifier.testTag("shop_detail_current_due")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isBn) {
                            "মোট বাকি নিয়েছি: ${MoneyUtils.formatPaisa(summary.totalDueGivenPaisa, theme.currencySymbol)}"
                        } else {
                            "Total Bought on Due: ${MoneyUtils.formatPaisa(summary.totalDueGivenPaisa, theme.currencySymbol)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBn) {
                            "মোট পরিশোধ করেছি: ${MoneyUtils.formatPaisa(summary.totalPaymentReceivedPaisa, theme.currencySymbol)}"
                        } else {
                            "Total Paid to Shop: ${MoneyUtils.formatPaisa(summary.totalPaymentReceivedPaisa, theme.currencySymbol)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceIncome
                    )
                }

                if (summary.shop.address.isNotBlank() || summary.shop.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = listOf(summary.shop.address, summary.shop.note).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Action Buttons: + Add Due & Pay Shop Due
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddDue,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinanceShopDue,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("shop_detail_add_due_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "+ বাকিতে কেনাকাটা" else "+ Add Due",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onReceivePayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinanceIncome,
                            contentColor = Color(0xFF042F2E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("shop_detail_receive_payment_button")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "বাকি পরিশোধ করুন" else "Pay Shop Due",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Date-Grouped Shop History (Section 10)
        item(key = "shop_history_title") {
            Text(
                text = if (isBn) "লেনদেনের ইতিহাস (Transactions)" else "Transactions History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (groupedByDate.isEmpty()) {
            item(key = "shop_history_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.Storefront,
                    title = strings.noTransactionsYet,
                    subtitle = if (isBn) {
                        "দোকান থেকে বাকিতে কেনাকাটার হিসাব রাখতে উপরে '+ বাকিতে কেনাকাটা'-তে চাপুন।"
                    } else {
                        "Tap '+ Add Due' above to record items bought on credit from ${summary.shop.name}."
                    },
                    actionLabel = if (isBn) "+ বাকিতে কেনাকাটা" else "+ Add Due",
                    onAction = onAddDue
                )
            }
        } else {
            groupedByDate.forEach { (dateLabel, dayTxs) ->
                item(key = "date_$dateLabel") {
                    GlassCard(
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        dayTxs.forEachIndexed { idx, tx ->
                            val isPayment = TransactionType.fromString(tx.type) == TransactionType.SHOP_PAYMENT
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditTransaction(tx) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "• ${tx.productOrDescription.ifBlank { if (isPayment) (if (isBn) "বাকি পরিশোধ" else "Payment to shop") else (if (isBn) "বাকিতে পণ্য ক্রয়" else "Products") }}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (tx.note.isNotBlank()) {
                                        Text(
                                            text = tx.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (isPayment) "−" else "+"}${MoneyUtils.formatPaisa(tx.amountPaisa, theme.currencySymbol)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isPayment) FinanceIncome else FinanceShopDue
                                )
                            }
                            if (idx < dayTxs.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${summary.shop.name}?") },
            text = { Text("This will remove the shop profile from your list.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteShop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpense)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun ShopFormDialog(
    existingShop: ShopEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    val theme = LocalHisabTheme.current
    val isBn = theme.languageCode == "bn"

    var name by remember(existingShop) { mutableStateOf(existingShop?.name ?: "") }
    var owner by remember(existingShop) { mutableStateOf(existingShop?.ownerName ?: "") }
    var phone by remember(existingShop) { mutableStateOf(existingShop?.phone ?: "") }
    var address by remember(existingShop) { mutableStateOf(existingShop?.address ?: "") }
    var note by remember(existingShop) { mutableStateOf(existingShop?.note ?: "") }
    var colorHex by remember(existingShop) { mutableStateOf(existingShop?.colorHex ?: "#F59E0B") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingShop == null) (if (isBn) "নতুন দোকান যোগ করুন" else "New Shop") else (if (isBn) "দোকানের তথ্য সম্পাদনা" else "Edit Shop"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isBn) "দোকানের নাম * (যেমন: রহমান স্টোর)" else "Shop Name * (e.g. Rahman Store)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_shop_name_input")
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text(if (isBn) "দোকানদারের নাম (ঐচ্ছিক)" else "Owner Name (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isBn) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isBn) "দোকানের ঠিকানা (ঐচ্ছিক)" else "Address (Optional)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isBn) "নোট / বিবরণ (ঐচ্ছিক)" else "Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, owner, phone, address, note, colorHex)
                    }
                },
                modifier = Modifier.testTag("dialog_save_shop_button")
            ) {
                Text(if (isBn) "সংরক্ষণ করুন" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
