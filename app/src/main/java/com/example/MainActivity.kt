package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppLockScreenOverlay
import com.example.ui.components.GlassmorphicBackground
import com.example.ui.components.UndoToastBanner
import com.example.ui.screens.BackupAndRestoreSubScreen
import com.example.ui.screens.CategoriesSubScreen
import com.example.ui.screens.CreateLoanDialog
import com.example.ui.screens.CustomizationSubScreen
import com.example.ui.screens.GlobalAddTransactionPickerSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoansSubScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PeopleScreen
import com.example.ui.screens.QuickEntryBottomSheet
import com.example.ui.screens.ReportsSubScreen
import com.example.ui.screens.SettingsAndSecuritySubScreen
import com.example.ui.screens.ShopsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.viewmodel.HisabViewModel
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.MoreSubScreen
import com.example.ui.viewmodel.QuickEntryRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContext = LocalContext.current.applicationContext
            val viewModel: HisabViewModel = viewModel(
                factory = HisabViewModel.Factory(appContext)
            )
            HisabAppRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun HisabAppRoot(viewModel: HisabViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val moreSubScreen by viewModel.moreSubScreen.collectAsStateWithLifecycle()
    val selectedShopId by viewModel.selectedShopId.collectAsStateWithLifecycle()
    val selectedPersonId by viewModel.selectedPersonId.collectAsStateWithLifecycle()
    val showGlobalTypePicker by viewModel.showGlobalTypePicker.collectAsStateWithLifecycle()
    val activeQuickEntry by viewModel.activeQuickEntry.collectAsStateWithLifecycle()
    val undoOperation by viewModel.undoOperation.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val pendingImportPreview by viewModel.pendingImportPreview.collectAsStateWithLifecycle()
    val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsStateWithLifecycle()

    var showGlobalCreateLoanDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    HisabTheme(settings = uiState.settings) {
        val strings = LocalHisabStrings.current
        val theme = LocalHisabTheme.current

        // Optional Local App Lock Gate (Section 33)
        val requiresLock = uiState.settings.appLockEnabled &&
            uiState.settings.appLockPin.length == 4 &&
            !isSessionUnlocked

        if (requiresLock) {
            AppLockScreenOverlay(
                expectedPin = uiState.settings.appLockPin,
                biometricEnabled = uiState.settings.biometricEnabled,
                onUnlocked = { viewModel.unlockSession() }
            )
            return@HisabTheme
        }

        // BackHandler for non-Home tabs when no sub-detail is open
        if (currentTab != MainNavTab.HOME &&
            selectedShopId == null &&
            selectedPersonId == null &&
            moreSubScreen == MoreSubScreen.HUB
        ) {
            BackHandler {
                viewModel.selectTab(MainNavTab.HOME)
            }
        }

        GlassmorphicBackground {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                floatingActionButton = {
                    // Luminous Gradient Glass Floating Action Button
                    val fabShape = RoundedCornerShape(22.dp)
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(fabShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        theme.accentColor,
                                        theme.accentColor.copy(alpha = 0.78f)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    1.2.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.70f),
                                            theme.accentColor.copy(alpha = 0.30f)
                                        )
                                    )
                                ),
                                fabShape
                            )
                            .clickable { viewModel.openGlobalAddMenu() }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("global_add_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addTransaction,
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF042F2E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                bottomBar = {
                    FloatingGlassBottomBar(
                        currentTab = currentTab,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainNavTab.HOME -> {
                            HomeScreen(
                                settings = uiState.settings,
                                snapshot = uiState.snapshot,
                                recentTransactions = uiState.recentTransactions,
                                onOpenQuickEntry = { req -> viewModel.openQuickEntry(req) },
                                onOpenLoanCreator = { showGlobalCreateLoanDialog = true },
                                onSelectTab = { tab -> viewModel.selectTab(tab) },
                                onOpenMoreSubScreen = { sub -> viewModel.openMoreSubScreen(sub) },
                                onOpenSearch = { viewModel.openGlobalSearchWithQuery("") },
                                onToggleDashboardCard = { id -> viewModel.toggleDashboardCardVisibility(id) },
                                onMoveDashboardCard = { id, up -> viewModel.moveDashboardCard(id, up) },
                                onChangeDashboardLayout = { style ->
                                    viewModel.updateSettings { it.copy(dashboardLayoutStyle = style) }
                                },
                                onCompleteFirstLaunch = { sym, code, openingPaisa, demo ->
                                    viewModel.completeFirstLaunch(sym, code, openingPaisa, demo)
                                }
                            )
                        }

                        MainNavTab.TRANSACTIONS -> {
                            TransactionsScreen(
                                transactions = uiState.filteredTransactions,
                                totalTransactionsCount = uiState.allTransactionsCount,
                                hasMore = uiState.hasMoreTransactions,
                                filterState = uiState.filterState,
                                onSearchQueryChange = { q -> viewModel.updateSearchQuery(q) },
                                onDatePresetChange = { preset, s, e -> viewModel.updateDateFilter(preset, s, e) },
                                onFilterGroupChange = { grp -> viewModel.updateFilterGroup(grp) },
                                onSortOptionChange = { srt -> viewModel.updateSortOption(srt) },
                                onResetFilters = { viewModel.resetFilters() },
                                onLoadMore = { viewModel.loadMoreTransactions() },
                                onEditTransaction = { tx ->
                                    viewModel.openQuickEntry(
                                        QuickEntryRequest(
                                            initialType = com.example.data.local.TransactionType.fromString(tx.type),
                                            editingTransaction = tx
                                        )
                                    )
                                },
                                onAddTransaction = { viewModel.openGlobalAddMenu() }
                            )
                        }

                        MainNavTab.PEOPLE -> {
                            PeopleScreen(
                                personSummaries = uiState.snapshot.personSummaries,
                                totalPeopleReceivablePaisa = uiState.snapshot.totalPeopleReceivablePaisa,
                                totalPeoplePayablePaisa = uiState.snapshot.totalPeoplePayablePaisa,
                                selectedPersonId = selectedPersonId,
                                onSelectPerson = { id -> viewModel.openPersonDetail(id) },
                                onOpenQuickEntry = { req -> viewModel.openQuickEntry(req) },
                                onSavePerson = { existing, name, phone, note, color ->
                                    viewModel.savePerson(existing, name, phone, note, color)
                                },
                                onDeletePerson = { id -> viewModel.deletePerson(id) }
                            )
                        }

                        MainNavTab.SHOPS -> {
                            ShopsScreen(
                                shopSummaries = uiState.snapshot.shopSummaries,
                                totalShopDuePaisa = uiState.snapshot.totalShopDuePaisa,
                                selectedShopId = selectedShopId,
                                onSelectShop = { id -> viewModel.openShopDetail(id) },
                                onOpenQuickEntry = { req -> viewModel.openQuickEntry(req) },
                                onSaveShop = { existing, name, owner, phone, addr, note, color ->
                                    viewModel.saveShop(existing, name, owner, phone, addr, note, color)
                                },
                                onDeleteShop = { id -> viewModel.deleteShop(id) }
                            )
                        }

                        MainNavTab.NOTES -> {
                            NotesScreen(
                                notes = uiState.notes,
                                shops = uiState.shops,
                                people = uiState.people,
                                showBackButton = false,
                                onSaveNote = { existing, title, content, items, colorHex, priority, labelsCsv, isPinned, isPinnedNotif, reminderMillis, repeat, linkedType, linkedId, linkedName, targetPaisa, fireNow ->
                                    viewModel.saveSmartNote(
                                        context = context,
                                        existingNote = existing,
                                        title = title,
                                        content = content,
                                        checklistItems = items,
                                        colorHex = colorHex,
                                        priority = priority,
                                        labelsCsv = labelsCsv,
                                        isPinned = isPinned,
                                        isPinnedToNotification = isPinnedNotif,
                                        reminderMillis = reminderMillis,
                                        repeatInterval = repeat,
                                        linkedTransactionType = linkedType,
                                        linkedEntityId = linkedId,
                                        linkedEntityName = linkedName,
                                        targetBudgetPaisa = targetPaisa,
                                        fireRealtimeAlertNow = fireNow
                                    )
                                },
                                onToggleChecklistItem = { note, itemId ->
                                    viewModel.toggleNoteChecklistItem(context, note, itemId)
                                },
                                onTogglePin = { note -> viewModel.toggleNotePin(note) },
                                onToggleArchive = { note -> viewModel.toggleNoteArchive(context, note) },
                                onToggleLiveStatusBarPin = { note -> viewModel.toggleNoteLiveStatusBarPin(context, note) },
                                onTriggerInstantAlert = { note -> viewModel.triggerInstantNoteNotification(context, note) },
                                onConvertNoteToTransaction = { note -> viewModel.convertNoteToTransaction(note) },
                                onDeleteNote = { noteId -> viewModel.deleteSmartNote(context, noteId) }
                            )
                        }

                        MainNavTab.MORE -> {
                            when (moreSubScreen) {
                                MoreSubScreen.HUB -> {
                                    MoreHubScreen(
                                        snapshot = uiState.snapshot,
                                        activeNotesCount = uiState.notes.count { !it.isArchived },
                                        onSelectSubScreen = { sub -> viewModel.openMoreSubScreen(sub) }
                                    )
                                }

                                MoreSubScreen.NOTES -> {
                                    NotesScreen(
                                        notes = uiState.notes,
                                        shops = uiState.shops,
                                        people = uiState.people,
                                        showBackButton = true,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) },
                                        onSaveNote = { existing, title, content, items, colorHex, priority, labelsCsv, isPinned, isPinnedNotif, reminderMillis, repeat, linkedType, linkedId, linkedName, targetPaisa, fireNow ->
                                            viewModel.saveSmartNote(
                                                context = context,
                                                existingNote = existing,
                                                title = title,
                                                content = content,
                                                checklistItems = items,
                                                colorHex = colorHex,
                                                priority = priority,
                                                labelsCsv = labelsCsv,
                                                isPinned = isPinned,
                                                isPinnedToNotification = isPinnedNotif,
                                                reminderMillis = reminderMillis,
                                                repeatInterval = repeat,
                                                linkedTransactionType = linkedType,
                                                linkedEntityId = linkedId,
                                                linkedEntityName = linkedName,
                                                targetBudgetPaisa = targetPaisa,
                                                fireRealtimeAlertNow = fireNow
                                            )
                                        },
                                        onToggleChecklistItem = { note, itemId ->
                                            viewModel.toggleNoteChecklistItem(context, note, itemId)
                                        },
                                        onTogglePin = { note -> viewModel.toggleNotePin(note) },
                                        onToggleArchive = { note -> viewModel.toggleNoteArchive(context, note) },
                                        onToggleLiveStatusBarPin = { note -> viewModel.toggleNoteLiveStatusBarPin(context, note) },
                                        onTriggerInstantAlert = { note -> viewModel.triggerInstantNoteNotification(context, note) },
                                        onConvertNoteToTransaction = { note -> viewModel.convertNoteToTransaction(note) },
                                        onDeleteNote = { noteId -> viewModel.deleteSmartNote(context, noteId) }
                                    )
                                }

                                MoreSubScreen.LOANS -> {
                                    LoansSubScreen(
                                        loanSummaries = uiState.snapshot.loanSummaries,
                                        people = uiState.people,
                                        totalLoansGivenRemainingPaisa = uiState.snapshot.totalLoansGivenRemainingPaisa,
                                        totalLoansTakenRemainingPaisa = uiState.snapshot.totalLoansTakenRemainingPaisa,
                                        availableCashPaisa = uiState.snapshot.cashBalancePaisa,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) },
                                        onCreateLoan = { isLent, pId, pName, prin, intr, rate, inst, start, due, note ->
                                            viewModel.createLoan(isLent, pId, pName, prin, intr, rate, inst, start, due, note)
                                        },
                                        onDeleteLoan = { id -> viewModel.deleteLoan(id) },
                                        onOpenRepaymentEntry = { req -> viewModel.openQuickEntry(req) }
                                    )
                                }

                                MoreSubScreen.REPORTS -> {
                                    ReportsSubScreen(
                                        snapshot = uiState.snapshot,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) }
                                    )
                                }

                                MoreSubScreen.CATEGORIES -> {
                                    CategoriesSubScreen(
                                        categories = uiState.categories,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) },
                                        onSaveCategory = { existing, name, nameBn, type, icon, color ->
                                            viewModel.saveCategory(existing, name, nameBn, type, icon, color)
                                        },
                                        onMoveCategory = { cat, up -> viewModel.moveCategoryOrder(cat, up) },
                                        onDeleteCategory = { id -> viewModel.deleteCategory(id) }
                                    )
                                }

                                MoreSubScreen.BACKUP -> {
                                    val dbSize = remember(uiState.allTransactionsCount, uiState.backupMetadata) {
                                        viewModel.getDatabaseSizeBytes()
                                    }
                                    val savedBackups = remember(uiState.backupMetadata) {
                                        viewModel.getSavedLocalBackups()
                                    }
                                    BackupAndRestoreSubScreen(
                                        backupMetadata = uiState.backupMetadata,
                                        totalTransactionsCount = uiState.allTransactionsCount,
                                        databaseSizeBytes = dbSize,
                                        savedLocalBackups = savedBackups,
                                        pendingPreview = pendingImportPreview,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) },
                                        onExportJson = { share, cb ->
                                            viewModel.exportJsonBackup(context, share, cb)
                                        },
                                        onExportCsv = { share, cb ->
                                            viewModel.exportCsvTransactions(context, share, cb)
                                        },
                                        onInspectBackupJson = { raw -> viewModel.inspectBackupForImport(raw) },
                                        onConfirmFullRestore = { viewModel.confirmFullRestore() },
                                        onConfirmMergeImport = { viewModel.confirmMergeImport() },
                                        onCancelPendingImport = { viewModel.cancelPendingImport() }
                                    )
                                }

                                MoreSubScreen.CUSTOMIZATION -> {
                                    CustomizationSubScreen(
                                        settings = uiState.settings,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) },
                                        onUpdateSettings = { transform -> viewModel.updateSettings(transform) }
                                    )
                                }

                                MoreSubScreen.SETTINGS, MoreSubScreen.SECURITY -> {
                                    SettingsAndSecuritySubScreen(
                                        settings = uiState.settings,
                                        isSecurityFocused = moreSubScreen == MoreSubScreen.SECURITY,
                                        onBack = { viewModel.openMoreSubScreen(MoreSubScreen.HUB) },
                                        onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                        onLoadDemoData = { viewModel.loadDemoData() },
                                        onClearAllData = { viewModel.clearAllData() }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Overlay Banners (Undo Toast + Status Info Banner)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        AnimatedVisibility(
                            visible = statusBannerMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF0F172A).copy(alpha = 0.85f),
                                                Color(0xFF1E293B).copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .testTag("status_message_banner")
                            ) {
                                Text(
                                    text = statusBannerMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        UndoToastBanner(
                            visible = undoOperation != null,
                            message = undoOperation?.message ?: strings.transactionSaved,
                            onUndo = { viewModel.performUndo() },
                            onDismiss = { viewModel.dismissUndo() }
                        )
                    }
                }
            }

            // Global Add Picker Sheet
            if (showGlobalTypePicker) {
                GlobalAddTransactionPickerSheet(
                    onDismiss = { viewModel.closeGlobalAddMenu() },
                    onSelectType = { type ->
                        viewModel.openQuickEntry(QuickEntryRequest(initialType = type))
                    },
                    onOpenLoanCreator = {
                        viewModel.closeGlobalAddMenu()
                        showGlobalCreateLoanDialog = true
                    }
                )
            }

            // Quick Transaction Entry Sheet (Create or Edit)
            val currentEntryReq = activeQuickEntry
            if (currentEntryReq != null) {
                QuickEntryBottomSheet(
                    request = currentEntryReq,
                    categories = uiState.categories,
                    shops = uiState.shops,
                    people = uiState.people,
                    loans = uiState.loans,
                    snapshot = uiState.snapshot,
                    onDismiss = { viewModel.closeQuickEntry() },
                    onSave = { existingTx, type, amountPaisa, timestamp, catId, catName, pId, pName, sId, sName, lId, prod, note, dueMillis ->
                        viewModel.saveTransactionFromForm(
                            existingTx = existingTx,
                            type = type,
                            amountPaisa = amountPaisa,
                            timestamp = timestamp,
                            categoryId = catId,
                            categoryName = catName,
                            personId = pId,
                            personName = pName,
                            shopId = sId,
                            shopName = sName,
                            loanId = lId,
                            productOrDescription = prod,
                            note = note,
                            dueDateMillis = dueMillis
                        )
                    },
                    onDeleteExisting = { txId ->
                        viewModel.deleteTransactionWithUndo(txId)
                    }
                )
            }

            // Global Loan Creator Dialog
            if (showGlobalCreateLoanDialog) {
                CreateLoanDialog(
                    people = uiState.people,
                    currencySymbol = uiState.settings.currencySymbol,
                    availableCashPaisa = uiState.snapshot.cashBalancePaisa,
                    onDismiss = { showGlobalCreateLoanDialog = false },
                    onCreate = { isLent, pId, pName, principal, interest, rateStr, inst, dueMillis, note ->
                        viewModel.createLoan(
                            isLentByMe = isLent,
                            personId = pId,
                            personName = pName,
                            principalPaisa = principal,
                            interestPaisa = interest,
                            interestRatePercent = rateStr,
                            installmentCount = inst,
                            startDateMillis = System.currentTimeMillis(),
                            dueDateMillis = dueMillis,
                            note = note
                        )
                        showGlobalCreateLoanDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingGlassBottomBar(
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    val strings = LocalHisabStrings.current
    val theme = LocalHisabTheme.current
    val dockShape = RoundedCornerShape(28.dp)

    data class NavEntry(
        val tab: MainNavTab,
        val label: String,
        val icon: ImageVector,
        val testTag: String
    )

    val entries = listOf(
        NavEntry(MainNavTab.HOME, strings.navHome, Icons.Default.Home, "nav_tab_home"),
        NavEntry(MainNavTab.TRANSACTIONS, strings.navTransactions, Icons.AutoMirrored.Filled.ReceiptLong, "nav_tab_transactions"),
        NavEntry(MainNavTab.PEOPLE, strings.navPeople, Icons.Default.People, "nav_tab_people"),
        NavEntry(MainNavTab.SHOPS, strings.navShops, Icons.Default.Storefront, "nav_tab_shops"),
        NavEntry(MainNavTab.NOTES, strings.navNotes, Icons.Default.Checklist, "nav_tab_notes"),
        NavEntry(MainNavTab.MORE, strings.navMore, Icons.Default.MoreHoriz, "nav_tab_more")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dockShape)
                .background(
                    Brush.linearGradient(
                        colors = if (theme.isDark) {
                            listOf(
                                Color(0xFF1E293B).copy(alpha = 0.58f),
                                Color(0xFF0F172A).copy(alpha = 0.48f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.78f),
                                Color.White.copy(alpha = 0.62f)
                            )
                        }
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (theme.isDark) 0.35f else 0.95f),
                                theme.accentColor.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    ),
                    dockShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in entries) {
                val selected = currentTab == item.tab
                val itemShape = RoundedCornerShape(22.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(itemShape)
                        .background(
                            if (selected) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        theme.accentColor.copy(alpha = if (theme.isDark) 0.28f else 0.20f),
                                        theme.accentColor.copy(alpha = 0.08f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .let { mod ->
                            if (selected) {
                                mod.border(
                                    BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.45f)),
                                    itemShape
                                )
                            } else mod
                        }
                        .clickable { onSelectTab(item.tab) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) theme.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            if (theme.isDark) Color.White else theme.accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
