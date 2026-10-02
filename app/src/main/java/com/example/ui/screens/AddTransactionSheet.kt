package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.LoanEntity
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionType
import com.example.domain.FinancialEngine
import com.example.domain.FinancialSnapshot
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.colorForTransactionType
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.iconForName
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
import com.example.ui.viewmodel.QuickEntryRequest
import com.example.util.MoneyUtils

data class GlobalActionOption(
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val type: TransactionType,
    val icon: ImageVector,
    val color: Color,
    val testTag: String,
    val isLoanModal: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAddTransactionPickerSheet(
    onDismiss: () -> Unit,
    onSelectType: (TransactionType) -> Unit,
    onOpenLoanCreator: () -> Unit
) {
    val theme = LocalHisabTheme.current
    val isBn = theme.languageCode == "bn"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val options = remember {
        listOf(
            GlobalActionOption(
                "Income", "আয় (+ Income)",
                "Salary, business, freelance, gift", "বেতন, ব্যবসা বা অন্যান্য আয়",
                TransactionType.INCOME, Icons.Default.Add, FinanceIncome, "global_add_income"
            ),
            GlobalActionOption(
                "Expense", "ব্যয় (− Expense)",
                "Food, transport, bills, shopping", "খাবার, যাতায়াত, বিল ও কেনাকাটা",
                TransactionType.EXPENSE, Icons.Default.Remove, FinanceExpense, "global_add_expense"
            ),
            GlobalActionOption(
                "Shop Due", "দোকানের বাকি (Shop Due)",
                "Products given to a shop on credit", "দোকানে বাকিতে পণ্য প্রদান",
                TransactionType.SHOP_DUE, Icons.Default.Storefront, FinanceShopDue, "global_add_shop_due"
            ),
            GlobalActionOption(
                "Receive Shop Payment", "দোকান থেকে আদায়",
                "Collect due payment from a shop", "দোকানের বকেয়া টাকা আদায়",
                TransactionType.SHOP_PAYMENT, Icons.Default.Payments, FinanceIncome, "global_add_shop_payment"
            ),
            GlobalActionOption(
                "Give Money (Lend)", "টাকা দেওয়া (ধার)",
                "Money lent to someone (Receivable)", "কাউকে টাকা ধার দেওয়া (পাওনা)",
                TransactionType.LEND, Icons.AutoMirrored.Filled.CallMade, FinanceReceivable, "global_add_lend"
            ),
            GlobalActionOption(
                "Take Money (Borrow)", "টাকা নেওয়া (ধার)",
                "Money borrowed from someone (Payable)", "কারো থেকে টাকা ধার নেওয়া (দেনা)",
                TransactionType.BORROW, Icons.AutoMirrored.Filled.CallReceived, FinancePayable, "global_add_borrow"
            ),
            GlobalActionOption(
                "Receive Payment", "পাওনা টাকা আদায়",
                "Someone paid back what they owed me", "পাওনা টাকা ফেরত পাওয়া",
                TransactionType.RECEIVE_PAYMENT, Icons.AutoMirrored.Filled.TrendingUp, FinanceIncome, "global_add_receive"
            ),
            GlobalActionOption(
                "Pay Money", "দেনা পরিশোধ",
                "Repay money I owed to someone", "ধারের টাকা পরিশোধ করা",
                TransactionType.MAKE_PAYMENT, Icons.AutoMirrored.Filled.TrendingDown, FinanceExpense, "global_add_pay"
            ),
            GlobalActionOption(
                "Loan", "ঋণ (Formal Loan)",
                "Track loan principal, installments & status", "কিস্তি ও মেয়াদসহ ঋণের হিসাব",
                TransactionType.LOAN_GIVEN, Icons.Default.AccountBalance, FinanceLoan, "global_add_loan",
                isLoanModal = true
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (theme.isDark) Color(0xFF0B1324).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f),
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBn) "নতুন লেনদেন যোগ করুন" else "Add Transaction",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBn) "লেনদেনের ধরন নির্বাচন করুন" else "Choose transaction type in one tap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            for (opt in options) {
                GlassCard(
                    tintColor = opt.color,
                    cornerRadius = 20.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    onClick = {
                        if (opt.isLoanModal) {
                            onDismiss()
                            onOpenLoanCreator()
                        } else {
                            onSelectType(opt.type)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(opt.testTag)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            opt.color.copy(alpha = 0.35f),
                                            opt.color.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                                .border(1.dp, opt.color.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = opt.titleEn,
                                tint = opt.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) opt.titleBn else opt.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) opt.subtitleBn else opt.subtitleEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickEntryBottomSheet(
    request: QuickEntryRequest,
    categories: List<CategoryEntity>,
    shops: List<ShopEntity>,
    people: List<PersonEntity>,
    loans: List<LoanEntity>,
    snapshot: FinancialSnapshot,
    onDismiss: () -> Unit,
    onSave: (
        existingTx: com.example.data.local.TransactionEntity?,
        type: TransactionType,
        amountPaisa: Long,
        timestamp: Long,
        categoryId: String?,
        categoryName: String?,
        personId: String?,
        personName: String?,
        shopId: String?,
        shopName: String?,
        loanId: String?,
        productOrDescription: String,
        note: String,
        dueDateMillis: Long?
    ) -> Unit,
    onDeleteExisting: ((String) -> Unit)? = null
) {
    val editingTx = request.editingTransaction
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember(request) {
        mutableStateOf(
            editingTx?.let { TransactionType.fromString(it.type) } ?: request.initialType
        )
    }

    var amountText by remember(request) {
        mutableStateOf(
            editingTx?.let { MoneyUtils.paisaToEditableString(it.amountPaisa) }
                ?: request.initialAmountPaisa?.let { MoneyUtils.paisaToEditableString(it) }
                ?: ""
        )
    }

    val relevantCategories = remember(categories, selectedType) {
        val catType = if (selectedType == TransactionType.INCOME) "INCOME" else "EXPENSE"
        categories.filter { it.type == catType }
            .sortedWith(compareByDescending<CategoryEntity> { it.usageCount }.thenBy { it.sortOrder })
    }

    var selectedCategoryId by remember(request, selectedType) {
        mutableStateOf(
            editingTx?.categoryId
                ?: request.preselectedCategoryId
                ?: relevantCategories.firstOrNull()?.id
        )
    }

    var selectedShopId by remember(request) {
        mutableStateOf(
            editingTx?.shopId ?: request.preselectedShopId ?: shops.firstOrNull()?.id
        )
    }
    var customShopName by remember(request) {
        mutableStateOf(editingTx?.shopName ?: "")
    }
    var isCreatingNewShopInline by remember(request, shops) {
        mutableStateOf(shops.isEmpty())
    }

    var selectedPersonId by remember(request) {
        mutableStateOf(
            editingTx?.personId ?: request.preselectedPersonId ?: people.firstOrNull()?.id
        )
    }
    var customPersonName by remember(request) {
        mutableStateOf(editingTx?.personName ?: "")
    }
    var isCreatingNewPersonInline by remember(request, people) {
        mutableStateOf(people.isEmpty())
    }

    var selectedLoanId by remember(request) {
        mutableStateOf(editingTx?.loanId ?: request.preselectedLoanId ?: loans.firstOrNull()?.id)
    }

    var productOrDescription by remember(request) {
        mutableStateOf(editingTx?.productOrDescription ?: request.initialDescription ?: "")
    }

    var noteText by remember(request) {
        mutableStateOf(editingTx?.note ?: request.initialNote ?: "")
    }

    var dueDaysOffset by remember(request) {
        mutableStateOf<Int?>(null)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedAmountPaisa = MoneyUtils.parseToPaisa(amountText) ?: 0L
    val typeAccentColor = colorForTransactionType(selectedType)

    val selectedShop: ShopEntity? = shops.find { it.id == selectedShopId }
    val selectedShopSummary = snapshot.shopSummaries.find { it.shop.id == selectedShopId }

    val selectedPerson: PersonEntity? = people.find { it.id == selectedPersonId }
    val selectedPersonSummary = snapshot.personSummaries.find { it.person.id == selectedPersonId }

    val selectedCategory: CategoryEntity? = relevantCategories.find { it.id == selectedCategoryId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (theme.isDark) Color(0xFF0B1324).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.90f),
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        typeAccentColor.copy(alpha = 0.35f),
                                        typeAccentColor.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(1.dp, typeAccentColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForTransactionType(selectedType),
                            contentDescription = selectedType.labelEn,
                            tint = typeAccentColor
                        )
                    }
                    Column {
                        Text(
                            text = if (editingTx != null) {
                                if (isBn) "লেনদেন সম্পাদনা" else "Edit Transaction"
                            } else {
                                if (isBn) selectedType.labelBn else selectedType.labelEn
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "ন্যূনতম তথ্যে দ্রুত সংরক্ষণ করুন" else "Fast entry • Automatic balance calculation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingTx != null && onDeleteExisting != null) {
                        GlassIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = strings.delete,
                            tint = FinanceExpense,
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_transaction_button")
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        onClick = onDismiss
                    )
                }
            }

            // Translucent Glass Type Switcher Row
            val quickTypes = listOf(
                TransactionType.EXPENSE,
                TransactionType.INCOME,
                TransactionType.SHOP_DUE,
                TransactionType.SHOP_PAYMENT,
                TransactionType.LEND,
                TransactionType.RECEIVE_PAYMENT,
                TransactionType.BORROW,
                TransactionType.MAKE_PAYMENT
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (t in quickTypes) {
                    val selected = t == selectedType
                    val c = colorForTransactionType(t)
                    GlassChip(
                        selected = selected,
                        label = if (isBn) t.labelBn else t.labelEn,
                        accentColor = c,
                        onClick = {
                            selectedType = t
                            validationError = null
                        },
                        modifier = Modifier.testTag("type_chip_${t.name.lowercase()}")
                    )
                }
            }

            // 1. Context Selector: SHOP
            if (selectedType == TransactionType.SHOP_DUE || selectedType == TransactionType.SHOP_PAYMENT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.selectShop,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (shops.isNotEmpty()) {
                            TextButton(
                                onClick = { isCreatingNewShopInline = !isCreatingNewShopInline }
                            ) {
                                Text(
                                    text = if (isCreatingNewShopInline) "Pick Existing Shop" else "+ New Shop"
                                )
                            }
                        }
                    }

                    if (isCreatingNewShopInline || shops.isEmpty()) {
                        OutlinedTextField(
                            value = customShopName,
                            onValueChange = {
                                customShopName = it
                                selectedShopId = null
                                validationError = null
                            },
                            label = { Text(if (isBn) "দোকানের নাম (যেমন: Rahman Store)" else "Shop Name (e.g. Rahman Store)") },
                            singleLine = true,
                            colors = glassTextFieldColors(typeAccentColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_shop_name")
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (shop in shops) {
                                val isSel = shop.id == selectedShopId
                                GlassChip(
                                    selected = isSel,
                                    label = shop.name,
                                    accentColor = FinanceShopDue,
                                    leadingIcon = Icons.Default.Storefront,
                                    onClick = {
                                        selectedShopId = shop.id
                                        customShopName = shop.name
                                        validationError = null
                                    },
                                    modifier = Modifier.testTag("shop_chip_${shop.name.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    // Live Shop Balance Preview Card
                    if (selectedShopSummary != null && !isCreatingNewShopInline) {
                        val currentDue = selectedShopSummary.currentDuePaisa
                        val projectedDue = if (selectedType == TransactionType.SHOP_DUE) {
                            currentDue + parsedAmountPaisa
                        } else {
                            (currentDue - parsedAmountPaisa).coerceAtLeast(0L)
                        }
                        GlassCard(
                            tintColor = FinanceShopDue,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedShopSummary.shop.name} • Previous Due",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyUtils.formatPaisa(currentDue, theme.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (selectedType == TransactionType.SHOP_DUE) "New Total Due" else "Remaining Due",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyUtils.formatPaisa(projectedDue, theme.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = if (selectedType == TransactionType.SHOP_DUE) FinanceShopDue else FinanceIncome,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Context Selector: PERSON
            if (selectedType == TransactionType.LEND ||
                selectedType == TransactionType.BORROW ||
                selectedType == TransactionType.RECEIVE_PAYMENT ||
                selectedType == TransactionType.MAKE_PAYMENT
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.selectPerson,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (people.isNotEmpty()) {
                            TextButton(
                                onClick = { isCreatingNewPersonInline = !isCreatingNewPersonInline }
                            ) {
                                Text(
                                    text = if (isCreatingNewPersonInline) "Pick Existing Person" else "+ New Person"
                                )
                            }
                        }
                    }

                    if (isCreatingNewPersonInline || people.isEmpty()) {
                        OutlinedTextField(
                            value = customPersonName,
                            onValueChange = {
                                customPersonName = it
                                selectedPersonId = null
                                validationError = null
                            },
                            label = { Text(if (isBn) "ব্যক্তির নাম (যেমন: Rahim / Karim)" else "Person Name (e.g. Rahim / Karim)") },
                            singleLine = true,
                            colors = glassTextFieldColors(typeAccentColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_person_name")
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (person in people) {
                                val isSel = person.id == selectedPersonId
                                GlassChip(
                                    selected = isSel,
                                    label = person.name,
                                    accentColor = typeAccentColor,
                                    onClick = {
                                        selectedPersonId = person.id
                                        customPersonName = person.name
                                        validationError = null
                                    },
                                    modifier = Modifier.testTag("person_chip_${person.name.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    if (selectedPersonSummary != null && !isCreatingNewPersonInline) {
                        val isReceivableFlow = selectedType == TransactionType.LEND || selectedType == TransactionType.RECEIVE_PAYMENT
                        val currentBal = if (isReceivableFlow) {
                            selectedPersonSummary.directReceivablePaisa
                        } else {
                            selectedPersonSummary.directPayablePaisa
                        }
                        val projectedBal = when (selectedType) {
                            TransactionType.LEND -> currentBal + parsedAmountPaisa
                            TransactionType.RECEIVE_PAYMENT -> (currentBal - parsedAmountPaisa).coerceAtLeast(0L)
                            TransactionType.BORROW -> currentBal + parsedAmountPaisa
                            TransactionType.MAKE_PAYMENT -> (currentBal - parsedAmountPaisa).coerceAtLeast(0L)
                            else -> currentBal
                        }
                        GlassCard(
                            tintColor = if (isReceivableFlow) FinanceReceivable else FinancePayable,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isReceivableFlow) {
                                            "${selectedPersonSummary.person.name} owes me"
                                        } else {
                                            "I owe ${selectedPersonSummary.person.name}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyUtils.formatPaisa(currentBal, theme.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "After Transaction",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyUtils.formatPaisa(projectedBal, theme.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = if (isReceivableFlow) FinanceReceivable else FinancePayable,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Context Selector: CATEGORY
            if (selectedType == TransactionType.INCOME || selectedType == TransactionType.EXPENSE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.selectCategory,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (cat in relevantCategories) {
                            val isSel = cat.id == selectedCategoryId
                            GlassChip(
                                selected = isSel,
                                label = if (isBn && cat.nameBn.isNotEmpty()) cat.nameBn else cat.name,
                                accentColor = typeAccentColor,
                                leadingIcon = iconForName(cat.iconName),
                                onClick = { selectedCategoryId = cat.id },
                                modifier = Modifier.testTag("category_chip_${cat.name.lowercase().replace("/", "_").replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 3B. Context Selector: LOAN (for Loan Repayment / Installment)
            if (selectedType == TransactionType.LOAN_REPAYMENT || selectedType == TransactionType.LOAN_PAYMENT) {
                val matchingLoans = snapshot.loanSummaries.filter {
                    val wantLent = selectedType == TransactionType.LOAN_REPAYMENT
                    it.loan.isLentByMe == wantLent
                }
                val activeLoanSum = snapshot.loanSummaries.find { it.loan.id == selectedLoanId }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn) "ঋণ নির্বাচন করুন" else "Select Active Loan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (matchingLoans.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (ls in matchingLoans) {
                                GlassChip(
                                    selected = ls.loan.id == selectedLoanId,
                                    label = "${ls.loan.personName} (${MoneyUtils.formatPaisa(ls.remainingPaisa, theme.currencySymbol)})",
                                    accentColor = FinanceLoan,
                                    onClick = {
                                        selectedLoanId = ls.loan.id
                                        selectedPersonId = ls.loan.personId
                                        customPersonName = ls.loan.personName
                                        validationError = null
                                    }
                                )
                            }
                        }
                    }
                    if (activeLoanSum != null) {
                        val remBefore = activeLoanSum.remainingPaisa
                        val remAfter = (remBefore - parsedAmountPaisa).coerceAtLeast(0L)
                        GlassCard(
                            tintColor = FinanceLoan,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Loan Remaining (${activeLoanSum.loan.personName})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyUtils.formatPaisa(remBefore, theme.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "After Repayment",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyUtils.formatPaisa(remAfter, theme.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = FinanceIncome,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Cash Balance & Strict Limit Guardrail Card
            val effectiveShopIdForCheck = if (isCreatingNewShopInline) null else selectedShop?.id
            val effectivePersonIdForCheck = if (isCreatingNewPersonInline) null else selectedPerson?.id
            val maxAllowedPaisa = FinancialEngine.computeMaxAllowedAmountPaisa(
                snapshot = snapshot,
                existingTx = editingTx,
                type = selectedType,
                shopId = effectiveShopIdForCheck,
                personId = effectivePersonIdForCheck,
                loanId = selectedLoanId
            )
            val effectiveCashAvailable = (snapshot.cashBalancePaisa - (editingTx?.let { FinancialEngine.cashEffectOf(it) } ?: 0L)).coerceAtLeast(0L)
            val isCashOutflow = FinancialEngine.isCashOutflowType(selectedType)
            val liveProposalError = if (parsedAmountPaisa > 0L) {
                FinancialEngine.validateTransactionProposal(
                    snapshot = snapshot,
                    existingTx = editingTx,
                    type = selectedType,
                    amountPaisa = parsedAmountPaisa,
                    shopId = effectiveShopIdForCheck,
                    shopName = if (isCreatingNewShopInline || selectedShop == null) customShopName.trim() else selectedShop.name,
                    personId = effectivePersonIdForCheck,
                    personName = if (isCreatingNewPersonInline || selectedPerson == null) customPersonName.trim() else selectedPerson.name,
                    loanId = selectedLoanId,
                    currencySymbol = theme.currencySymbol
                )
            } else null

            if (isCashOutflow || maxAllowedPaisa != null) {
                val isExceeded = liveProposalError != null
                val guardTint = if (isExceeded) FinanceExpense else theme.accentColor
                GlassCard(
                    tintColor = guardTint,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balance_guardrail_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isCashOutflow && maxAllowedPaisa != null && maxAllowedPaisa < effectiveCashAvailable ->
                                        "Available Cash: ${MoneyUtils.formatPaisa(effectiveCashAvailable, theme.currencySymbol)} • Max Payable: ${MoneyUtils.formatPaisa(maxAllowedPaisa, theme.currencySymbol)}"
                                    isCashOutflow ->
                                        if (isBn) "ব্যবহারযোগ্য নগদ ব্যালেন্স (Available Cash)" else "Available Cash Balance"
                                    selectedType == TransactionType.SHOP_PAYMENT ->
                                        "Max Collectible Shop Due"
                                    selectedType == TransactionType.RECEIVE_PAYMENT ->
                                        "Max Collectible Receivable"
                                    else -> "Maximum Allowed Amount"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val shownCap = maxAllowedPaisa ?: effectiveCashAvailable
                            Text(
                                text = MoneyUtils.formatPaisa(shownCap, theme.currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (isExceeded) FinanceExpense else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("available_limit_value")
                            )
                            if (isCashOutflow && parsedAmountPaisa > 0L && !isExceeded) {
                                val remainingCashAfter = (effectiveCashAvailable - parsedAmountPaisa).coerceAtLeast(0L)
                                Text(
                                    text = "Cash after transaction: ${MoneyUtils.formatPaisa(remainingCashAfter, theme.currencySymbol)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (maxAllowedPaisa != null && maxAllowedPaisa > 0L) {
                            GlassChip(
                                selected = parsedAmountPaisa == maxAllowedPaisa,
                                label = "Max (${MoneyUtils.formatPaisa(maxAllowedPaisa, theme.currencySymbol)})",
                                accentColor = guardTint,
                                onClick = {
                                    amountText = MoneyUtils.paisaToEditableString(maxAllowedPaisa)
                                    validationError = null
                                },
                                modifier = Modifier.testTag("fill_max_allowed_chip")
                            )
                        }
                    }
                }
            }

            // 4. Amount Input + Fast Amount Glass Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = { Text("${strings.amountLabel} (${theme.currencySymbol})") },
                    placeholder = { Text("500") },
                    singleLine = true,
                    colors = glassTextFieldColors(typeAccentColor),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount")
                )

                val presets = listOf(100L, 200L, 500L, 1000L, 5000L)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (preset in presets) {
                        GlassChip(
                            selected = false,
                            label = "+${theme.currencySymbol}$preset",
                            accentColor = typeAccentColor,
                            onClick = {
                                val currentWhole = (MoneyUtils.parseToPaisa(amountText) ?: 0L) / 100L
                                amountText = (currentWhole + preset).toString()
                                validationError = null
                            },
                            modifier = Modifier.testTag("quick_amount_$preset")
                        )
                    }
                }
            }

            // 5. Product / Description Input + Smart Suggestions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val descLabel = when (selectedType) {
                    TransactionType.SHOP_DUE -> if (isBn) "পণ্য / বিবরণ (যেমন: Rice, Oil)" else "Product / Description (e.g. Rice, Oil)"
                    TransactionType.EXPENSE -> if (isBn) "কী বাবদ খরচ? (যেমন: Lunch)" else "Description / Note (e.g. Lunch)"
                    TransactionType.INCOME -> if (isBn) "আয়ের উৎস / বিবরণ" else "Source / Description"
                    else -> strings.productOrDescLabel
                }
                OutlinedTextField(
                    value = productOrDescription,
                    onValueChange = { productOrDescription = it },
                    label = { Text(descLabel) },
                    singleLine = true,
                    colors = glassTextFieldColors(typeAccentColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_description")
                )

                val suggestions: List<String> = remember(selectedType, selectedShop, selectedCategory) {
                    when (selectedType) {
                        TransactionType.SHOP_DUE -> selectedShop?.recentProductList()
                            ?: listOf("Rice", "Oil", "Biscuits", "Sugar", "Lentils", "Flour")
                        TransactionType.EXPENSE -> when (selectedCategory?.name) {
                            "Food" -> listOf("Lunch", "Dinner", "Breakfast", "Groceries", "Tea & Snacks")
                            "Transport" -> listOf("Rickshaw", "Bus Fare", "CNG / Ride", "Fuel")
                            "Bills" -> listOf("Electricity Bill", "Internet Bill", "House Rent", "Gas Bill")
                            else -> listOf("Lunch", "Groceries", "Transport", "Medicine", "Bill")
                        }
                        TransactionType.SHOP_PAYMENT, TransactionType.RECEIVE_PAYMENT ->
                            listOf("Payment received", "Cash received", "bKash payment", "Partial settlement")
                        else -> emptyList()
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (sug in suggestions) {
                            val active = productOrDescription.equals(sug, ignoreCase = true)
                            GlassChip(
                                selected = active,
                                label = sug,
                                accentColor = typeAccentColor,
                                onClick = { productOrDescription = sug },
                                modifier = Modifier.testTag("suggestion_chip_${sug.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 6. Optional Due Date chips
            if (selectedType == TransactionType.LEND ||
                selectedType == TransactionType.BORROW ||
                selectedType == TransactionType.SHOP_DUE
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isBn) "ঐচ্ছিক পরিশোধের সময়সীমা (Due Date)" else "Optional Due Date Reminder",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(null to "None", 3 to "In 3 Days", 7 to "In 7 Days", 15 to "In 15 Days", 30 to "In 30 Days")
                        for ((days, label) in options) {
                            GlassChip(
                                selected = dueDaysOffset == days,
                                label = label,
                                accentColor = typeAccentColor,
                                onClick = { dueDaysOffset = days }
                            )
                        }
                    }
                }
            }

            val activeErrorText = validationError ?: liveProposalError
            if (activeErrorText != null) {
                GlassCard(
                    tintColor = FinanceExpense,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_validation_error_banner")
                ) {
                    Text(
                        text = activeErrorText,
                        color = FinanceExpense,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val paisa = MoneyUtils.parseToPaisa(amountText)
                    if (paisa == null || paisa <= 0L) {
                        validationError = "Please enter a valid amount greater than 0."
                        return@Button
                    }

                    val resolvedSId = if (selectedType == TransactionType.SHOP_DUE || selectedType == TransactionType.SHOP_PAYMENT) {
                        if (isCreatingNewShopInline) null else selectedShop?.id
                    } else null
                    val resolvedSName = if (selectedType == TransactionType.SHOP_DUE || selectedType == TransactionType.SHOP_PAYMENT) {
                        if (isCreatingNewShopInline || selectedShop == null) customShopName.trim() else selectedShop.name
                    } else null

                    if ((selectedType == TransactionType.SHOP_DUE || selectedType == TransactionType.SHOP_PAYMENT) &&
                        resolvedSName.isNullOrEmpty()
                    ) {
                        validationError = "Please select or enter a shop name."
                        return@Button
                    }

                    val isPersonType = selectedType in listOf(
                        TransactionType.LEND,
                        TransactionType.BORROW,
                        TransactionType.RECEIVE_PAYMENT,
                        TransactionType.MAKE_PAYMENT
                    )
                    val resolvedPId = if (isPersonType) {
                        if (isCreatingNewPersonInline) null else selectedPerson?.id
                    } else null
                    val resolvedPName = if (isPersonType) {
                        if (isCreatingNewPersonInline || selectedPerson == null) customPersonName.trim() else selectedPerson.name
                    } else null

                    if (isPersonType && resolvedPName.isNullOrEmpty()) {
                        validationError = "Please select or enter a person's name."
                        return@Button
                    }

                    val resolvedLId = selectedLoanId.takeIf {
                        selectedType == TransactionType.LOAN_REPAYMENT || selectedType == TransactionType.LOAN_PAYMENT
                    }

                    // Strict Business Logic Validation before saving!
                    val proposalErr = FinancialEngine.validateTransactionProposal(
                        snapshot = snapshot,
                        existingTx = editingTx,
                        type = selectedType,
                        amountPaisa = paisa,
                        shopId = resolvedSId,
                        shopName = resolvedSName,
                        personId = resolvedPId,
                        personName = resolvedPName,
                        loanId = resolvedLId,
                        currencySymbol = theme.currencySymbol
                    )
                    if (proposalErr != null) {
                        validationError = proposalErr
                        return@Button
                    }

                    val now = editingTx?.timestamp ?: System.currentTimeMillis()
                    val dueMillis = dueDaysOffset?.let { System.currentTimeMillis() + it * 24L * 3600_000L }
                        ?: editingTx?.dueDateMillis

                    onSave(
                        editingTx,
                        selectedType,
                        paisa,
                        now,
                        if (selectedType == TransactionType.INCOME || selectedType == TransactionType.EXPENSE) selectedCategory?.id else null,
                        if (selectedType == TransactionType.INCOME || selectedType == TransactionType.EXPENSE) selectedCategory?.name else null,
                        resolvedPId,
                        resolvedPName,
                        resolvedSId,
                        resolvedSName,
                        resolvedLId,
                        productOrDescription,
                        noteText,
                        dueMillis
                    )
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeAccentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.save,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDeleteConfirm && editingTx != null && onDeleteExisting != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            title = { Text("Delete Transaction?") },
            text = {
                Text("Deleting this transaction will automatically recalculate all affected balances, dues, and reports.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteExisting(editingTx.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpense),
                    modifier = Modifier.testTag("confirm_delete_tx_button")
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
