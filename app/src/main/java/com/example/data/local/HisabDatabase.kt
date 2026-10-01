package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
        ShopEntity::class,
        LoanEntity::class,
        AppSettingsEntity::class,
        BackupMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun hisabDao(): HisabDao

    companion object {
        const val DATABASE_NAME = "hisab_personal_finance.db"

        @Volatile
        private var INSTANCE: HisabDatabase? = null

        fun getInstance(context: Context): HisabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HisabDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun defaultCategories(): List<CategoryEntity> = listOf(
            // Income Categories
            CategoryEntity(
                id = "cat_inc_salary",
                name = "Salary",
                nameBn = "বেতন",
                type = "INCOME",
                iconName = "Work",
                colorHex = "#10B981",
                sortOrder = 1
            ),
            CategoryEntity(
                id = "cat_inc_business",
                name = "Business Income",
                nameBn = "ব্যবসায়িক আয়",
                type = "INCOME",
                iconName = "Store",
                colorHex = "#06B6D4",
                sortOrder = 2
            ),
            CategoryEntity(
                id = "cat_inc_freelance",
                name = "Freelance Income",
                nameBn = "ফ্রিল্যান্সিং আয়",
                type = "INCOME",
                iconName = "Laptop",
                colorHex = "#8B5CF6",
                sortOrder = 3
            ),
            CategoryEntity(
                id = "cat_inc_gift",
                name = "Gift",
                nameBn = "উপহার",
                type = "INCOME",
                iconName = "CardGiftcard",
                colorHex = "#F59E0B",
                sortOrder = 4
            ),
            CategoryEntity(
                id = "cat_inc_other",
                name = "Other Income",
                nameBn = "অন্যান্য আয়",
                type = "INCOME",
                iconName = "Savings",
                colorHex = "#14B8A6",
                sortOrder = 5
            ),
            // Expense Categories
            CategoryEntity(
                id = "cat_exp_food",
                name = "Food",
                nameBn = "খাবার ও বাজার",
                type = "EXPENSE",
                iconName = "Restaurant",
                colorHex = "#F97316",
                sortOrder = 1
            ),
            CategoryEntity(
                id = "cat_exp_transport",
                name = "Transport",
                nameBn = "যাতায়াত",
                type = "EXPENSE",
                iconName = "DirectionsBus",
                colorHex = "#3B82F6",
                sortOrder = 2
            ),
            CategoryEntity(
                id = "cat_exp_shopping",
                name = "Shopping",
                nameBn = "কেনাকাটা",
                type = "EXPENSE",
                iconName = "ShoppingBag",
                colorHex = "#EC4899",
                sortOrder = 3
            ),
            CategoryEntity(
                id = "cat_exp_bills",
                name = "Bills",
                nameBn = "বিল ও ভাড়া",
                type = "EXPENSE",
                iconName = "ReceiptLong",
                colorHex = "#EAB308",
                sortOrder = 4
            ),
            CategoryEntity(
                id = "cat_exp_education",
                name = "Education",
                nameBn = "শিক্ষা",
                type = "EXPENSE",
                iconName = "School",
                colorHex = "#6366F1",
                sortOrder = 5
            ),
            CategoryEntity(
                id = "cat_exp_medical",
                name = "Medical",
                nameBn = "চিকিৎসা ও ওষুধ",
                type = "EXPENSE",
                iconName = "LocalHospital",
                colorHex = "#EF4444",
                sortOrder = 6
            ),
            CategoryEntity(
                id = "cat_exp_entertainment",
                name = "Entertainment",
                nameBn = "বিনোদন",
                type = "EXPENSE",
                iconName = "Movie",
                colorHex = "#A855F7",
                sortOrder = 7
            ),
            CategoryEntity(
                id = "cat_exp_family",
                name = "Family",
                nameBn = "পরিবার",
                type = "EXPENSE",
                iconName = "FamilyRestroom",
                colorHex = "#14B8A6",
                sortOrder = 8
            ),
            CategoryEntity(
                id = "cat_exp_mobile",
                name = "Mobile/Internet",
                nameBn = "মোবাইল ও ইন্টারনেট",
                type = "EXPENSE",
                iconName = "Wifi",
                colorHex = "#0EA5E9",
                sortOrder = 9
            ),
            CategoryEntity(
                id = "cat_exp_other",
                name = "Other",
                nameBn = "অন্যান্য ব্যয়",
                type = "EXPENSE",
                iconName = "MoreHoriz",
                colorHex = "#64748B",
                sortOrder = 10
            )
        )
    }
}
