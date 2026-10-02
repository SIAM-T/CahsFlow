package com.example.data.repository

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.ValidatedBackupPayload
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BackupMetadataEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.HisabDao
import com.example.data.local.HisabDatabase
import com.example.data.local.LoanEntity
import com.example.data.local.NoteChecklistItem
import com.example.data.local.NotePriority
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.SmartNoteEntity
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class HisabRepository(
    private val dao: HisabDao,
    private val appContext: Context
) {
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeCategories()
    val peopleFlow: Flow<List<PersonEntity>> = dao.observePeople()
    val shopsFlow: Flow<List<ShopEntity>> = dao.observeShops()
    val loansFlow: Flow<List<LoanEntity>> = dao.observeLoans()
    val settingsFlow: Flow<AppSettingsEntity?> = dao.observeSettings()
    val backupMetadataFlow: Flow<BackupMetadataEntity?> = dao.observeBackupMetadata()
    val notesFlow: Flow<List<SmartNoteEntity>> = dao.observeSmartNotes()

    suspend fun ensureInitialized() {
        val currentSettings = dao.getSettingsOnce()
        if (currentSettings == null) {
            dao.saveSettings(AppSettingsEntity(id = 1))
        }
        val currentCategories = dao.getAllCategoriesOnce()
        if (currentCategories.isEmpty()) {
            dao.insertCategories(HisabDatabase.defaultCategories())
        }
        val currentBackupMeta = dao.getBackupMetadataOnce()
        if (currentBackupMeta == null) {
            dao.saveBackupMetadata(BackupMetadataEntity(id = 1))
        }
    }

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        val current = dao.getSettingsOnce() ?: AppSettingsEntity(id = 1)
        dao.saveSettings(transform(current))
    }

    // --- Smart Transaction Recording ---
    suspend fun saveTransaction(tx: TransactionEntity): TransactionEntity {
        require(tx.amountPaisa > 0L) { "Amount must be greater than zero" }
        dao.insertTransaction(tx)

        // Update Smart UX recency & frequency metadata
        if (tx.categoryId != null) {
            dao.incrementCategoryUsage(tx.categoryId)
        }
        if (tx.personId != null) {
            dao.touchPersonInteraction(tx.personId, System.currentTimeMillis())
        }
        if (tx.shopId != null) {
            val shop = dao.getShopById(tx.shopId)
            if (shop != null) {
                val updatedProducts = if (tx.productOrDescription.isNotBlank() &&
                    tx.type == TransactionType.SHOP_DUE.name
                ) {
                    val existing = shop.recentProductList().toMutableList()
                    existing.removeAll { it.equals(tx.productOrDescription.trim(), ignoreCase = true) }
                    existing.add(0, tx.productOrDescription.trim())
                    existing.take(10).joinToString(",")
                } else {
                    shop.recentProductsCsv
                }
                dao.updateShop(
                    shop.copy(
                        recentProductsCsv = updatedProducts,
                        lastInteractionAt = System.currentTimeMillis()
                    )
                )
            }
        }
        return tx
    }

    suspend fun deleteTransaction(id: String): TransactionEntity? {
        val existing = dao.getTransactionById(id)
        if (existing != null) {
            dao.deleteTransactionById(id)
        }
        return existing
    }

    // --- Shops ---
    suspend fun saveShop(shop: ShopEntity): ShopEntity {
        dao.insertShop(shop)
        return shop
    }

    suspend fun deleteShop(shopId: String) {
        dao.deleteShopById(shopId)
    }

    // --- People ---
    suspend fun savePerson(person: PersonEntity): PersonEntity {
        dao.insertPerson(person)
        return person
    }

    suspend fun deletePerson(personId: String) {
        dao.deletePersonById(personId)
    }

    // --- Categories ---
    suspend fun saveCategory(category: CategoryEntity) {
        dao.insertCategory(category)
    }

    suspend fun deleteCategory(categoryId: String) {
        dao.deleteCategoryById(categoryId)
    }

    suspend fun reorderCategory(category: CategoryEntity, newSortOrder: Int) {
        dao.updateCategory(category.copy(sortOrder = newSortOrder))
    }

    // --- Loans ---
    suspend fun createLoanWithInitialTransaction(
        loan: LoanEntity,
        recordCashMovement: Boolean = true
    ): LoanEntity {
        dao.insertLoan(loan)
        if (loan.personId != null) {
            dao.touchPersonInteraction(loan.personId, System.currentTimeMillis())
        }
        if (recordCashMovement) {
            val txType = if (loan.isLentByMe) TransactionType.LOAN_GIVEN else TransactionType.LOAN_RECEIVED
            val tx = TransactionEntity(
                id = "tx_loan_init_${loan.id}",
                type = txType.name,
                direction = txType.defaultDirection.name,
                amountPaisa = loan.principalPaisa,
                timestamp = loan.startDateMillis,
                personId = loan.personId,
                personName = loan.personName,
                loanId = loan.id,
                productOrDescription = if (loan.isLentByMe) "Loan given to ${loan.personName}" else "Loan taken from ${loan.personName}",
                note = loan.note,
                dueDateMillis = loan.dueDateMillis
            )
            dao.insertTransaction(tx)
        }
        return loan
    }

    suspend fun updateLoan(loan: LoanEntity) {
        dao.updateLoan(loan)
    }

    suspend fun deleteLoanAndRelatedTransactions(loanId: String) {
        val allTx = dao.getAllTransactionsOnce()
        for (tx in allTx) {
            if (tx.loanId == loanId) {
                dao.deleteTransactionById(tx.id)
            }
        }
        dao.deleteLoanById(loanId)
    }

    // --- Smart Notes & Checklists ---
    suspend fun saveSmartNote(note: SmartNoteEntity): SmartNoteEntity {
        dao.insertSmartNote(note)
        return note
    }

    suspend fun deleteSmartNote(noteId: String) {
        dao.deleteSmartNoteById(noteId)
    }

    // --- Backup & Restore ---
    suspend fun createJsonBackupFile(): Pair<File, String> {
        val now = System.currentTimeMillis()
        val settings = dao.getSettingsOnce() ?: AppSettingsEntity(id = 1)
        val categories = dao.getAllCategoriesOnce()
        val people = dao.getAllPeopleOnce()
        val shops = dao.getAllShopsOnce()
        val loans = dao.getAllLoansOnce()
        val transactions = dao.getAllTransactionsOnce()
        val notes = dao.getAllSmartNotesOnce()

        val jsonContent = BackupManager.exportToJsonString(
            settings = settings,
            categories = categories,
            people = people,
            shops = shops,
            loans = loans,
            transactions = transactions,
            notes = notes,
            nowMillis = now
        )
        val fileName = BackupManager.defaultBackupFileName(now)
        val file = BackupManager.writeBackupFileToInternalStorage(appContext, fileName, jsonContent)

        dao.saveBackupMetadata(
            BackupMetadataEntity(
                id = 1,
                lastBackupTimestamp = now,
                lastBackupFileName = fileName,
                lastBackupTransactionCount = transactions.size,
                lastBackupSizeBytes = file.length()
            )
        )
        return Pair(file, jsonContent)
    }

    suspend fun createTransactionsCsvFile(): Pair<File, String> {
        val now = System.currentTimeMillis()
        val settings = dao.getSettingsOnce() ?: AppSettingsEntity(id = 1)
        val transactions = dao.getAllTransactionsOnce()
        val csvContent = BackupManager.exportTransactionsToCsv(transactions, settings.currencySymbol)
        val fileName = BackupManager.defaultCsvFileName(now)
        val file = BackupManager.writeBackupFileToInternalStorage(appContext, fileName, csvContent)
        return Pair(file, csvContent)
    }

    fun validateBackupJson(rawJson: String): BackupValidationResult {
        return BackupManager.validateAndParseBackup(rawJson)
    }

    suspend fun performFullRestore(payload: ValidatedBackupPayload) {
        val cats = payload.categories.ifEmpty { HisabDatabase.defaultCategories() }
        dao.fullRestoreDatabase(
            settings = payload.settings,
            categories = cats,
            people = payload.people,
            shops = payload.shops,
            loans = payload.loans,
            transactions = payload.transactions,
            notes = payload.notes
        )
    }

    suspend fun performMergeImport(payload: ValidatedBackupPayload): Int {
        return dao.mergeRestoreDatabase(
            categories = payload.categories,
            people = payload.people,
            shops = payload.shops,
            loans = payload.loans,
            transactions = payload.transactions,
            notes = payload.notes
        )
    }

    fun getDatabaseSizeBytes(): Long {
        return BackupManager.getDatabaseSizeBytes(appContext, HisabDatabase.DATABASE_NAME)
    }

    fun getSavedLocalBackups(): List<File> {
        return BackupManager.listSavedLocalBackups(appContext)
    }

    /**
     * Optional helper invoked only when user explicitly taps "Load Sample Demo Data"
     * in Settings -> Data Management or First Launch demo toggle.
     */
    suspend fun seedDemoDataIfRequested() {
        val now = System.currentTimeMillis()
        val hour = 3600_000L
        val day = 24 * hour

        val rahmanShop = ShopEntity(
            id = "shop_rahman_store",
            name = "Rahman Store",
            ownerName = "Abdul Rahman",
            phone = "01711-223344",
            address = "Mirpur 10, Dhaka",
            note = "Wholesale grocery & rice supplier",
            colorHex = "#F59E0B",
            recentProductsCsv = "Rice,Oil,Biscuits,Sugar,Lentils",
            lastInteractionAt = now - hour
        )
        val maStore = ShopEntity(
            id = "shop_bismillah_mart",
            name = "Bismillah General Store",
            ownerName = "Rafiqul Islam",
            phone = "01819-556677",
            address = "Dhanmondi, Dhaka",
            note = "Monthly provision shop",
            colorHex = "#10B981",
            recentProductsCsv = "Flour,Tea,Milk,Salt,Spices",
            lastInteractionAt = now - 3 * hour
        )
        dao.insertShops(listOf(rahmanShop, maStore))

        val rahim = PersonEntity(
            id = "person_rahim",
            name = "Rahim",
            phone = "01712-998877",
            note = "Colleague & friend",
            avatarColorHex = "#06B6D4",
            lastInteractionAt = now - 2 * hour
        )
        val karim = PersonEntity(
            id = "person_karim",
            name = "Karim",
            phone = "01911-334455",
            note = "Business partner",
            avatarColorHex = "#8B5CF6",
            lastInteractionAt = now - 4 * hour
        )
        dao.insertPeople(listOf(rahim, karim))

        val loanKarim = LoanEntity(
            id = "loan_karim_1",
            isLentByMe = false,
            personId = karim.id,
            personName = karim.name,
            principalPaisa = 10000_00L, // ৳10,000
            interestPaisa = 0L,
            installmentCount = 4,
            startDateMillis = now - 5 * day,
            dueDateMillis = now + 25 * day,
            note = "Short-term equipment loan"
        )
        val loanRahim = LoanEntity(
            id = "loan_rahim_1",
            isLentByMe = true,
            personId = rahim.id,
            personName = rahim.name,
            principalPaisa = 15000_00L, // ৳15,000
            interestPaisa = 0L,
            installmentCount = 3,
            startDateMillis = now - 7 * day,
            dueDateMillis = now + 20 * day,
            note = "Emergency personal loan"
        )
        dao.insertLoans(listOf(loanKarim, loanRahim))

        val sampleTxs = listOf(
            // Today Income ৳5,000
            TransactionEntity(
                id = "tx_demo_inc_1",
                type = TransactionType.INCOME.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 5000_00L,
                timestamp = now - 5 * hour,
                categoryId = "cat_inc_business",
                categoryName = "Business Income",
                productOrDescription = "Daily store consultancy & sales",
                note = "Cash received"
            ),
            // Today Expense ৳200 (Food - Lunch)
            TransactionEntity(
                id = "tx_demo_exp_1",
                type = TransactionType.EXPENSE.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 200_00L,
                timestamp = now - 4 * hour,
                categoryId = "cat_exp_food",
                categoryName = "Food",
                productOrDescription = "Lunch",
                note = "Kacchi & borhani"
            ),
            // Rahman Store dues & payment matching prompt examples
            TransactionEntity(
                id = "tx_demo_shop_1",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 500_00L,
                timestamp = now - 2 * day,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Rice",
                note = "Miniket 10kg bag"
            ),
            TransactionEntity(
                id = "tx_demo_shop_2",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 300_00L,
                timestamp = now - 2 * day + hour,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Oil",
                note = "Soybean oil 2L"
            ),
            TransactionEntity(
                id = "tx_demo_shop_3",
                type = TransactionType.SHOP_PAYMENT.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 200_00L,
                timestamp = now - day,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Payment received",
                note = "Partial cash settlement"
            ),
            TransactionEntity(
                id = "tx_demo_shop_4",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 650_00L,
                timestamp = now - 2 * hour,
                shopId = rahmanShop.id,
                shopName = rahmanShop.name,
                productOrDescription = "Biscuits",
                note = "Wholesale carton"
            ),
            // Rahim lending & partial payment (Lent 1,000, Received 500 -> Remaining 500)
            TransactionEntity(
                id = "tx_demo_rahim_1",
                type = TransactionType.LEND.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 1000_00L,
                timestamp = now - 3 * day,
                personId = rahim.id,
                personName = rahim.name,
                productOrDescription = "Lent to Rahim",
                note = "Cash given"
            ),
            TransactionEntity(
                id = "tx_demo_rahim_2",
                type = TransactionType.RECEIVE_PAYMENT.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 500_00L,
                timestamp = now - hour,
                personId = rahim.id,
                personName = rahim.name,
                productOrDescription = "Payment received",
                note = "bKash transfer"
            ),
            // Karim borrowing & repayment (Borrowed 5,000, Repaid 2,000 -> Remaining 3,000)
            TransactionEntity(
                id = "tx_demo_karim_1",
                type = TransactionType.BORROW.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 5000_00L,
                timestamp = now - 4 * day,
                personId = karim.id,
                personName = karim.name,
                productOrDescription = "Borrowed from Karim",
                note = "Inventory top-up"
            ),
            TransactionEntity(
                id = "tx_demo_karim_2",
                type = TransactionType.MAKE_PAYMENT.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 2000_00L,
                timestamp = now - 2 * day,
                personId = karim.id,
                personName = karim.name,
                productOrDescription = "Repayment to Karim",
                note = "Cash paid"
            ),
            // Loan transactions (Karim loan 10,000, repaid 3,000 -> 7,000; Rahim loan 15,000, repaid 5,000 -> 10,000)
            TransactionEntity(
                id = "tx_loan_init_${loanKarim.id}",
                type = TransactionType.LOAN_RECEIVED.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 10000_00L,
                timestamp = now - 5 * day,
                personId = karim.id,
                personName = karim.name,
                loanId = loanKarim.id,
                productOrDescription = "Loan taken from Karim"
            ),
            TransactionEntity(
                id = "tx_loan_rep_${loanKarim.id}",
                type = TransactionType.LOAN_PAYMENT.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 3000_00L,
                timestamp = now - 2 * day,
                personId = karim.id,
                personName = karim.name,
                loanId = loanKarim.id,
                productOrDescription = "Loan installment paid to Karim"
            ),
            TransactionEntity(
                id = "tx_loan_init_${loanRahim.id}",
                type = TransactionType.LOAN_GIVEN.name,
                direction = TransactionDirection.OUTFLOW.name,
                amountPaisa = 15000_00L,
                timestamp = now - 7 * day,
                personId = rahim.id,
                personName = rahim.name,
                loanId = loanRahim.id,
                productOrDescription = "Loan given to Rahim"
            ),
            TransactionEntity(
                id = "tx_loan_rep_${loanRahim.id}",
                type = TransactionType.LOAN_REPAYMENT.name,
                direction = TransactionDirection.INFLOW.name,
                amountPaisa = 5000_00L,
                timestamp = now - 3 * day,
                personId = rahim.id,
                personName = rahim.name,
                loanId = loanRahim.id,
                productOrDescription = "Loan repayment received from Rahim"
            )
        )
        dao.insertTransactions(sampleTxs)

        val sampleNotes = listOf(
            SmartNoteEntity(
                id = "note_demo_bazar",
                title = "Monthly Bazar & Grocery List",
                content = "Wholesale market purchase plan — convert to Expense when done",
                checklistJson = SmartNoteEntity.serializeChecklist(
                    listOf(
                        NoteChecklistItem("item_1", "Miniket Rice (25kg)", true, 1650_00L),
                        NoteChecklistItem("item_2", "Soybean Oil (5L)", true, 850_00L),
                        NoteChecklistItem("item_3", "Red Lentils & Spices", false, 450_00L),
                        NoteChecklistItem("item_4", "Tea, Sugar & Milk Powder", false, 600_00L)
                    )
                ),
                colorHex = "#10B981",
                priority = NotePriority.HIGH.name,
                labelsCsv = "Bazar,Shopping,Monthly",
                isPinned = true,
                linkedTransactionType = TransactionType.EXPENSE.name,
                reminderMillis = now + 2 * hour,
                updatedAt = now - 30 * 60_000L
            ),
            SmartNoteEntity(
                id = "note_demo_shop_supply",
                title = "Rahman Store Restock Order",
                content = "Deliver on Thursday morning and record in Shop Due",
                checklistJson = SmartNoteEntity.serializeChecklist(
                    listOf(
                        NoteChecklistItem("item_s1", "2 sacks Nazirshail Rice", false, 3200_00L),
                        NoteChecklistItem("item_s2", "1 carton Biscuits", false, 650_00L)
                    )
                ),
                colorHex = "#F59E0B",
                priority = NotePriority.URGENT.name,
                labelsCsv = "Shop Due,Rahman Store",
                isPinned = true,
                linkedTransactionType = TransactionType.SHOP_DUE.name,
                linkedEntityId = rahmanShop.id,
                linkedEntityName = rahmanShop.name,
                reminderMillis = now + 4 * hour,
                updatedAt = now - hour
            )
        )
        dao.insertSmartNotes(sampleNotes)
    }

    suspend fun clearAllUserData() {
        dao.deleteAllTransactions()
        dao.deleteAllLoans()
        dao.deleteAllShops()
        dao.deleteAllPeople()
        dao.deleteAllSmartNotes()
    }
}
