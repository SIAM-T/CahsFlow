package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HisabDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, createdAt DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, createdAt DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, createdAt DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeTransactionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCountOnce(): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionsIgnoreDuplicates(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC, usageCount DESC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoriesIgnoreDuplicates(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET usageCount = usageCount + 1 WHERE id = :categoryId")
    suspend fun incrementCategoryUsage(categoryId: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    // --- People ---
    @Query("SELECT * FROM people ORDER BY lastInteractionAt DESC, name ASC")
    fun observePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY lastInteractionAt DESC, name ASC")
    suspend fun getAllPeopleOnce(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPeopleIgnoreDuplicates(people: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("UPDATE people SET lastInteractionAt = :timestamp WHERE id = :personId")
    suspend fun touchPersonInteraction(personId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePersonById(id: String)

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

    // --- Shops ---
    @Query("SELECT * FROM shops ORDER BY lastInteractionAt DESC, name ASC")
    fun observeShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops ORDER BY lastInteractionAt DESC, name ASC")
    suspend fun getAllShopsOnce(): List<ShopEntity>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    suspend fun getShopById(id: String): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShops(shops: List<ShopEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShopsIgnoreDuplicates(shops: List<ShopEntity>)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("DELETE FROM shops WHERE id = :id")
    suspend fun deleteShopById(id: String)

    @Query("DELETE FROM shops")
    suspend fun deleteAllShops()

    // --- Loans ---
    @Query("SELECT * FROM loans ORDER BY startDateMillis DESC")
    fun observeLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY startDateMillis DESC")
    suspend fun getAllLoansOnce(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: String): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLoansIgnoreDuplicates(loans: List<LoanEntity>)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: String)

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()

    // --- App Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)

    // --- Backup Metadata ---
    @Query("SELECT * FROM backup_metadata WHERE id = 1 LIMIT 1")
    fun observeBackupMetadata(): Flow<BackupMetadataEntity?>

    @Query("SELECT * FROM backup_metadata WHERE id = 1 LIMIT 1")
    suspend fun getBackupMetadataOnce(): BackupMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBackupMetadata(metadata: BackupMetadataEntity)

    // --- Atomic Restore Operations ---
    @Transaction
    suspend fun fullRestoreDatabase(
        settings: AppSettingsEntity,
        categories: List<CategoryEntity>,
        people: List<PersonEntity>,
        shops: List<ShopEntity>,
        loans: List<LoanEntity>,
        transactions: List<TransactionEntity>
    ) {
        deleteAllTransactions()
        deleteAllLoans()
        deleteAllShops()
        deleteAllPeople()
        deleteAllCategories()

        saveSettings(settings.copy(id = 1, isFirstLaunchCompleted = true))
        insertCategories(categories)
        insertPeople(people)
        insertShops(shops)
        insertLoans(loans)
        insertTransactions(transactions)
    }

    @Transaction
    suspend fun mergeRestoreDatabase(
        categories: List<CategoryEntity>,
        people: List<PersonEntity>,
        shops: List<ShopEntity>,
        loans: List<LoanEntity>,
        transactions: List<TransactionEntity>
    ): Int {
        insertCategoriesIgnoreDuplicates(categories)
        insertPeopleIgnoreDuplicates(people)
        insertShopsIgnoreDuplicates(shops)
        insertLoansIgnoreDuplicates(loans)
        val results = insertTransactionsIgnoreDuplicates(transactions)
        return results.count { it != -1L }
    }
}
