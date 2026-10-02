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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NoteChecklistItem
import com.example.data.local.NotePriority
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.SmartNoteEntity
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.glassTextFieldColors
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceLoan
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor
import com.example.util.MoneyUtils
import java.util.Calendar

enum class TodoFilterTab {
    ALL,
    ACTIVE,
    COMPLETED
}

@Composable
fun NotesScreen(
    notes: List<SmartNoteEntity>,
    shops: List<ShopEntity> = emptyList(),
    people: List<PersonEntity> = emptyList(),
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
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
    onToggleChecklistItem: (SmartNoteEntity, String) -> Unit = { _, _ -> },
    onTogglePin: (SmartNoteEntity) -> Unit = {},
    onToggleArchive: (SmartNoteEntity) -> Unit = {},
    onToggleLiveStatusBarPin: (SmartNoteEntity) -> Unit = {},
    onTriggerInstantAlert: (SmartNoteEntity) -> Unit = {},
    onConvertNoteToTransaction: (SmartNoteEntity) -> Unit = {},
    onDeleteNote: (String) -> Unit
) {
    val theme = LocalHisabTheme.current
    val strings = LocalHisabStrings.current
    val isBn = theme.languageCode == "bn"
    val context = LocalContext.current

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* state handled by system */ }
    )

    var currentFilter by remember { mutableStateOf(TodoFilterTab.ALL) }
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(NotePriority.NORMAL) }
    var reminderForNote by remember { mutableStateOf<SmartNoteEntity?>(null) }
    var editingNote by remember { mutableStateOf<SmartNoteEntity?>(null) }
    var showClearCompletedConfirm by remember { mutableStateOf(false) }

    if (showBackButton) {
        BackHandler { onBack() }
    }

    val activeCount = remember(notes) { notes.count { !it.isCompleted } }
    val completedCount = remember(notes) { notes.count { it.isCompleted } }

    val filteredNotes = remember(notes, currentFilter) {
        val base = when (currentFilter) {
            TodoFilterTab.ALL -> notes
            TodoFilterTab.ACTIVE -> notes.filter { !it.isCompleted }
            TodoFilterTab.COMPLETED -> notes.filter { it.isCompleted }
        }
        base.sortedWith(
            compareByDescending<SmartNoteEntity> { it.isPinned }
                .thenBy { it.isCompleted }
                .thenByDescending { it.updatedAt }
        )
    }

    fun submitNewTask() {
        if (newTaskTitle.isNotBlank()) {
            val title = newTaskTitle.trim()
            val colorHex = when (selectedPriority) {
                NotePriority.URGENT, NotePriority.HIGH -> "#EF4444"
                NotePriority.NORMAL -> "#10B981"
                NotePriority.LOW -> "#06B6D4"
            }
            val checklist = listOf(
                NoteChecklistItem(
                    id = "item_${System.currentTimeMillis()}",
                    text = title,
                    isChecked = false
                )
            )
            onSaveNote(
                null,
                title,
                "",
                checklist,
                colorHex,
                selectedPriority.name,
                "",
                false,
                false,
                null,
                "NONE",
                null,
                null,
                null,
                0L,
                false
            )
            newTaskTitle = ""
        }
    }

    fun toggleTaskCompletion(note: SmartNoteEntity) {
        val newDone = !note.isCompleted
        val existingItems = note.checklistItems()
        val updatedItems = if (existingItems.isNotEmpty()) {
            existingItems.map { it.copy(isChecked = newDone) }
        } else {
            listOf(
                NoteChecklistItem(
                    id = "item_1",
                    text = note.title,
                    isChecked = newDone
                )
            )
        }
        onSaveNote(
            note,
            note.title,
            note.content,
            updatedItems,
            note.colorHex,
            note.priority,
            note.labelsCsv,
            note.isPinned,
            note.isPinnedToNotification,
            note.reminderMillis,
            note.repeatInterval,
            note.linkedTransactionType,
            note.linkedEntityId,
            note.linkedEntityName,
            note.targetBudgetPaisa,
            false
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("todos_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header with Stats & Back button
        item(key = "todo_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Column {
                        Text(
                            text = if (isBn) "করণীয় তালিকা (To-Do)" else "To-Do List",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) {
                                "${MoneyUtils.formatNumber(activeCount, "bn")}টি চলমান • ${MoneyUtils.formatNumber(completedCount, "bn")}টি সম্পন্ন"
                            } else {
                                "$activeCount active • $completedCount completed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (completedCount > 0) {
                    TextButton(
                        onClick = { showClearCompletedConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = FinanceExpense)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBn) "সম্পন্ন মুছুন" else "Clear Done")
                    }
                }
            }
        }

        // 2. Fast Inline Task Creation Card
        item(key = "quick_add_task_card") {
            GlassCard(
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isBn) "নতুন কাজ যোগ করুন" else "Quick Add Task",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = {
                            Text(
                                if (isBn) "নতুন করণীয় লিখুন... (যেমন: চাল ডাল কেনা)"
                                else "Add a new task..."
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(theme.accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("todo_quick_input")
                    )

                    Button(
                        onClick = { submitNewTask() },
                        enabled = newTaskTitle.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accentColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 50.dp)
                            .testTag("todo_quick_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBn) "যোগ করুন" else "Add", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Priority Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isBn) "অগ্রাধিকার:" else "Priority:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        NotePriority.HIGH to (if (isBn) "জরুরি" else "High"),
                        NotePriority.NORMAL to (if (isBn) "সাধারণ" else "Normal"),
                        NotePriority.LOW to (if (isBn) "কম" else "Low")
                    ).forEach { (p, label) ->
                        val isSel = selectedPriority == p
                        val pColor = when (p) {
                            NotePriority.URGENT, NotePriority.HIGH -> FinanceExpense
                            NotePriority.NORMAL -> FinanceIncome
                            NotePriority.LOW -> Color(0xFF06B6D4)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) pColor.copy(alpha = 0.25f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) pColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPriority = p }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) pColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Filter Tabs (সব / চলমান / সম্পন্ন)
        item(key = "todo_filter_tabs") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TodoFilterTab.ALL to (if (isBn) "সকল (${MoneyUtils.formatNumber(notes.size, "bn")})" else "All (${notes.size})"),
                    TodoFilterTab.ACTIVE to (if (isBn) "চলমান (${MoneyUtils.formatNumber(activeCount, "bn")})" else "Active ($activeCount)"),
                    TodoFilterTab.COMPLETED to (if (isBn) "সম্পন্ন (${MoneyUtils.formatNumber(completedCount, "bn")})" else "Done ($completedCount)")
                ).forEach { (tab, label) ->
                    val isSel = currentFilter == tab
                    FilterChip(
                        selected = isSel,
                        onClick = { currentFilter = tab },
                        label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // 4. Todo Tasks List or Empty State
        if (filteredNotes.isEmpty()) {
            item(key = "todo_empty_state") {
                FriendlyEmptyState(
                    icon = Icons.Default.Checklist,
                    title = if (currentFilter == TodoFilterTab.COMPLETED) {
                        if (isBn) "কোনো সম্পন্ন কাজ নেই" else "No completed tasks yet"
                    } else {
                        if (isBn) "কোনো করণীয় বাকি নেই!" else "No tasks found!"
                    },
                    subtitle = if (isBn) {
                        "নতুন কোনো কাজের কথা মনে পড়লে উপরের বক্সে লিখে সহজেই যোগ করুন।"
                    } else {
                        "Type in the box above to quickly record what you need to do."
                    },
                    actionLabel = if (isBn) "+ প্রথম কাজ লিখুন" else "+ Add a Task",
                    onAction = { /* Focus or ready */ }
                )
            }
        } else {
            items(items = filteredNotes, key = { it.id }) { note ->
                val priorityColor = when (note.priority) {
                    NotePriority.HIGH.name -> FinanceExpense
                    NotePriority.LOW.name -> Color(0xFF06B6D4)
                    else -> FinanceIncome
                }
                val isCompleted = note.isCompleted

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("todo_card_${note.id}"),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Checkbox + Title Row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { toggleTaskCompletion(note) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Checkbox circle
                            IconButton(
                                onClick = { toggleTaskCompletion(note) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("todo_toggle_${note.id}")
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = FinanceIncome,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Pending",
                                        tint = priorityColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCompleted) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (note.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Badges Row: Priority + Reminder info
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Priority badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = priorityColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = when (note.priority) {
                                                NotePriority.HIGH.name -> if (isBn) "জরুরি" else "High"
                                                NotePriority.LOW.name -> if (isBn) "কম" else "Low"
                                                else -> if (isBn) "সাধারণ" else "Normal"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = priorityColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Reminder badge if scheduled
                                    if (note.reminderMillis != null && note.reminderMillis > System.currentTimeMillis()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = FinanceLoan.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Alarm,
                                                    contentDescription = null,
                                                    tint = FinanceLoan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = MoneyUtils.formatDateShort(note.reminderMillis, if (isBn) "bn" else "en"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = FinanceLoan
                                                )
                                            }
                                        }
                                    }

                                    if (note.isPinnedToNotification) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.PushPin,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBn) "পিন করা" else "Pinned",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFF59E0B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Icons: Reminder, Edit, Delete
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Reminder / Alert
                            IconButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    reminderForNote = note
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Alarm,
                                    contentDescription = "Set Reminder",
                                    tint = if (note.reminderMillis != null) FinanceLoan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Edit
                            IconButton(
                                onClick = { editingNote = note },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete
                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = FinanceExpense.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reminder Preset Dialog
    if (reminderForNote != null) {
        val target = reminderForNote!!
        AlertDialog(
            onDismissRequest = { reminderForNote = null },
            title = {
                Text(
                    text = if (isBn) "রিমাইন্ডার ও নোটিফিকেশন" else "Task Reminder",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset 1: Instant Notification
                    OutlinedButton(
                        onClick = {
                            onTriggerInstantAlert(target)
                            reminderForNote = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "এখনই নোটিফিকেশন পাঠান" else "Notify Now")
                    }

                    // Preset 2: Status Bar Pin
                    OutlinedButton(
                        onClick = {
                            onToggleLiveStatusBarPin(target)
                            reminderForNote = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (target.isPinnedToNotification) {
                                if (isBn) "স্ট্যাটাস বার থেকে আনপিন করুন" else "Unpin from Status Bar"
                            } else {
                                if (isBn) "স্ট্যাটাস বারে পিন করে রাখুন" else "Pin to Status Bar"
                            }
                        )
                    }

                    // Preset 3: In 1 Hour
                    OutlinedButton(
                        onClick = {
                            val inOneHour = System.currentTimeMillis() + 60 * 60 * 1000L
                            onSaveNote(
                                target,
                                target.title,
                                target.content,
                                target.checklistItems(),
                                target.colorHex,
                                target.priority,
                                target.labelsCsv,
                                target.isPinned,
                                target.isPinnedToNotification,
                                inOneHour,
                                "NONE",
                                target.linkedTransactionType,
                                target.linkedEntityId,
                                target.linkedEntityName,
                                target.targetBudgetPaisa,
                                false
                            )
                            reminderForNote = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "১ ঘণ্টা পর মনে করিয়ে দিন" else "Remind in 1 Hour")
                    }

                    // Preset 4: Tonight 8 PM
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 20)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                if (timeInMillis <= System.currentTimeMillis()) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            onSaveNote(
                                target,
                                target.title,
                                target.content,
                                target.checklistItems(),
                                target.colorHex,
                                target.priority,
                                target.labelsCsv,
                                target.isPinned,
                                target.isPinnedToNotification,
                                cal.timeInMillis,
                                "NONE",
                                target.linkedTransactionType,
                                target.linkedEntityId,
                                target.linkedEntityName,
                                target.targetBudgetPaisa,
                                false
                            )
                            reminderForNote = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "আজ রাত ৮:০০ টায়" else "Tonight at 8:00 PM")
                    }

                    // Preset 5: Tomorrow 9 AM
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            onSaveNote(
                                target,
                                target.title,
                                target.content,
                                target.checklistItems(),
                                target.colorHex,
                                target.priority,
                                target.labelsCsv,
                                target.isPinned,
                                target.isPinnedToNotification,
                                cal.timeInMillis,
                                "NONE",
                                target.linkedTransactionType,
                                target.linkedEntityId,
                                target.linkedEntityName,
                                target.targetBudgetPaisa,
                                false
                            )
                            reminderForNote = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "কাল সকাল ৯:০০ টায়" else "Tomorrow at 9:00 AM")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reminderForNote = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Edit Task Dialog
    if (editingNote != null) {
        val target = editingNote!!
        var editTitle by remember(target) { mutableStateOf(target.title) }
        var editContent by remember(target) { mutableStateOf(target.content) }
        var editPriority by remember(target) {
            mutableStateOf(
                try { NotePriority.valueOf(target.priority) } catch (_: Exception) { NotePriority.NORMAL }
            )
        }

        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = {
                Text(
                    text = if (isBn) "করণীয় সম্পাদনা" else "Edit Task",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(if (isBn) "কাজের শিরোনাম" else "Task Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text(if (isBn) "বিস্তারিত নোট (ঐচ্ছিক)" else "Notes (Optional)") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isBn) "অগ্রাধিকার নির্বাচন করুন" else "Select Priority",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            NotePriority.HIGH to (if (isBn) "জরুরি" else "High"),
                            NotePriority.NORMAL to (if (isBn) "সাধারণ" else "Normal"),
                            NotePriority.LOW to (if (isBn) "কম" else "Low")
                        ).forEach { (p, label) ->
                            val isSel = editPriority == p
                            val pColor = when (p) {
                                NotePriority.URGENT, NotePriority.HIGH -> FinanceExpense
                                NotePriority.NORMAL -> FinanceIncome
                                NotePriority.LOW -> Color(0xFF06B6D4)
                            }
                            FilterChip(
                                selected = isSel,
                                onClick = { editPriority = p },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = pColor.copy(alpha = 0.25f),
                                    selectedLabelColor = pColor
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            val newColor = when (editPriority) {
                                NotePriority.URGENT, NotePriority.HIGH -> "#EF4444"
                                NotePriority.NORMAL -> "#10B981"
                                NotePriority.LOW -> "#06B6D4"
                            }
                            val items = target.checklistItems().map { it.copy(text = editTitle.trim()) }
                            val effectiveItems = if (items.isNotEmpty()) items else listOf(
                                NoteChecklistItem("item_1", editTitle.trim(), target.isCompleted)
                            )
                            onSaveNote(
                                target,
                                editTitle.trim(),
                                editContent.trim(),
                                effectiveItems,
                                newColor,
                                editPriority.name,
                                target.labelsCsv,
                                target.isPinned,
                                target.isPinnedToNotification,
                                target.reminderMillis,
                                target.repeatInterval,
                                target.linkedTransactionType,
                                target.linkedEntityId,
                                target.linkedEntityName,
                                target.targetBudgetPaisa,
                                false
                            )
                            editingNote = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isBn) "সংরক্ষণ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Clear Completed Confirm Dialog
    if (showClearCompletedConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCompletedConfirm = false },
            title = {
                Text(if (isBn) "সম্পন্ন কাজগুলো মুছে ফেলতে চান?" else "Clear Completed Tasks?")
            },
            text = {
                Text(
                    if (isBn) {
                        "এটি নিশ্চিত করলে সকল সম্পন্ন (${MoneyUtils.formatNumber(completedCount, "bn")}টি) করণীয় মুছে ফেলা হবে।"
                    } else {
                        "This will permanently delete all $completedCount completed tasks."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCompletedConfirm = false
                        val completedList = notes.filter { it.isCompleted }
                        for (task in completedList) {
                            onDeleteNote(task.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpense)
                ) {
                    Text(if (isBn) "সব মুছুন" else "Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedConfirm = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
