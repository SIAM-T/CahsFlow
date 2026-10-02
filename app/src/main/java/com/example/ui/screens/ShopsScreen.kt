package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.ShopBalanceSummary
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.glassTextFieldColors
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactScreen = maxWidth < 360.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            if (selectedSummary != null) {
                BackHandler {
                    onSelectShop(null)
                }
                ShopDetailView(
                    summary = selectedSummary,
                    isCompactScreen = isCompactScreen,
                    onBack = { onSelectShop(null) },
                    onAddDue = {
                        onOpenQuickEntry(
                            QuickEntryRequest(
                                initialType = TransactionType.SHOP_DUE,
                                preselectedShopId = selectedSummary.shop.id
                            )
                        )
                    },
                    onPayDue = {
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
                    isCompactScreen = isCompactScreen,
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
                    onQuickPayDue = { shopId ->
                        onOpenQuickEntry(
                            QuickEntryRequest(
                                initialType = TransactionType.SHOP_PAYMENT,
                                preselectedShopId = shopId
                            )
                        )
                    }
                )
            }
        }
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
    isCompactScreen: Boolean,
    onSelectShop: (String) -> Unit,
    onCreateShop: () -> Unit,
    onQuickAddDue: (String) -> Unit,
    onQuickPayDue: (String) -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shops_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Create Shop button
        item(key = "shops_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.navShops,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBn) "দোকান থেকে বাকিতে পণ্য ক্রয় ও পরে টাকা পরিশোধের হিসাব" else "Take products on due now • Pay money to shop later",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onCreateShop,
                    shape = RoundedCornerShape(16.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "দোকানে মোট বাকি (পরিশোধযোগ্য)" else "Total Shop Due (To Pay Later)",
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
                        Text(
                            text = if (isBn) "বাকিতে নেওয়া পণ্যের মোট বকেয়া" else "Remaining balance for products taken on credit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        FinanceShopDue.copy(alpha = 0.35f),
                                        FinanceShopDue.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(1.dp, FinanceShopDue.copy(alpha = 0.5f), CircleShape),
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
                    subtitle = if (isBn) "দোকান তৈরি করে বাকিতে নেওয়া পণ্য ও পরিশোধের হিসাব রাখুন" else "Create a shop (e.g. Rahman Store) to record products taken on due and pay later.",
                    actionLabel = "+ ${strings.createFirstShop}",
                    actionTestTag = "empty_create_shop_button",
                    onAction = onCreateShop
                )
            }
        } else {
            items(items = shopSummaries, key = { it.shop.id }) { summary ->
                val shopColor = parseHexColor(summary.shop.colorHex, FinanceShopDue)
                GlassCard(
                    tintColor = shopColor,
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
                                    .background(shopColor.copy(alpha = 0.22f))
                                    .border(1.dp, shopColor.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = summary.shop.name,
                                    tint = shopColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = summary.shop.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val meta = listOf(summary.shop.ownerName, summary.shop.phone, summary.shop.address)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                Text(
                                    text = meta.ifEmpty {
                                        "Taken: ${MoneyUtils.formatPaisa(summary.totalDueTakenPaisa, theme.currencySymbol)} • Paid: ${MoneyUtils.formatPaisa(summary.totalPaidToShopPaisa, theme.currencySymbol)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBn) "বর্তমান বাকি" else "Due to Pay",
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

                    // Responsive Fast Action Buttons: "+ Add Due (Product)" & "Pay Due (Money)"
                    if (isCompactScreen) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onQuickAddDue(summary.shop.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FinanceShopDue.copy(alpha = 0.25f),
                                    contentColor = FinanceShopDue
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shop_quick_due_${summary.shop.name.lowercase().replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "+ বাকি যোগ (পণ্য নিন)" else "+ Add Due (Get Product)", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onQuickPayDue(summary.shop.id) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, FinanceIncome.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shop_quick_receive_${summary.shop.name.lowercase().replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = FinanceIncome, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "বাকি পরিশোধ (টাকা দিন)" else "Pay Due (Give Money)",
                                    color = FinanceIncome,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onQuickAddDue(summary.shop.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FinanceShopDue.copy(alpha = 0.24f),
                                    contentColor = FinanceShopDue
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shop_quick_due_${summary.shop.name.lowercase().replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "+ বাকি (পণ্য নিন)" else "+ Add Due",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onQuickPayDue(summary.shop.id) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, FinanceIncome.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shop_quick_receive_${summary.shop.name.lowercase().replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = FinanceIncome, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "বাকি পরিশোধ" else "Pay Due",
                                    color = FinanceIncome,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
    isCompactScreen: Boolean,
    onBack: () -> Unit,
    onAddDue: () -> Unit,
    onPayDue: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onEditShop: () -> Unit,
    onDeleteShop: () -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val groupedByDate = remember(summary.transactions) {
        summary.transactions
            .sortedByDescending { it.timestamp }
            .groupBy { MoneyUtils.formatDateShort(it.timestamp, "en") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shop_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        modifier = Modifier.testTag("shop_detail_back_button")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.shop.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = strings.edit,
                        onClick = onEditShop,
                        modifier = Modifier.testTag("edit_shop_button")
                    )
                    GlassIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = FinanceExpense,
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_shop_button")
                    )
                }
            }
        }

        // 2. Hero Due Balance & Quick Shop Entry Buttons
        item(key = "shop_detail_hero") {
            GlassCard(
                tintColor = FinanceShopDue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isBn) "বর্তমান বাকি (দোকানকে দিতে হবে)" else "Current Due (Money to Pay Shop)",
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
                            "মোট বাকিতে পণ্য ক্রয়: ${MoneyUtils.formatPaisa(summary.totalDueTakenPaisa, theme.currencySymbol)}"
                        } else {
                            "Products Taken: ${MoneyUtils.formatPaisa(summary.totalDueTakenPaisa, theme.currencySymbol)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBn) {
                            "মোট পরিশোধ: ${MoneyUtils.formatPaisa(summary.totalPaidToShopPaisa, theme.currencySymbol)}"
                        } else {
                            "Total Paid: ${MoneyUtils.formatPaisa(summary.totalPaidToShopPaisa, theme.currencySymbol)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceIncome,
                        fontWeight = FontWeight.SemiBold
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

                if (isCompactScreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAddDue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinanceShopDue,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("shop_detail_add_due_button")
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "+ বাকি যোগ করুন (পণ্য নিন)" else "+ Add Due (Get Product)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onPayDue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinanceIncome,
                                contentColor = Color(0xFF042F2E)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("shop_detail_receive_payment_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "বাকি পরিশোধ করুন (টাকা দিন)" else "Pay Due (Give Money)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
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
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "+ বাকি (পণ্য নিন)" else "+ Add Due (Product)",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onPayDue,
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
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "বাকি পরিশোধ" else "Pay Due (Money)",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Date-Grouped Shop History
        item(key = "shop_history_title") {
            Text(
                text = if (isBn) "লেনদেনের ইতিহাস (Transactions)" else "Transactions History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        if (groupedByDate.isEmpty()) {
            item(key = "shop_history_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.Storefront,
                    title = strings.noTransactionsYet,
                    subtitle = if (isBn) {
                        "উপরে '+ বাকি' বাটনে চাপ দিয়ে ${summary.shop.name} থেকে বাকিতে নেওয়া পণ্যের হিসাব লিখুন।"
                    } else {
                        "Tap '+ Add Due' above when you get products from ${summary.shop.name} to pay later."
                    },
                    actionLabel = "+ Add Due",
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
                                        text = "• ${tx.productOrDescription.ifBlank { if (isPayment) "Due paid to shop" else "Products on due" }}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isPayment) {
                                            if (tx.note.isNotBlank()) "Paid to shop • ${tx.note}" else "Paid money to shop (Reduces due)"
                                        } else {
                                            if (tx.note.isNotBlank()) "Got product on due • ${tx.note}" else "Got product on due"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
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
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
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
    var name by remember(existingShop) { mutableStateOf(existingShop?.name ?: "") }
    var owner by remember(existingShop) { mutableStateOf(existingShop?.ownerName ?: "") }
    var phone by remember(existingShop) { mutableStateOf(existingShop?.phone ?: "") }
    var address by remember(existingShop) { mutableStateOf(existingShop?.address ?: "") }
    var note by remember(existingShop) { mutableStateOf(existingShop?.note ?: "") }
    var colorHex by remember(existingShop) { mutableStateOf(existingShop?.colorHex ?: "#F59E0B") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        title = {
            Text(if (existingShop == null) "Create Shop" else "Edit Shop")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shop Name * (e.g. Rahman Store)") },
                    colors = glassTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_shop_name_input")
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner Name (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    colors = glassTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    colors = glassTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    colors = glassTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    colors = glassTextFieldColors(),
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
