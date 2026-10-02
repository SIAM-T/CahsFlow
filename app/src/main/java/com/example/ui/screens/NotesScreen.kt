package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NoteChecklistItem
import com.example.data.local.NotePriority
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.SmartNoteEntity
import com.example.data.local.TransactionType
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.glassTextFieldColors
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinancePayable
import com.example.ui.theme.FinanceReceivable
import com.example.ui.theme.FinanceShopDue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor
import com.example.util.MoneyUtils
import com.example.util.NoteNotificationHelper
import java.util.UUID

enum class NoteFilterTab(val labelEn: String, val labelBn: String) {
    ALL("All Notes", "সব নোট"),
    PINNED("Pinned 📌", "পিন করা 📌"),
    CHECKLISTS("To-Do Lists ☑", "চেকলিস্ট ☑"),
    LIVE_NOTIF("Live in Status Bar 🔔", "স্ট্যাটাস বারে লাইভ 🔔"),
    REMINDERS("Reminders ⏰", "রিমাইন্ডার ⏰"),
    URGENT("Urgent 🔥", "জরুরি 🔥"),
    ARCHIVED("Archived 🗄", "আর্কাইভ 🗄")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    notes: List<SmartNoteEntity>,
    shops: List<ShopEntity>,
    people: List<PersonEntity>,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    onSaveNote: (
        existingNote: SmartNoteEntity?,
        title: String,
        content: String,
        checklistItems: List<NoteChecklistItem>,
        colorHex: String,
        priority: String,
        labelsCsv: String,
        isPinned: Boolean,
        isPinnedToNotification: Boolean,
        reminderMillis: Long?,
        repeatInterval: String,
        linkedTransactionType: String?,
        linkedEntityId: String?,
        linkedEntityName: String?,
        targetBudgetPaisa: Long,
        fireRealtimeAlertNow: Boolean
    ) -> Unit,
    onToggleChecklistItem: (SmartNoteEntity, String) -> Unit,
    onTogglePin: (SmartNoteEntity) -> Unit,
    onToggleArchive: (SmartNoteEntity) -> Unit,
    onToggleLiveStatusBarPin: (SmartNoteEntity) -> Unit,
    onTriggerInstantAlert: (SmartNoteEntity) -> Unit,
    onConvertNoteToTransaction: (SmartNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    if (showBackButton && onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    val theme = LocalHisabTheme.current
    val isBn = theme.languageCode == "bn"

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(NoteFilterTab.ALL) }
    var editingNote by remember { mutableStateOf<SmartNoteEntity?>(null) }
    var showNoteEditorSheet by remember { mutableStateOf(false) }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingNotificationAction?.invoke()
        pendingNotificationAction = null
    }

