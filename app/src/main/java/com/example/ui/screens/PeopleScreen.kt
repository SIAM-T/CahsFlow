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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.example.data.local.PersonEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.PersonBalanceSummary
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceLoan
import com.example.ui.theme.FinancePayable
import com.example.ui.theme.FinanceReceivable
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.QuickEntryRequest
import com.example.util.MoneyUtils

@Composable
fun PeopleScreen(
    personSummaries: List<PersonBalanceSummary>,
    totalPeopleReceivablePaisa: Long,
    totalPeoplePayablePaisa: Long,
    selectedPersonId: String?,
    onSelectPerson: (String?) -> Unit,
    onOpenQuickEntry: (QuickEntryRequest) -> Unit,
    onSavePerson: (PersonEntity?, String, String, String, String) -> Unit,
    onDeletePerson: (String) -> Unit
) {
    val selectedSummary = remember(personSummaries, selectedPersonId) {
        personSummaries.find { it.person.id == selectedPersonId }
    }

    var showCreateOrEditPersonDialog by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }

    if (selectedSummary != null) {
        BackHandler {
            onSelectPerson(null)
        }
        PersonDetailView(
            summary = selectedSummary,
            onBack = { onSelectPerson(null) },
            onGiveMoney = {
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.LEND,
                        preselectedPersonId = selectedSummary.person.id
                    )
                )
            },
            onReceiveMoney = {
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.RECEIVE_PAYMENT,
                        preselectedPersonId = selectedSummary.person.id
                    )
                )
            },
            onBorrowMoney = {
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.BORROW,
                        preselectedPersonId = selectedSummary.person.id
                    )
                )
            },
            onRepayMoney = {
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.MAKE_PAYMENT,
                        preselectedPersonId = selectedSummary.person.id
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
            onEditPerson = {
                editingPerson = selectedSummary.person
                showCreateOrEditPersonDialog = true
            },
            onDeletePerson = {
                onDeletePerson(selectedSummary.person.id)
            }
        )
    } else {
        PeopleListView(
            personSummaries = personSummaries,
            totalReceivablePaisa = totalPeopleReceivablePaisa,
            totalPayablePaisa = totalPeoplePayablePaisa,
            onSelectPerson = { onSelectPerson(it) },
            onCreatePerson = {
                editingPerson = null
                showCreateOrEditPersonDialog = true
            },
            onQuickGive = { personId ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.LEND,
                        preselectedPersonId = personId
                    )
                )
            },
            onQuickReceive = { personId ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.RECEIVE_PAYMENT,
                        preselectedPersonId = personId
                    )
                )
            },
            onQuickBorrow = { personId ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.BORROW,
                        preselectedPersonId = personId
                    )
                )
            },
            onQuickRepay = { personId ->
                onOpenQuickEntry(
                    QuickEntryRequest(
                        initialType = TransactionType.MAKE_PAYMENT,
                        preselectedPersonId = personId
                    )
                )
            }
        )
    }

    if (showCreateOrEditPersonDialog) {
        PersonFormDialog(
            existingPerson = editingPerson,
            onDismiss = { showCreateOrEditPersonDialog = false },
            onSave = { name, phone, note, colorHex ->
                onSavePerson(editingPerson, name, phone, note, colorHex)
                showCreateOrEditPersonDialog = false
            }
        )
    }
}

