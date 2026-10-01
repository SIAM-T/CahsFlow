package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.ValidatedBackupPayload
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BackupMetadataEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DateFilterPreset
import com.example.data.local.HisabDatabase
import com.example.data.local.LoanEntity
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.SortOption
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionFilterGroup
import com.example.data.local.TransactionType
import com.example.data.repository.HisabRepository
import com.example.domain.FinancialEngine
import com.example.domain.FinancialSnapshot
import com.example.util.MoneyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class MainNavTab {
    HOME,
    TRANSACTIONS,
    PEOPLE,
    SHOPS,
    MORE
}

enum class MoreSubScreen {
    HUB,
    LOANS,
    REPORTS,
    CATEGORIES,
    BACKUP,
    CUSTOMIZATION,
    SETTINGS,
    SECURITY
}

sealed class UndoableOperation(val message: String) {
    data class AddedTransaction(val tx: TransactionEntity, val msg: String) : UndoableOperation(msg)
    data class DeletedTransaction(val tx: TransactionEntity, val msg: String) : UndoableOperation(msg)
    data class EditedTransaction(val previousTx: TransactionEntity, val msg: String) : UndoableOperation(msg)
}

data class QuickEntryRequest(
    val initialType: TransactionType = TransactionType.EXPENSE,
    val preselectedShopId: String? = null,
    val preselectedPersonId: String? = null,
    val preselectedLoanId: String? = null,
    val preselectedCategoryId: String? = null,
    val editingTransaction: TransactionEntity? = null
)

data class TransactionsFilterState(
    val searchQuery: String = "",
    val datePreset: DateFilterPreset = DateFilterPreset.ALL,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val filterGroup: TransactionFilterGroup = TransactionFilterGroup.ALL,
    val sortOption: SortOption = SortOption.NEWEST,
    val pageSizeLimit: Int = 100
)

data class HisabUiState(
    val isInitialized: Boolean = false,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val backupMetadata: BackupMetadataEntity = BackupMetadataEntity(),
    val categories: List<CategoryEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val shops: List<ShopEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val allTransactionsCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val hasMoreTransactions: Boolean = false,
    val snapshot: FinancialSnapshot = FinancialSnapshot(),
    val filterState: TransactionsFilterState = TransactionsFilterState()
)