    fun runWithNotificationPermission(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NoteNotificationHelper.hasNotificationPermission(context)
        ) {
            pendingNotificationAction = block
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            block()
        }
    }

    val activeNotes = remember(notes) { notes.filter { !it.isArchived } }
    val totalPlannedBudgetPaisa = remember(activeNotes) {
        activeNotes.sumOf { it.effectiveTotalPaisa() }
    }
    val totalPendingTasks = remember(activeNotes) {
        activeNotes.sumOf { n -> n.checklistItems().count { !it.isChecked } }
    }
    val livePinnedCount = remember(activeNotes) {
        activeNotes.count { it.isPinnedToNotification }
    }

    val filteredNotes = remember(notes, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        notes.filter { note ->
            val tabMatches = when (selectedFilter) {
                NoteFilterTab.ALL -> !note.isArchived
                NoteFilterTab.PINNED -> !note.isArchived && note.isPinned
                NoteFilterTab.CHECKLISTS -> !note.isArchived && note.checklistItems().isNotEmpty()
                NoteFilterTab.LIVE_NOTIF -> !note.isArchived && note.isPinnedToNotification
                NoteFilterTab.REMINDERS -> !note.isArchived && note.reminderMillis != null
                NoteFilterTab.URGENT -> !note.isArchived && (note.priority == NotePriority.URGENT.name || note.priority == NotePriority.HIGH.name)
                NoteFilterTab.ARCHIVED -> note.isArchived
            }
            if (!tabMatches) return@filter false

            if (q.isNotEmpty()) {
                val inTitle = note.title.lowercase().contains(q)
                val inBody = note.content.lowercase().contains(q)
                val inLabels = note.labelsCsv.lowercase().contains(q)
                val inItems = note.checklistItems().any { it.text.lowercase().contains(q) }
                inTitle || inBody || inLabels || inItems
            } else {
                true
            }
        }.sortedWith(
            compareByDescending<SmartNoteEntity> { it.isPinned }
                .thenByDescending { it.priority == NotePriority.URGENT.name }
                .thenByDescending { it.updatedAt }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notes_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Create Note Button
        item(key = "notes_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBackButton && onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("notes_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Column {
                        Text(
                            text = if (isBn) "স্মার্ট নোট ও চেকলিস্ট" else "Smart Notes & To-Do",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "বাজেট চেকলিস্ট, রিয়েল-টাইম নোটিফিকেশন ও ১-ট্যাপ লেনদেন"
                            else "Financial checklists, live status-bar pin & real-time alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        editingNote = null
                        showNoteEditorSheet = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accentColor,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.testTag("create_smart_note_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "নতুন নোট" else "New Note",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Live Summary Metrics Card
        item(key = "notes_summary_hero") {
            GlassCard(
                tintColor = theme.accentColor,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_summary_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBn) "পরিকল্পিত নোট বাজেট" else "Planned Checklist Budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MoneyUtils.formatPaisa(totalPlannedBudgetPaisa, theme.currencySymbol),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = theme.accentColor,
                            modifier = Modifier.testTag("notes_total_planned_budget")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${activeNotes.size}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalPendingTasks",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = FinanceShopDue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$livePinnedCount",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = FinanceReceivable,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live Pin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. 1-Tap Smart Templates Strip (Faster & Smarter than Google Keep!)
        item(key = "notes_quick_templates") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isBn) "১-ট্যাপ স্মার্ট টেমপ্লেট" else "1-Tap Smart Financial Templates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassChip(
                        selected = false,
                        label = "🛒 Bazar Checklist",
                        accentColor = FinanceIncome,
                        onClick = {
                            runWithNotificationPermission {
                                onSaveNote(
                                    null,
                                    "Weekly Bazar & Groceries",
                                    "Check items as you buy and tap 'Record in Hisab' when finished",
                                    listOf(
                                        NoteChecklistItem("b1", "Rice & Lentils", false, 850_00L),
                                        NoteChecklistItem("b2", "Vegetables & Fish", false, 1200_00L),
                                        NoteChecklistItem("b3", "Cooking Oil & Spices", false, 650_00L)
                                    ),
                                    "#10B981",
                                    NotePriority.HIGH.name,
                                    "Bazar,Grocery",
                                    true,
                                    true,
                                    System.currentTimeMillis() + 3600_000L,
                                    "NONE",
                                    TransactionType.EXPENSE.name,
                                    null,
                                    null,
                                    2700_00L,
                                    true
                                )
                            }
                        },
                        modifier = Modifier.testTag("template_bazar_chip")
                    )

                    GlassChip(
                        selected = false,
                        label = "🧾 Monthly Bills Todo",
                        accentColor = FinanceExpense,
                        onClick = {
                            runWithNotificationPermission {
                                onSaveNote(
                                    null,
                                    "Monthly Utility & Rent Bills",
                                    "Pay before the 10th of the month",
                                    listOf(
                                        NoteChecklistItem("m1", "Electricity Bill", false, 1800_00L),
                                        NoteChecklistItem("m2", "Home Internet / WiFi", false, 1000_00L),
                                        NoteChecklistItem("m3", "Gas & Water Bill", false, 1200_00L)
                                    ),
                                    "#F43F5E",
                                    NotePriority.URGENT.name,
                                    "Bills,Monthly",
                                    true,
                                    false,
                                    System.currentTimeMillis() + 7200_000L,
                                    "MONTHLY",
                                    TransactionType.EXPENSE.name,
                                    null,
                                    null,
                                    4000_00L,
                                    true
                                )
                            }
                        },
                        modifier = Modifier.testTag("template_bills_chip")
                    )

                    GlassChip(
                        selected = false,
                        label = "🏪 Shop Restock Order",
                        accentColor = FinanceShopDue,
                        onClick = {
                            val firstShop = shops.firstOrNull()
                            runWithNotificationPermission {
                                onSaveNote(
                                    null,
                                    "${firstShop?.name ?: "Shop"} Supply Order",
                                    "Deliver goods and convert to Shop Due in 1 tap",
                                    listOf(
                                        NoteChecklistItem("s1", "Miniket Rice 50kg", false, 3400_00L),
                                        NoteChecklistItem("s2", "Soybean Oil Carton", false, 1900_00L)
                                    ),
                                    "#F59E0B",
                                    NotePriority.HIGH.name,
                                    "Shop,Supply",
                                    false,
                                    false,
                                    null,
                                    "NONE",
                                    TransactionType.SHOP_DUE.name,
                                    firstShop?.id,
                                    firstShop?.name,
                                    5300_00L,
                                    true
                                )
                            }
                        },
                        modifier = Modifier.testTag("template_shop_order_chip")
                    )
                }
            }
        }

        // 4. Search & Filter Chips
        item(key = "notes_search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(if (isBn) "নোট, চেকলিস্ট বা ট্যাগ খুঁজুন..." else "Search notes, checklist items, labels...")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    colors = glassTextFieldColors(theme.accentColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (tab in NoteFilterTab.entries) {
                        GlassChip(
                            selected = selectedFilter == tab,
                            label = if (isBn) tab.labelBn else tab.labelEn,
                            accentColor = theme.accentColor,
                            onClick = { selectedFilter = tab },
                            modifier = Modifier.testTag("note_filter_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 5. Notes List or Empty State
        if (filteredNotes.isEmpty()) {
            item(key = "notes_empty") {
                FriendlyEmptyState(
                    icon = Icons.Default.Checklist,
                    title = if (isBn) "কোনো স্মার্ট নোট পাওয়া যায়নি" else "No smart notes yet",
                    subtitle = if (isBn) "বাজেট চেকলিস্ট, রিমাইন্ডার এবং স্ট্যাটাস বার পিনসহ নোট তৈরি করুন"
                    else "Create interactive financial checklists, pin live tasks to your notification bar, and convert notes to Hisab transactions in 1 tap.",
                    actionLabel = "+ Create Smart Note",
                    actionTestTag = "empty_create_note_button",
                    onAction = {
                        editingNote = null
                        showNoteEditorSheet = true
                    }
                )
            }
        } else {
            items(items = filteredNotes, key = { it.id }) { note ->
                SmartNoteGlassCard(
                    note = note,
                    currencySymbol = theme.currencySymbol,
                    isBn = isBn,
                    onToggleItem = { itemId ->
                        runWithNotificationPermission {
                            onToggleChecklistItem(note, itemId)
                        }
                    },
                    onTogglePin = { onTogglePin(note) },
                    onToggleLiveStatusBar = {
                        runWithNotificationPermission {
                            onToggleLiveStatusBarPin(note)
                        }
                    },
                    onTriggerRealtimeAlert = {
                        runWithNotificationPermission {
                            onTriggerInstantAlert(note)
                        }
                    },
                    onConvertToTransaction = { onConvertNoteToTransaction(note) },
                    onEdit = {
                        editingNote = note
                        showNoteEditorSheet = true
                    },
                    onToggleArchive = { onToggleArchive(note) },
                    onDelete = { onDeleteNote(note.id) }
                )
            }
        }
    }

    if (showNoteEditorSheet) {
        SmartNoteEditorBottomSheet(
            existingNote = editingNote,
            currencySymbol = theme.currencySymbol,
            isBn = isBn,
            onDismiss = { showNoteEditorSheet = false },
            onSave = { existing, title, content, items, colorHex, priority, labelsCsv, isPinned, isPinnedNotif, reminderMillis, repeat, linkedType, linkedId, linkedName, targetPaisa, fireNow ->
                showNoteEditorSheet = false
                runWithNotificationPermission {
                    onSaveNote(
                        existing,
                        title,
                        content,
                        items,
                        colorHex,
                        priority,
                        labelsCsv,
                        isPinned,
                        isPinnedNotif,
                        reminderMillis,
                        repeat,
                        linkedType,
                        linkedId,
                        linkedName,
                        targetPaisa,
                        fireNow
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartNoteGlassCard(
    note: SmartNoteEntity,
    currencySymbol: String,
    isBn: Boolean,
    onToggleItem: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleLiveStatusBar: () -> Unit,
    onTriggerRealtimeAlert: () -> Unit,
    onConvertToTransaction: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val noteColor = parseHexColor(note.colorHex, Color(0xFF10B981))
    val priorityEnum = remember(note.priority) {
        NotePriority.entries.find { it.name == note.priority } ?: NotePriority.NORMAL
    }
    val priorityColor = parseHexColor(priorityEnum.colorHex, noteColor)
    val items = remember(note.checklistJson) { note.checklistItems() }
    val checkedCount = items.count { it.isChecked }
    val totalBudget = note.effectiveTotalPaisa()
    val checkedBudget = note.checklistCheckedPaisa()
    val labels = remember(note.labelsCsv) { note.labelsList() }

    GlassCard(
        tintColor = noteColor,
        cornerRadius = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_note_card_${note.id}")
    ) {
        // Top Row: Priority Badge, Live Status-Bar Badge & Pin/Edit Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = priorityColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (isBn) priorityEnum.labelBn else priorityEnum.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (note.isPinnedToNotification) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FinanceReceivable.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, FinanceReceivable.copy(alpha = 0.50f))
                    ) {
                        Text(
                            text = "🔔 Live in Status Bar",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinanceReceivable,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.testTag("pin_note_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (note.isPinned) noteColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_note_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_note_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = FinanceExpense,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Note Title & Total Amount Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (totalBudget > 0L) {
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyUtils.formatPaisa(totalBudget, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = noteColor
                    )
                    if (items.isNotEmpty() && checkedBudget > 0L) {
                        Text(
                            text = "Done: ${MoneyUtils.formatPaisa(checkedBudget, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = FinanceIncome
                        )
                    }
                }
            }
        }

        // Interactive Checklist Items
        if (items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val progress = (checkedCount.toFloat() / items.size.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checklist ($checkedCount/${items.size} completed)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = noteColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.03f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(noteColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (item in items) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onToggleItem(item.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("checklist_item_${note.id}_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (item.isChecked) "Checked" else "Unchecked",
                                tint = if (item.isChecked) FinanceIncome else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (item.isChecked) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.amountPaisa > 0L) {
                            Text(
                                text = MoneyUtils.formatPaisa(item.amountPaisa, currencySymbol),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (item.isChecked) FinanceIncome else noteColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Labels & Scheduled Reminder Info
        if (labels.isNotEmpty() || note.reminderMillis != null) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (note.reminderMillis != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FinanceShopDue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, FinanceShopDue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = FinanceShopDue,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = MoneyUtils.formatDateTime(note.reminderMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = FinanceShopDue
                            )
                        }
                    }
                }
                for (lbl in labels) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Text(
                            text = "#$lbl",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
        Spacer(modifier = Modifier.height(10.dp))

        // Smart Actions Row: Notify Now 🔔, Live Status Bar Pin 📌, Convert to Hisab Transaction 💸, Archive
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassChip(
                selected = false,
                label = if (isBn) "🔔 এখনই নোটিফিকেশন" else "🔔 Notify Now",
                accentColor = FinanceShopDue,
                leadingIcon = Icons.Default.NotificationsActive,
                onClick = onTriggerRealtimeAlert,
                modifier = Modifier.testTag("notify_now_note_${note.id}")
            )

            GlassChip(
                selected = note.isPinnedToNotification,
                label = if (note.isPinnedToNotification) "📌 Unpin Status Bar" else "📌 Pin to Status Bar",
                accentColor = FinanceReceivable,
                leadingIcon = Icons.Default.PushPin,
                onClick = onToggleLiveStatusBar,
                modifier = Modifier.testTag("pin_status_bar_note_${note.id}")
            )

            GlassChip(
                selected = false,
                label = if (isBn) "হিসাবে যোগ করুন" else "Record in Hisab",
                accentColor = noteColor,
                leadingIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                onClick = onConvertToTransaction,
                modifier = Modifier.testTag("convert_note_to_tx_${note.id}")
            )

            GlassChip(
                selected = note.isArchived,
                label = if (note.isArchived) "Unarchive" else "Archive",
                accentColor = FinancePayable,
                leadingIcon = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                onClick = onToggleArchive
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SmartNoteEditorBottomSheet(
    existingNote: SmartNoteEntity?,
    currencySymbol: String,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingNote: SmartNoteEntity?,
        title: String,
        content: String,
        checklistItems: List<NoteChecklistItem>,
        colorHex: String,
        priority: String,
        labelsCsv: String,
        isPinned: Boolean,
        isPinnedToNotification: Boolean,
        reminderMillis: Long?,
        repeatInterval: String,
        linkedTransactionType: String?,
        linkedEntityId: String?,
        linkedEntityName: String?,
        targetBudgetPaisa: Long,
        fireRealtimeAlertNow: Boolean
    ) -> Unit
) {
    val theme = LocalHisabTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(existingNote) { mutableStateOf(existingNote?.title ?: "") }
    var content by remember(existingNote) { mutableStateOf(existingNote?.content ?: "") }
    var colorHex by remember(existingNote) { mutableStateOf(existingNote?.colorHex ?: "#10B981") }
    var priority by remember(existingNote) { mutableStateOf(existingNote?.priority ?: NotePriority.NORMAL.name) }
    var labelsCsv by remember(existingNote) { mutableStateOf(existingNote?.labelsCsv ?: "") }
    var isPinned by remember(existingNote) { mutableStateOf(existingNote?.isPinned ?: true) }
    var isPinnedToNotification by remember(existingNote) { mutableStateOf(existingNote?.isPinnedToNotification ?: false) }
    var fireRealtimeAlertOnSave by remember(existingNote) { mutableStateOf(true) }
    var linkedTxType by remember(existingNote) {
        mutableStateOf(existingNote?.linkedTransactionType ?: TransactionType.EXPENSE.name)
    }
    var targetBudgetInput by remember(existingNote) {
        mutableStateOf(
            existingNote?.targetBudgetPaisa?.takeIf { it > 0L }?.let { MoneyUtils.paisaToEditableString(it) } ?: ""
        )
    }

    val checklistItems = remember(existingNote) {
        mutableStateListOf<NoteChecklistItem>().apply {
            addAll(existingNote?.checklistItems() ?: emptyList())
        }
    }
    var newItemText by remember { mutableStateOf("") }
    var newItemPriceText by remember { mutableStateOf("") }

    var reminderMinutesOffset by remember { mutableStateOf<Int?>(null) }
    var repeatInterval by remember(existingNote) { mutableStateOf(existingNote?.repeatInterval ?: "NONE") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectedAccent = parseHexColor(colorHex, theme.accentColor)
    val checklistSumPaisa = checklistItems.sumOf { it.amountPaisa }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (theme.isDark) Color(0xFF0B1324).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingNote == null) {
                        if (isBn) "নতুন স্মার্ট নোট ও চেকলিস্ট" else "New Smart Note & To-Do"
                    } else {
                        if (isBn) "স্মার্ট নোট সম্পাদনা" else "Edit Smart Note"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss
                )
            }

            // Title & Note Content
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = { Text(if (isBn) "নোটের শিরোনাম *" else "Note / Task Title *") },
                placeholder = { Text("e.g. Monthly Bazar List / Collect Rahman Store Due") },
                singleLine = true,
                colors = glassTextFieldColors(selectedAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_note_title")
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(if (isBn) "বিস্তারিত নোট (ঐচ্ছিক)" else "Detailed Note / Memo (Optional)") },
                minLines = 2,
                maxLines = 4,
                colors = glassTextFieldColors(selectedAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_note_content")
            )

            // Interactive Financial Checklist Builder
            GlassCard(
                tintColor = selectedAccent,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "বাজেট চেকলিস্ট আইটেম" else "Financial To-Do Checklist",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (checklistSumPaisa > 0L) {
                        Text(
                            text = "Sum: ${MoneyUtils.formatPaisa(checklistSumPaisa, currencySymbol)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = selectedAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Task / Item") },
                        placeholder = { Text("Rice 10kg") },
                        singleLine = true,
                        colors = glassTextFieldColors(selectedAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("input_checklist_item_text")
                    )
                    OutlinedTextField(
                        value = newItemPriceText,
                        onValueChange = { newItemPriceText = it },
                        label = { Text(currencySymbol) },
                        placeholder = { Text("650") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = glassTextFieldColors(selectedAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_checklist_item_amount")
                    )
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add Checklist Item",
                        tint = selectedAccent,
                        onClick = {
                            val cleanItem = newItemText.trim()
                            if (cleanItem.isNotEmpty()) {
                                val pricePaisa = MoneyUtils.parseToPaisa(newItemPriceText) ?: 0L
                                checklistItems.add(
                                    NoteChecklistItem(
                                        id = "chk_${UUID.randomUUID().toString().take(6)}",
                                        text = cleanItem,
                                        isChecked = false,
                                        amountPaisa = pricePaisa
                                    )
                                )
                                newItemText = ""
                                newItemPriceText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_checklist_item_button")
                    )
                }

                if (checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        checklistItems.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            checklistItems[idx] = item.copy(isChecked = checked)
                                        }
                                    )
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (item.amountPaisa > 0L) {
                                    Text(
                                        text = MoneyUtils.formatPaisa(item.amountPaisa, currencySymbol),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = selectedAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { checklistItems.removeAt(idx) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Item",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Priority & Color Palette
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isBn) "অগ্রাধিকার (Priority)" else "Priority Level",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (p in NotePriority.entries) {
                        val pColor = parseHexColor(p.colorHex)
                        GlassChip(
                            selected = priority == p.name,
                            label = if (isBn) p.labelBn else p.labelEn,
                            accentColor = pColor,
                            onClick = { priority = p.name },
                            modifier = Modifier.testTag("priority_chip_${p.name.lowercase()}")
                        )
                    }
                }
            }

            // Color Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Note Color:", style = MaterialTheme.typography.labelMedium)
                val colors = listOf("#10B981", "#06B6D4", "#F59E0B", "#F43F5E", "#8B5CF6", "#6366F1")
                for (hex in colors) {
                    val c = parseHexColor(hex)
                    val selected = colorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Linked Hisab Transaction Type & Optional Target Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = targetBudgetInput,
                    onValueChange = { targetBudgetInput = it },
                    label = { Text("Target Budget ($currencySymbol)") },
                    placeholder = { Text("Auto or 2000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = glassTextFieldColors(selectedAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_note_budget")
                )
                OutlinedTextField(
                    value = labelsCsv,
                    onValueChange = { labelsCsv = it },
                    label = { Text("Labels (comma separated)") },
                    placeholder = { Text("Bazar, Urgent") },
                    singleLine = true,
                    colors = glassTextFieldColors(selectedAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_note_labels")
                )
            }

            // Linked Transaction Type for 1-Tap Conversion
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "1-Tap Convert to Transaction Type:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val txChoices = listOf(
                        TransactionType.EXPENSE,
                        TransactionType.INCOME,
                        TransactionType.SHOP_DUE,
                        TransactionType.LEND,
                        TransactionType.BORROW
                    )
                    for (t in txChoices) {
                        GlassChip(
                            selected = linkedTxType == t.name,
                            label = if (isBn) t.labelBn else t.labelEn,
                            accentColor = selectedAccent,
                            onClick = { linkedTxType = t.name }
                        )
                    }
                }
            }

            // Real-Time Notification & Scheduled Alarm Studio
            GlassCard(
                tintColor = FinanceReceivable,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔔 Real-Time Notification & Reminder Studio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pin Live to Status Bar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keep checklist & progress bar visible in your notification shade",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPinnedToNotification,
                        onCheckedChange = { isPinnedToNotification = it },
                        modifier = Modifier.testTag("switch_pin_to_notification")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fire Instant Real-Time Notification",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Triggers a live heads-up notification immediately on save",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = fireRealtimeAlertOnSave,
                        onCheckedChange = { fireRealtimeAlertOnSave = it },
                        modifier = Modifier.testTag("switch_fire_realtime_alert")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Schedule Alarm Reminder:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reminderPresets = listOf(
                        null to "No Alarm",
                        1 to "In 1 Min",
                        15 to "In 15 Mins",
                        60 to "In 1 Hour",
                        240 to "In 4 Hours",
                        1440 to "Tomorrow"
                    )
                    for ((mins, label) in reminderPresets) {
                        GlassChip(
                            selected = reminderMinutesOffset == mins,
                            label = label,
                            accentColor = FinanceShopDue,
                            onClick = { reminderMinutesOffset = mins }
                        )
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = FinanceExpense,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    // Auto-add any typed checklist item that user didn't press '+' on yet
                    if (newItemText.trim().isNotEmpty()) {
                        val pricePaisa = MoneyUtils.parseToPaisa(newItemPriceText) ?: 0L
                        checklistItems.add(
                            NoteChecklistItem(
                                id = "chk_${UUID.randomUUID().toString().take(6)}",
                                text = newItemText.trim(),
                                isChecked = false,
                                amountPaisa = pricePaisa
                            )
                        )
                        newItemText = ""
                        newItemPriceText = ""
                    }

                    if (title.isBlank() && checklistItems.isEmpty() && content.isBlank()) {
                        validationError = "Please enter a note title or add at least one checklist item."
                        return@Button
                    }

                    val manualBudget = MoneyUtils.parseToPaisa(targetBudgetInput) ?: 0L
                    val reminderMillis = reminderMinutesOffset?.let {
                        System.currentTimeMillis() + it * 60_000L
                    } ?: existingNote?.reminderMillis

                    onSave(
                        existingNote,
                        title,
                        content,
                        checklistItems.toList(),
                        colorHex,
                        priority,
                        labelsCsv,
                        isPinned,
                        isPinnedToNotification,
                        reminderMillis,
                        repeatInterval,
                        linkedTxType,
                        existingNote?.linkedEntityId,
                        existingNote?.linkedEntityName,
                        manualBudget,
                        fireRealtimeAlertOnSave
                    )
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedAccent,
                    contentColor = Color(0xFF042F2E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_smart_note_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) "স্মার্ট নোট সংরক্ষণ করুন" else "Save Smart Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