@Composable
private fun PeopleListView(
    personSummaries: List<PersonBalanceSummary>,
    totalReceivablePaisa: Long,
    totalPayablePaisa: Long,
    onSelectPerson: (String) -> Unit,
    onCreatePerson: () -> Unit,
    onQuickGive: (String) -> Unit,
    onQuickReceive: (String) -> Unit,
    onQuickBorrow: (String) -> Unit,
    onQuickRepay: (String) -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("people_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header + New Person button
        item(key = "people_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.navPeople,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isBn) "পাওনা (Receivable) এবং দেনা (Payable) আলাদা হিসাব" else "Track money lent (Receivable) & borrowed (Payable)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onCreatePerson,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("create_person_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "নতুন ব্যক্তি" else "Add Person")
                }
            }
        }

        // 2. Receivable vs Payable Overview Cards (Clearly Distinguished per Section 7)
        item(key = "people_summary_cards") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    tintColor = FinanceReceivable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.receivable,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBn) "মানুষের কাছে পাবো" else "Others owe me",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = MoneyUtils.formatPaisa(totalReceivablePaisa, theme.currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FinanceReceivable,
                        modifier = Modifier.testTag("people_total_receivable")
                    )
                }

                GlassCard(
                    tintColor = FinancePayable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.payable,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBn) "মানুষকে দিতে হবে" else "I owe others",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = MoneyUtils.formatPaisa(totalPayablePaisa, theme.currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FinancePayable,
                        modifier = Modifier.testTag("people_total_payable")
                    )
                }
            }
        }

        // 3. People Cards or Empty State
        if (personSummaries.isEmpty()) {
            item(key = "people_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.Person,
                    title = strings.noPeopleYet,
                    subtitle = strings.addFirstPerson,
                    actionLabel = "+ Add Person",
                    actionTestTag = "empty_create_person_button",
                    onAction = onCreatePerson
                )
            }
        } else {
            items(items = personSummaries, key = { it.person.id }) { summary ->
                val avatarColor = parseHexColor(summary.person.avatarColorHex, FinanceReceivable)
                GlassCard(
                    onClick = { onSelectPerson(summary.person.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("person_card_${summary.person.name.lowercase().replace(" ", "_")}")
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
                                    .background(avatarColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = summary.person.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = avatarColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = summary.person.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                val sub = listOf(summary.person.phone, summary.person.note)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                if (sub.isNotEmpty()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Distinct Receivable & Payable badges for this person
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FinanceReceivable.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, FinanceReceivable.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${summary.person.name} ${strings.owesMe}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyUtils.formatPaisa(summary.directReceivablePaisa, theme.currencySymbol),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FinanceReceivable
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FinancePayable.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, FinancePayable.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${strings.iOwe} ${summary.person.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyUtils.formatPaisa(summary.directPayablePaisa, theme.currencySymbol),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FinancePayable
                                )
                            }
                        }
                    }

                    if (summary.loanReceivablePaisa > 0L || summary.loanPayablePaisa > 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active Loan Balance: Lent ${
                                MoneyUtils.formatPaisa(summary.loanReceivablePaisa, theme.currencySymbol)
                            } • Borrowed ${
                                MoneyUtils.formatPaisa(summary.loanPayablePaisa, theme.currencySymbol)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinanceLoan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Quick Action Buttons on Person Card: Give, Receive, Borrow, Repay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onQuickGive(summary.person.id) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Give", style = MaterialTheme.typography.labelSmall, color = FinanceReceivable)
                        }
                        OutlinedButton(
                            onClick = { onQuickReceive(summary.person.id) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Receive", style = MaterialTheme.typography.labelSmall, color = FinanceIncome)
                        }
                        OutlinedButton(
                            onClick = { onQuickBorrow(summary.person.id) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Borrow", style = MaterialTheme.typography.labelSmall, color = FinancePayable)
                        }
                        OutlinedButton(
                            onClick = { onQuickRepay(summary.person.id) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Repay", style = MaterialTheme.typography.labelSmall, color = FinanceExpense)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonDetailView(
    summary: PersonBalanceSummary,
    onBack: () -> Unit,
    onGiveMoney: () -> Unit,
    onReceiveMoney: () -> Unit,
    onBorrowMoney: () -> Unit,
    onRepayMoney: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onEditPerson: () -> Unit,
    onDeletePerson: () -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("person_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Bar
        item(key = "person_detail_topbar") {
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
                        modifier = Modifier.testTag("person_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = summary.person.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (summary.person.phone.isNotBlank() || summary.person.note.isNotBlank()) {
                            Text(
                                text = listOf(summary.person.phone, summary.person.note)
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
                        onClick = onEditPerson,
                        modifier = Modifier.testTag("edit_person_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit)
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_person_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = FinanceExpense)
                    }
                }
            }
        }

        // 2. Dual Balance Overview (Receivable & Payable + Loan Balance) & 4 Action Buttons (Section 43)
        item(key = "person_detail_balances") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Receivable Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FinanceReceivable.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, FinanceReceivable.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = strings.receivable,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${summary.person.name} owes me",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = MoneyUtils.formatPaisa(summary.directReceivablePaisa, theme.currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FinanceReceivable,
                                modifier = Modifier.testTag("person_detail_receivable")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lent: ${MoneyUtils.formatPaisa(summary.lentPaisa, theme.currencySymbol)} • Recv: ${MoneyUtils.formatPaisa(summary.receivedPaymentPaisa, theme.currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Payable Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FinancePayable.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, FinancePayable.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = strings.payable,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "I owe ${summary.person.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = MoneyUtils.formatPaisa(summary.directPayablePaisa, theme.currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FinancePayable,
                                modifier = Modifier.testTag("person_detail_payable")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Borrowed: ${MoneyUtils.formatPaisa(summary.borrowedPaisa, theme.currencySymbol)} • Paid: ${MoneyUtils.formatPaisa(summary.madePaymentPaisa, theme.currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (summary.loanReceivablePaisa > 0L || summary.loanPayablePaisa > 0L) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FinanceLoan.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Formal Loans with ${summary.person.name}:",
                                style = MaterialTheme.typography.labelMedium,
                                color = FinanceLoan
                            )
                            Text(
                                text = "Lent ${MoneyUtils.formatPaisa(summary.loanReceivablePaisa, theme.currencySymbol)} / Borrowed ${MoneyUtils.formatPaisa(summary.loanPayablePaisa, theme.currencySymbol)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = FinanceLoan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Quick Person Action Buttons: Give Money, Receive Money, Borrow, Repay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onGiveMoney,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinanceReceivable,
                            contentColor = Color(0xFF042F2E)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("person_detail_give_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "টাকা দিন" else "Give Money", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReceiveMoney,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinanceIncome,
                            contentColor = Color(0xFF042F2E)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("person_detail_receive_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "টাকা আদায়" else "Receive Money", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBorrowMoney,
                        border = BorderStroke(1.dp, FinancePayable.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("person_detail_borrow_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = FinancePayable, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.borrow, color = FinancePayable, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRepayMoney,
                        border = BorderStroke(1.dp, FinanceExpense.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("person_detail_repay_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = FinanceExpense, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.repay, color = FinanceExpense, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Complete History
        item(key = "person_history_header") {
            Text(
                text = if (isBn) "লেনদেনের সম্পূর্ণ ইতিহাস" else "Complete Transaction History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (summary.transactions.isEmpty()) {
            item(key = "person_history_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.Person,
                    title = strings.noTransactionsYet,
                    subtitle = "Use the buttons above to record money given, received, borrowed, or repaid.",
                    actionLabel = "Give Money",
                    onAction = onGiveMoney
                )
            }
        } else {
            item(key = "person_history_card") {
                GlassCard(
                    contentPadding = PaddingValues(12.dp)
                ) {
                    summary.transactions.forEachIndexed { idx, tx ->
                        TransactionRowItem(
                            tx = tx,
                            currencySymbol = theme.currencySymbol,
                            onClick = { onEditTransaction(tx) }
                        )
                        if (idx < summary.transactions.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${summary.person.name}?") },
            text = { Text("This will remove ${summary.person.name}'s profile from People.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePerson()
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
private fun PersonFormDialog(
    existingPerson: PersonEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember(existingPerson) { mutableStateOf(existingPerson?.name ?: "") }
    var phone by remember(existingPerson) { mutableStateOf(existingPerson?.phone ?: "") }
    var note by remember(existingPerson) { mutableStateOf(existingPerson?.note ?: "") }
    var colorHex by remember(existingPerson) { mutableStateOf(existingPerson?.avatarColorHex ?: "#06B6D4") }

    val colorChoices = listOf("#06B6D4", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899", "#3B82F6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingPerson == null) "Add Person" else "Edit Person")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name * (e.g. Rahim, Karim)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_person_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Profile Color",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (hex in colorChoices) {
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex.equals(hex, ignoreCase = true)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
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
                        onSave(name, phone, note, colorHex)
                    }
                },
                modifier = Modifier.testTag("dialog_save_person_button")
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