class HisabViewModel(
    private val repository: HisabRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(MainNavTab.HOME)
    val currentTab: StateFlow<MainNavTab> = _currentTab.asStateFlow()

    private val _moreSubScreen = MutableStateFlow(MoreSubScreen.HUB)
    val moreSubScreen: StateFlow<MoreSubScreen> = _moreSubScreen.asStateFlow()

    private val _selectedShopId = MutableStateFlow<String?>(null)
    val selectedShopId: StateFlow<String?> = _selectedShopId.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<String?>(null)
    val selectedPersonId: StateFlow<String?> = _selectedPersonId.asStateFlow()

    private val _showGlobalTypePicker = MutableStateFlow(false)
    val showGlobalTypePicker: StateFlow<Boolean> = _showGlobalTypePicker.asStateFlow()

    private val _activeQuickEntry = MutableStateFlow<QuickEntryRequest?>(null)
    val activeQuickEntry: StateFlow<QuickEntryRequest?> = _activeQuickEntry.asStateFlow()

    private val _filterState = MutableStateFlow(TransactionsFilterState())
    val filterState: StateFlow<TransactionsFilterState> = _filterState.asStateFlow()

    private val _undoOperation = MutableStateFlow<UndoableOperation?>(null)
    val undoOperation: StateFlow<UndoableOperation?> = _undoOperation.asStateFlow()
    private var undoDismissJob: Job? = null

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _pendingImportPreview = MutableStateFlow<ValidatedBackupPayload?>(null)
    val pendingImportPreview: StateFlow<ValidatedBackupPayload?> = _pendingImportPreview.asStateFlow()

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialized()
        }
    }

    private val coreDataFlow = combine(
        repository.settingsFlow,
        repository.backupMetadataFlow,
        repository.categoriesFlow,
        repository.peopleFlow,
        repository.shopsFlow
    ) { settings, backupMeta, categories, people, shops ->
        CoreDataBundle(
            settings = settings ?: AppSettingsEntity(),
            backupMeta = backupMeta ?: BackupMetadataEntity(),
            categories = categories,
            people = people,
            shops = shops
        )
    }

    val uiState: StateFlow<HisabUiState> = combine(
        coreDataFlow,
        repository.loansFlow,
        repository.transactionsFlow,
        _filterState
    ) { core, loans, transactions, filter ->
        val snapshot = FinancialEngine.calculateSnapshot(
            settings = core.settings,
            transactions = transactions,
            shops = core.shops,
            people = core.people,
            loans = loans,
            categories = core.categories
        )

        val filteredAll = applyFiltersAndSort(transactions, filter)
        val paged = filteredAll.take(filter.pageSizeLimit)

        HisabUiState(
            isInitialized = true,
            settings = core.settings,
            backupMetadata = core.backupMeta,
            categories = core.categories,
            people = core.people,
            shops = core.shops,
            loans = loans,
            allTransactionsCount = transactions.size,
            recentTransactions = transactions.take(12),
            filteredTransactions = paged,
            hasMoreTransactions = filteredAll.size > filter.pageSizeLimit,
            snapshot = snapshot,
            filterState = filter
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HisabUiState()
        )

    private data class CoreDataBundle(
        val settings: AppSettingsEntity,
        val backupMeta: BackupMetadataEntity,
        val categories: List<CategoryEntity>,
        val people: List<PersonEntity>,
        val shops: List<ShopEntity>
    )

    private fun applyFiltersAndSort(
        transactions: List<TransactionEntity>,
        filter: TransactionsFilterState
    ): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val query = filter.searchQuery.trim().lowercase()

        val dateRange: Pair<Long, Long>? = when (filter.datePreset) {
            DateFilterPreset.ALL -> null
            DateFilterPreset.TODAY -> Pair(MoneyUtils.startOfTodayMillis(now), Long.MAX_VALUE)
            DateFilterPreset.YESTERDAY -> {
                val startYesterday = MoneyUtils.startOfYesterdayMillis(now)
                val startToday = MoneyUtils.startOfTodayMillis(now)
                Pair(startYesterday, startToday - 1L)
            }
            DateFilterPreset.THIS_WEEK -> Pair(MoneyUtils.startOfWeekMillis(now), Long.MAX_VALUE)
            DateFilterPreset.THIS_MONTH -> Pair(MoneyUtils.startOfMonthMillis(now), Long.MAX_VALUE)
            DateFilterPreset.LAST_MONTH -> MoneyUtils.startOfLastMonthMillis(now)
            DateFilterPreset.CUSTOM -> {
                val s = filter.customStartMillis ?: 0L
                val e = filter.customEndMillis ?: Long.MAX_VALUE
                Pair(s, e)
            }
        }

        val matching = transactions.filter { tx ->
            // 1. Date filter
            if (dateRange != null) {
                if (tx.timestamp < dateRange.first || tx.timestamp > dateRange.second) return@filter false
            }

            // 2. Group filter
            val tType = TransactionType.fromString(tx.type)
            val groupMatch = when (filter.filterGroup) {
                TransactionFilterGroup.ALL -> true
                TransactionFilterGroup.INCOME -> tType == TransactionType.INCOME
                TransactionFilterGroup.EXPENSE -> tType == TransactionType.EXPENSE
                TransactionFilterGroup.LENDING -> tType == TransactionType.LEND || tType == TransactionType.RECEIVE_PAYMENT
                TransactionFilterGroup.BORROWING -> tType == TransactionType.BORROW || tType == TransactionType.MAKE_PAYMENT
                TransactionFilterGroup.SHOP_DUE -> tType == TransactionType.SHOP_DUE || tType == TransactionType.SHOP_PAYMENT
                TransactionFilterGroup.PAYMENTS -> tType == TransactionType.RECEIVE_PAYMENT ||
                    tType == TransactionType.MAKE_PAYMENT ||
                    tType == TransactionType.SHOP_PAYMENT ||
                    tType == TransactionType.LOAN_REPAYMENT ||
                    tType == TransactionType.LOAN_PAYMENT
                TransactionFilterGroup.LOANS -> tType == TransactionType.LOAN_GIVEN ||
                    tType == TransactionType.LOAN_RECEIVED ||
                    tType == TransactionType.LOAN_REPAYMENT ||
                    tType == TransactionType.LOAN_PAYMENT ||
                    tx.loanId != null
            }
            if (!groupMatch) return@filter false

            // 3. Search query filter (Shop names, Person names, Product names, Notes, Transaction IDs, Amounts, Categories)
            if (query.isNotEmpty()) {
                val amountEditable = MoneyUtils.paisaToEditableString(tx.amountPaisa)
                val matchesShop = tx.shopName?.lowercase()?.contains(query) == true
                val matchesPerson = tx.personName?.lowercase()?.contains(query) == true
                val matchesProduct = tx.productOrDescription.lowercase().contains(query)
                val matchesNote = tx.note.lowercase().contains(query)
                val matchesCategory = tx.categoryName?.lowercase()?.contains(query) == true
                val matchesId = tx.id.lowercase().contains(query)
                val matchesAmount = amountEditable.contains(query)
                val matchesType = tType.labelEn.lowercase().contains(query) || tType.labelBn.contains(query)

                if (!(matchesShop || matchesPerson || matchesProduct || matchesNote || matchesCategory || matchesId || matchesAmount || matchesType)) {
                    return@filter false
                }
            }
            true
        }

        return when (filter.sortOption) {
            SortOption.NEWEST -> matching.sortedWith(compareByDescending<TransactionEntity> { it.timestamp }.thenByDescending { it.createdAt })
            SortOption.OLDEST -> matching.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.createdAt })
            SortOption.HIGHEST_AMOUNT -> matching.sortedByDescending { it.amountPaisa }
            SortOption.LOWEST_AMOUNT -> matching.sortedBy { it.amountPaisa }
        }
    }

    // --- Navigation Actions ---
    fun selectTab(tab: MainNavTab) {
        _currentTab.value = tab
        if (tab != MainNavTab.SHOPS) _selectedShopId.value = null
        if (tab != MainNavTab.PEOPLE) _selectedPersonId.value = null
        if (tab == MainNavTab.MORE) _moreSubScreen.value = MoreSubScreen.HUB
    }

    fun openMoreSubScreen(subScreen: MoreSubScreen) {
        _currentTab.value = MainNavTab.MORE
        _moreSubScreen.value = subScreen
    }

    fun openShopDetail(shopId: String?) {
        _selectedShopId.value = shopId
        if (shopId != null) {
            _currentTab.value = MainNavTab.SHOPS
        }
    }

    fun openPersonDetail(personId: String?) {
        _selectedPersonId.value = personId
        if (personId != null) {
            _currentTab.value = MainNavTab.PEOPLE
        }
    }

    fun openGlobalAddMenu() {
        _showGlobalTypePicker.value = true
    }

    fun closeGlobalAddMenu() {
        _showGlobalTypePicker.value = false
    }

    fun openQuickEntry(request: QuickEntryRequest) {
        _showGlobalTypePicker.value = false
        _activeQuickEntry.value = request
    }

    fun closeQuickEntry() {
        _activeQuickEntry.value = null
    }

    fun unlockSession() {
        _isSessionUnlocked.value = true
    }

    // --- Search & Filter Updates ---
    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query, pageSizeLimit = 100) }
    }

    fun openGlobalSearchWithQuery(query: String = "") {
        _filterState.update { it.copy(searchQuery = query, pageSizeLimit = 100) }
        _currentTab.value = MainNavTab.TRANSACTIONS
    }

    fun updateDateFilter(preset: DateFilterPreset, customStart: Long? = null, customEnd: Long? = null) {
        _filterState.update {
            it.copy(
                datePreset = preset,
                customStartMillis = customStart,
                customEndMillis = customEnd,
                pageSizeLimit = 100
            )
        }
    }

    fun updateFilterGroup(group: TransactionFilterGroup) {
        _filterState.update { it.copy(filterGroup = group, pageSizeLimit = 100) }
    }

    fun updateSortOption(sort: SortOption) {
        _filterState.update { it.copy(sortOption = sort) }
    }

    fun loadMoreTransactions() {
        _filterState.update { it.copy(pageSizeLimit = it.pageSizeLimit + 100) }
    }

    fun resetFilters() {
        _filterState.value = TransactionsFilterState()
    }

    // --- First Launch Setup ---
    fun completeFirstLaunch(
        currencySymbol: String,
        currencyCode: String,
        openingBalancePaisa: Long,
        loadDemoData: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings { current ->
                current.copy(
                    isFirstLaunchCompleted = true,
                    currencySymbol = currencySymbol,
                    currencyCode = currencyCode,
                    openingCashBalancePaisa = openingBalancePaisa
                )
            }
            if (loadDemoData) {
                repository.seedDemoDataIfRequested()
            }
        }
    }

    // --- Transaction CRUD + Undo ---
    fun saveTransactionFromForm(
        existingTx: TransactionEntity?,
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
        dueDateMillis: Long? = null,
        adjustmentIsOutflow: Boolean = false
    ) {
        if (amountPaisa <= 0L) {
            showBannerMessage("This transaction could not be saved. Please enter a valid amount greater than 0.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val direction = if (type == TransactionType.ADJUSTMENT) {
                    if (adjustmentIsOutflow) TransactionDirection.OUTFLOW else TransactionDirection.INFLOW
                } else {
                    type.defaultDirection
                }

                // Auto-create Shop if user typed a new shop name directly
                var resolvedShopId = shopId
                var resolvedShopName = shopName?.trim()
                if ((type == TransactionType.SHOP_DUE || type == TransactionType.SHOP_PAYMENT) &&
                    resolvedShopId == null && !resolvedShopName.isNullOrEmpty()
                ) {
                    val newShop = ShopEntity(
                        id = "shop_${UUID.randomUUID().toString().take(8)}",
                        name = resolvedShopName
                    )
                    repository.saveShop(newShop)
                    resolvedShopId = newShop.id
                }

                // Auto-create Person if user typed a new person name directly
                var resolvedPersonId = personId
                var resolvedPersonName = personName?.trim()
                if ((type == TransactionType.LEND || type == TransactionType.BORROW ||
                        type == TransactionType.RECEIVE_PAYMENT || type == TransactionType.MAKE_PAYMENT) &&
                    resolvedPersonId == null && !resolvedPersonName.isNullOrEmpty()
                ) {
                    val newPerson = PersonEntity(
                        id = "person_${UUID.randomUUID().toString().take(8)}",
                        name = resolvedPersonName
                    )
                    repository.savePerson(newPerson)
                    resolvedPersonId = newPerson.id
                }

                val defaultDesc = productOrDescription.trim().ifEmpty {
                    when (type) {
                        TransactionType.INCOME -> categoryName ?: "Income"
                        TransactionType.EXPENSE -> categoryName ?: "Expense"
                        TransactionType.SHOP_DUE -> "Products on due"
                        TransactionType.SHOP_PAYMENT -> "Payment received"
                        TransactionType.LEND -> "Lent to ${resolvedPersonName ?: "Person"}"
                        TransactionType.BORROW -> "Borrowed from ${resolvedPersonName ?: "Person"}"
                        TransactionType.RECEIVE_PAYMENT -> "Payment received"
                        TransactionType.MAKE_PAYMENT -> "Payment made"
                        TransactionType.LOAN_GIVEN -> "Loan given"
                        TransactionType.LOAN_RECEIVED -> "Loan taken"
                        TransactionType.LOAN_REPAYMENT -> "Loan repayment received"
                        TransactionType.LOAN_PAYMENT -> "Loan installment paid"
                        TransactionType.ADJUSTMENT -> "Balance adjustment"
                    }
                }

                val tx = TransactionEntity(
                    id = existingTx?.id ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                    type = type.name,
                    direction = direction.name,
                    amountPaisa = amountPaisa,
                    timestamp = timestamp,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    personId = resolvedPersonId,
                    personName = resolvedPersonName,
                    shopId = resolvedShopId,
                    shopName = resolvedShopName,
                    loanId = loanId,
                    productOrDescription = defaultDesc,
                    note = note.trim(),
                    dueDateMillis = dueDateMillis,
                    createdAt = existingTx?.createdAt ?: System.currentTimeMillis()
                )

                repository.saveTransaction(tx)
                _activeQuickEntry.value = null

                if (existingTx != null) {
                    postUndoable(UndoableOperation.EditedTransaction(existingTx, "Transaction updated"))
                } else {
                    postUndoable(UndoableOperation.AddedTransaction(tx, "Transaction saved"))
                }
            } catch (_: Exception) {
                showBannerMessage("This transaction could not be saved. Please check the information and try again.")
            }
        }
    }

    fun deleteTransactionWithUndo(txId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = repository.deleteTransaction(txId)
            if (deleted != null) {
                postUndoable(UndoableOperation.DeletedTransaction(deleted, "Transaction deleted"))
            }
        }
    }

    fun performUndo() {
        val op = _undoOperation.value ?: return
        undoDismissJob?.cancel()
        _undoOperation.value = null
        viewModelScope.launch(Dispatchers.IO) {
            when (op) {
                is UndoableOperation.AddedTransaction -> {
                    repository.deleteTransaction(op.tx.id)
                }
                is UndoableOperation.DeletedTransaction -> {
                    repository.saveTransaction(op.tx)
                }
                is UndoableOperation.EditedTransaction -> {
                    repository.saveTransaction(op.previousTx)
                }
            }
        }
    }

    fun dismissUndo() {
        undoDismissJob?.cancel()
        _undoOperation.value = null
    }

    private fun postUndoable(op: UndoableOperation) {
        undoDismissJob?.cancel()
        _undoOperation.value = op
        undoDismissJob = viewModelScope.launch {
            delay(6500L)
            _undoOperation.value = null
        }
    }

    fun showBannerMessage(msg: String?) {
        _statusBannerMessage.value = msg
        if (msg != null) {
            viewModelScope.launch {
                delay(4000L)
                if (_statusBannerMessage.value == msg) {
                    _statusBannerMessage.value = null
                }
            }
        }
    }

    // --- Shop CRUD ---
    fun saveShop(
        existingShop: ShopEntity?,
        name: String,
        ownerName: String,
        phone: String,
        address: String,
        note: String,
        colorHex: String
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val shop = existingShop?.copy(
                name = trimmedName,
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                note = note.trim(),
                colorHex = colorHex
            ) ?: ShopEntity(
                id = "shop_${UUID.randomUUID().toString().take(8)}",
                name = trimmedName,
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                note = note.trim(),
                colorHex = colorHex
            )
            repository.saveShop(shop)
        }
    }

    fun deleteShop(shopId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedShopId.value == shopId) {
                _selectedShopId.value = null
            }
            repository.deleteShop(shopId)
        }
    }

    // --- Person CRUD ---
    fun savePerson(
        existingPerson: PersonEntity?,
        name: String,
        phone: String,
        note: String,
        avatarColorHex: String
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val person = existingPerson?.copy(
                name = trimmedName,
                phone = phone.trim(),
                note = note.trim(),
                avatarColorHex = avatarColorHex
            ) ?: PersonEntity(
                id = "person_${UUID.randomUUID().toString().take(8)}",
                name = trimmedName,
                phone = phone.trim(),
                note = note.trim(),
                avatarColorHex = avatarColorHex
            )
            repository.savePerson(person)
        }
    }

    fun deletePerson(personId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedPersonId.value == personId) {
                _selectedPersonId.value = null
            }
            repository.deletePerson(personId)
        }
    }

    // --- Loan CRUD ---
    fun createLoan(
        isLentByMe: Boolean,
        personId: String?,
        personName: String,
        principalPaisa: Long,
        interestPaisa: Long,
        interestRatePercent: String,
        installmentCount: Int,
        startDateMillis: Long,
        dueDateMillis: Long?,
        note: String
    ) {
        if (principalPaisa <= 0L || personName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            var resolvedPersonId = personId
            val cleanName = personName.trim()
            if (resolvedPersonId == null) {
                val existing = uiState.value.people.find { it.name.equals(cleanName, ignoreCase = true) }
                if (existing != null) {
                    resolvedPersonId = existing.id
                } else {
                    val created = PersonEntity(
                        id = "person_${UUID.randomUUID().toString().take(8)}",
                        name = cleanName
                    )
                    repository.savePerson(created)
                    resolvedPersonId = created.id
                }
            }
            val loan = LoanEntity(
                id = "loan_${UUID.randomUUID().toString().take(8)}",
                isLentByMe = isLentByMe,
                personId = resolvedPersonId,
                personName = cleanName,
                principalPaisa = principalPaisa,
                interestPaisa = interestPaisa,
                interestRatePercent = interestRatePercent.trim(),
                installmentCount = installmentCount.coerceAtLeast(1),
                startDateMillis = startDateMillis,
                dueDateMillis = dueDateMillis,
                note = note.trim()
            )
            repository.createLoanWithInitialTransaction(loan, recordCashMovement = true)
            showBannerMessage("Loan saved & balance updated")
        }
    }

    fun deleteLoan(loanId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLoanAndRelatedTransactions(loanId)
            showBannerMessage("Loan deleted & balances recalculated")
        }
    }

    // --- Category CRUD ---
    fun saveCategory(
        existing: CategoryEntity?,
        name: String,
        nameBn: String,
        type: String,
        iconName: String,
        colorHex: String
    ) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val cat = existing?.copy(
                name = clean,
                nameBn = nameBn.trim().ifEmpty { clean },
                type = type,
                iconName = iconName,
                colorHex = colorHex
            ) ?: CategoryEntity(
                id = "cat_${UUID.randomUUID().toString().take(8)}",
                name = clean,
                nameBn = nameBn.trim().ifEmpty { clean },
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                sortOrder = uiState.value.categories.count { it.type == type } + 1
            )
            repository.saveCategory(cat)
        }
    }

    fun moveCategoryOrder(category: CategoryEntity, moveUp: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val sameType = uiState.value.categories.filter { it.type == category.type }.sortedBy { it.sortOrder }
            val index = sameType.indexOfFirst { it.id == category.id }
            if (index == -1) return@launch
            val swapIndex = if (moveUp) index - 1 else index + 1
            if (swapIndex !in sameType.indices) return@launch
            val neighbor = sameType[swapIndex]
            repository.reorderCategory(category, neighbor.sortOrder)
            repository.reorderCategory(neighbor, category.sortOrder)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(categoryId)
        }
    }

    // --- Settings & Customization Updates ---
    fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(transform)
        }
    }

    fun toggleDashboardCardVisibility(cardId: String) {
        updateSettings { current ->
            val hiddenSet = current.hiddenDashboardCards.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            if (cardId in hiddenSet) {
                hiddenSet.remove(cardId)
            } else {
                hiddenSet.add(cardId)
            }
            current.copy(hiddenDashboardCards = hiddenSet.joinToString(","))
        }
    }

    fun moveDashboardCard(cardId: String, moveUp: Boolean) {
        updateSettings { current ->
            val list = current.allDashboardCardsOrdered().toMutableList()
            val idx = list.indexOf(cardId)
            if (idx == -1) return@updateSettings current
            val targetIdx = if (moveUp) idx - 1 else idx + 1
            if (targetIdx !in list.indices) return@updateSettings current
            val temp = list[idx]
            list[idx] = list[targetIdx]
            list[targetIdx] = temp
            current.copy(dashboardCardsOrder = list.joinToString(","))
        }
    }

    // --- Backup & Restore Workflows ---
    fun exportJsonBackup(context: Context, shareAfterExport: Boolean = true, onReadyToSaveExternally: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (file, content) = repository.createJsonBackupFile()
                showBannerMessage("Backup saved: ${file.name}")
                if (shareAfterExport) {
                    BackupManager.shareFile(
                        context = context,
                        file = file,
                        mimeType = "application/json",
                        chooserTitle = "Export Hisab Backup (${file.name})"
                    )
                }
                onReadyToSaveExternally?.invoke(content)
            } catch (_: Exception) {
                showBannerMessage("Could not generate backup file. Please try again.")
            }
        }
    }

    fun exportCsvTransactions(context: Context, shareAfterExport: Boolean = true, onReadyToSaveExternally: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (file, content) = repository.createTransactionsCsvFile()
                showBannerMessage("CSV exported: ${file.name}")
                if (shareAfterExport) {
                    BackupManager.shareFile(
                        context = context,
                        file = file,
                        mimeType = "text/csv",
                        chooserTitle = "Export Transactions CSV (${file.name})"
                    )
                }
                onReadyToSaveExternally?.invoke(content)
            } catch (_: Exception) {
                showBannerMessage("Could not generate CSV file. Please try again.")
            }
        }
    }

    fun inspectBackupForImport(rawJson: String) {
        when (val res = repository.validateBackupJson(rawJson)) {
            is BackupValidationResult.Valid -> {
                _pendingImportPreview.value = res.payload
            }
            is BackupValidationResult.Invalid -> {
                _pendingImportPreview.value = null
                showBannerMessage(res.userFriendlyError)
            }
        }
    }

    fun cancelPendingImport() {
        _pendingImportPreview.value = null
    }

    fun confirmFullRestore() {
        val payload = _pendingImportPreview.value ?: return
        _pendingImportPreview.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.performFullRestore(payload)
                showBannerMessage("Full backup restored (${payload.transactions.size} transactions)")
            } catch (_: Exception) {
                showBannerMessage("Restore failed. Existing data was kept safe.")
            }
        }
    }

    fun confirmMergeImport() {
        val payload = _pendingImportPreview.value ?: return
        _pendingImportPreview.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newlyInserted = repository.performMergeImport(payload)
                showBannerMessage("Merge complete: $newlyInserted new transactions added, duplicates skipped.")
            } catch (_: Exception) {
                showBannerMessage("Merge import could not be completed.")
            }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDemoDataIfRequested()
            showBannerMessage("Sample হিসাব data loaded (Rahman Store, Rahim, Karim)")
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllUserData()
            showBannerMessage("All transactions, shops, people & loans cleared")
        }
    }

    fun getDatabaseSizeBytes(): Long = repository.getDatabaseSizeBytes()

    fun getSavedLocalBackups(): List<File> = repository.getSavedLocalBackups()

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = HisabDatabase.getInstance(appContext)
            val repo = HisabRepository(db.hisabDao(), appContext.applicationContext)
            return HisabViewModel(repo) as T
        }
    }
}
