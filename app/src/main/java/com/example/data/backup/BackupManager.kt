package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.LoanEntity
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.SmartNoteEntity
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.MoneyUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ValidatedBackupPayload(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val formattedDate: String,
    val settings: AppSettingsEntity,
    val categories: List<CategoryEntity>,
    val people: List<PersonEntity>,
    val shops: List<ShopEntity>,
    val loans: List<LoanEntity>,
    val transactions: List<TransactionEntity>,
    val notes: List<SmartNoteEntity> = emptyList(),
    val duplicateIdsIgnoredInFile: Int = 0
)

sealed class BackupValidationResult {
    data class Valid(val payload: ValidatedBackupPayload) : BackupValidationResult()
    data class Invalid(val userFriendlyError: String) : BackupValidationResult()
}

object BackupManager {

    const val CURRENT_SCHEMA_VERSION = 1

    fun defaultBackupFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "HisabBackup_${sdf.format(Date(nowMillis))}.json"
    }

    fun defaultCsvFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "HisabTransactions_${sdf.format(Date(nowMillis))}.csv"
    }

    /**
     * Generates a complete, human-readable JSON backup string containing all entities and preferences.
     */
    fun exportToJsonString(
        settings: AppSettingsEntity,
        categories: List<CategoryEntity>,
        people: List<PersonEntity>,
        shops: List<ShopEntity>,
        loans: List<LoanEntity>,
        transactions: List<TransactionEntity>,
        notes: List<SmartNoteEntity> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("app", "Hisab")
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("exportedAtMillis", nowMillis)
        root.put("exportedDate", MoneyUtils.formatDateFull(nowMillis, "en"))

        // 1. Settings & Theme/App Preferences
        val settingsObj = JSONObject().apply {
            put("currencySymbol", settings.currencySymbol)
            put("currencyCode", settings.currencyCode)
            put("openingCashBalancePaisa", settings.openingCashBalancePaisa)
            put("themeMode", settings.themeMode)
            put("accentColorHex", settings.accentColorHex)
            put("backgroundStyle", settings.backgroundStyle)
            put("glassTransparency", settings.glassTransparency)
            put("glassBlurAmount", settings.glassBlurAmount)
            put("cardOpacity", settings.cardOpacity)
            put("uiDensity", settings.uiDensity)
            put("languageCode", settings.languageCode)
            put("dashboardCardsOrder", settings.dashboardCardsOrder)
            put("hiddenDashboardCards", settings.hiddenDashboardCards)
            put("dashboardLayoutStyle", settings.dashboardLayoutStyle)
            put("notificationsEnabled", settings.notificationsEnabled)
            put("loanReminderEnabled", settings.loanReminderEnabled)
            put("personDueReminderEnabled", settings.personDueReminderEnabled)
            put("shopDueReminderEnabled", settings.shopDueReminderEnabled)
        }
        root.put("settings", settingsObj)

        // 2. Categories
        val categoriesArr = JSONArray()
        for (c in categories) {
            categoriesArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("nameBn", c.nameBn)
                    put("type", c.type)
                    put("iconName", c.iconName)
                    put("colorHex", c.colorHex)
                    put("sortOrder", c.sortOrder)
                    put("usageCount", c.usageCount)
                }
            )
        }
        root.put("categories", categoriesArr)

        // 3. People / Customers
        val peopleArr = JSONArray()
        for (p in people) {
            peopleArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("phone", p.phone)
                    put("note", p.note)
                    put("avatarColorHex", p.avatarColorHex)
                    put("iconName", p.iconName)
                    put("lastInteractionAt", p.lastInteractionAt)
                    put("createdAt", p.createdAt)
                }
            )
        }
        root.put("people", peopleArr)

        // 4. Shops
        val shopsArr = JSONArray()
        for (s in shops) {
            shopsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("ownerName", s.ownerName)
                    put("phone", s.phone)
                    put("address", s.address)
                    put("note", s.note)
                    put("colorHex", s.colorHex)
                    put("recentProductsCsv", s.recentProductsCsv)
                    put("lastInteractionAt", s.lastInteractionAt)
                    put("createdAt", s.createdAt)
                }
            )
        }
        root.put("shops", shopsArr)

        // 5. Loans
        val loansArr = JSONArray()
        for (l in loans) {
            loansArr.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("isLentByMe", l.isLentByMe)
                    put("personId", l.personId ?: JSONObject.NULL)
                    put("personName", l.personName)
                    put("principalPaisa", l.principalPaisa)
                    put("interestPaisa", l.interestPaisa)
                    put("interestRatePercent", l.interestRatePercent)
                    put("installmentCount", l.installmentCount)
                    put("startDateMillis", l.startDateMillis)
                    put("dueDateMillis", l.dueDateMillis ?: JSONObject.NULL)
                    put("note", l.note)
                    put("createdAt", l.createdAt)
                }
            )
        }
        root.put("loans", loansArr)

        // 6. Transactions (Income, Expenses, Due records, Payment records, Lending, Borrowing, Loan repayments)
        val txArr = JSONArray()
        for (t in transactions) {
            txArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type)
                    put("direction", t.direction)
                    put("amountPaisa", t.amountPaisa)
                    put("timestamp", t.timestamp)
                    put("categoryId", t.categoryId ?: JSONObject.NULL)
                    put("categoryName", t.categoryName ?: JSONObject.NULL)
                    put("personId", t.personId ?: JSONObject.NULL)
                    put("personName", t.personName ?: JSONObject.NULL)
                    put("shopId", t.shopId ?: JSONObject.NULL)
                    put("shopName", t.shopName ?: JSONObject.NULL)
                    put("loanId", t.loanId ?: JSONObject.NULL)
                    put("productOrDescription", t.productOrDescription)
                    put("note", t.note)
                    put("dueDateMillis", t.dueDateMillis ?: JSONObject.NULL)
                    put("referenceId", t.referenceId ?: JSONObject.NULL)
                    put("createdAt", t.createdAt)
                }
            )
        }
        root.put("transactions", txArr)

        // 7. Smart Notes & Checklists
        val notesArr = JSONArray()
        for (n in notes) {
            notesArr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("checklistJson", n.checklistJson)
                    put("colorHex", n.colorHex)
                    put("priority", n.priority)
                    put("labelsCsv", n.labelsCsv)
                    put("isPinned", n.isPinned)
                    put("isArchived", n.isArchived)
                    put("isPinnedToNotification", n.isPinnedToNotification)
                    put("reminderMillis", n.reminderMillis ?: JSONObject.NULL)
                    put("repeatInterval", n.repeatInterval)
                    put("linkedTransactionType", n.linkedTransactionType ?: JSONObject.NULL)
                    put("linkedEntityId", n.linkedEntityId ?: JSONObject.NULL)
                    put("linkedEntityName", n.linkedEntityName ?: JSONObject.NULL)
                    put("targetBudgetPaisa", n.targetBudgetPaisa)
                    put("isCompleted", n.isCompleted)
                    put("updatedAt", n.updatedAt)
                    put("createdAt", n.createdAt)
                }
            )
        }
        root.put("notes", notesArr)

        return root.toString(2)
    }

    /**
     * Validates a JSON backup string before any database modification.
     * Checks schema version, required fields, transaction IDs, duplicate IDs, amounts, and dates.
     */
    fun validateAndParseBackup(rawJson: String): BackupValidationResult {
        if (rawJson.isBlank()) {
            return BackupValidationResult.Invalid("The selected backup file is empty.")
        }
        return try {
            val root = JSONObject(rawJson)
            val app = root.optString("app", "")
            val schemaVersion = root.optInt("schemaVersion", -1)

            if (app != "Hisab" || schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION + 1) {
                return BackupValidationResult.Invalid(
                    "Invalid or unsupported backup format. Please select a valid Hisab JSON backup file."
                )
            }

            if (!root.has("transactions") || !root.has("settings")) {
                return BackupValidationResult.Invalid(
                    "This backup file is missing required financial sections (settings or transactions)."
                )
            }

            val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())
            val formattedDate = root.optString(
                "exportedDate",
                MoneyUtils.formatDateFull(exportedAtMillis, "en")
            )

            // Parse Settings
            val sObj = root.getJSONObject("settings")
            val openingPaisa = sObj.optLong("openingCashBalancePaisa", 0L)
            val settings = AppSettingsEntity(
                id = 1,
                isFirstLaunchCompleted = true,
                currencySymbol = sObj.optString("currencySymbol", "৳").ifBlank { "৳" },
                currencyCode = sObj.optString("currencyCode", "BDT").ifBlank { "BDT" },
                openingCashBalancePaisa = openingPaisa,
                themeMode = sObj.optString("themeMode", "DARK"),
                accentColorHex = sObj.optString("accentColorHex", "#10B981"),
                backgroundStyle = sObj.optString("backgroundStyle", "HERO_ART"),
                glassTransparency = sObj.optInt("glassTransparency", 78).coerceIn(20, 100),
                glassBlurAmount = sObj.optInt("glassBlurAmount", 65).coerceIn(0, 100),
                cardOpacity = sObj.optInt("cardOpacity", 84).coerceIn(30, 100),
                uiDensity = sObj.optString("uiDensity", "COMFORTABLE"),
                languageCode = sObj.optString("languageCode", "en"),
                dashboardCardsOrder = sObj.optString(
                    "dashboardCardsOrder",
                    "CASH_HERO,TODAY_SUMMARY,CURRENT_POSITION,QUICK_ACTIONS,RECENT_ACTIVITY"
                ),
                hiddenDashboardCards = sObj.optString("hiddenDashboardCards", ""),
                dashboardLayoutStyle = sObj.optString("dashboardLayoutStyle", "GRID"),
                notificationsEnabled = sObj.optBoolean("notificationsEnabled", false),
                loanReminderEnabled = sObj.optBoolean("loanReminderEnabled", true),
                personDueReminderEnabled = sObj.optBoolean("personDueReminderEnabled", true),
                shopDueReminderEnabled = sObj.optBoolean("shopDueReminderEnabled", true)
            )

            // Parse Categories
            val categories = ArrayList<CategoryEntity>()
            val seenCatIds = HashSet<String>()
            val catArr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArr.length()) {
                val c = catArr.getJSONObject(i)
                val id = c.optString("id", "").trim()
                val name = c.optString("name", "").trim()
                if (id.isEmpty() || name.isEmpty() || !seenCatIds.add(id)) continue
                categories.add(
                    CategoryEntity(
                        id = id,
                        name = name,
                        nameBn = c.optString("nameBn", name),
                        type = c.optString("type", "EXPENSE"),
                        iconName = c.optString("iconName", "Category"),
                        colorHex = c.optString("colorHex", "#10B981"),
                        sortOrder = c.optInt("sortOrder", i),
                        usageCount = c.optInt("usageCount", 0).coerceAtLeast(0)
                    )
                )
            }

            // Parse People
            val people = ArrayList<PersonEntity>()
            val seenPersonIds = HashSet<String>()
            val pArr = root.optJSONArray("people") ?: JSONArray()
            for (i in 0 until pArr.length()) {
                val p = pArr.getJSONObject(i)
                val id = p.optString("id", "").trim()
                val name = p.optString("name", "").trim()
                if (id.isEmpty() || name.isEmpty() || !seenPersonIds.add(id)) continue
                people.add(
                    PersonEntity(
                        id = id,
                        name = name,
                        phone = p.optString("phone", ""),
                        note = p.optString("note", ""),
                        avatarColorHex = p.optString("avatarColorHex", "#06B6D4"),
                        iconName = p.optString("iconName", "Person"),
                        lastInteractionAt = p.optLong("lastInteractionAt", exportedAtMillis),
                        createdAt = p.optLong("createdAt", exportedAtMillis)
                    )
                )
            }

            // Parse Shops
            val shops = ArrayList<ShopEntity>()
            val seenShopIds = HashSet<String>()
            val shArr = root.optJSONArray("shops") ?: JSONArray()
            for (i in 0 until shArr.length()) {
                val s = shArr.getJSONObject(i)
                val id = s.optString("id", "").trim()
                val name = s.optString("name", "").trim()
                if (id.isEmpty() || name.isEmpty() || !seenShopIds.add(id)) continue
                shops.add(
                    ShopEntity(
                        id = id,
                        name = name,
                        ownerName = s.optString("ownerName", ""),
                        phone = s.optString("phone", ""),
                        address = s.optString("address", ""),
                        note = s.optString("note", ""),
                        colorHex = s.optString("colorHex", "#F59E0B"),
                        recentProductsCsv = s.optString("recentProductsCsv", "Rice,Oil,Sugar"),
                        lastInteractionAt = s.optLong("lastInteractionAt", exportedAtMillis),
                        createdAt = s.optLong("createdAt", exportedAtMillis)
                    )
                )
            }

            // Parse Loans
            val loans = ArrayList<LoanEntity>()
            val seenLoanIds = HashSet<String>()
            val lArr = root.optJSONArray("loans") ?: JSONArray()
            for (i in 0 until lArr.length()) {
                val l = lArr.getJSONObject(i)
                val id = l.optString("id", "").trim()
                val personName = l.optString("personName", "").trim()
                val principal = l.optLong("principalPaisa", -1L)
                val startMillis = l.optLong("startDateMillis", -1L)
                if (id.isEmpty() || personName.isEmpty() || principal <= 0L || startMillis <= 0L) {
                    return BackupValidationResult.Invalid(
                        "Backup contains an invalid loan record (ID: ${id.ifEmpty { "unknown" }})."
                    )
                }
                if (!seenLoanIds.add(id)) continue
                loans.add(
                    LoanEntity(
                        id = id,
                        isLentByMe = l.optBoolean("isLentByMe", false),
                        personId = l.optNullableString("personId"),
                        personName = personName,
                        principalPaisa = principal,
                        interestPaisa = l.optLong("interestPaisa", 0L).coerceAtLeast(0L),
                        interestRatePercent = l.optString("interestRatePercent", ""),
                        installmentCount = l.optInt("installmentCount", 1).coerceAtLeast(1),
                        startDateMillis = startMillis,
                        dueDateMillis = l.optNullableLong("dueDateMillis"),
                        note = l.optString("note", ""),
                        createdAt = l.optLong("createdAt", startMillis)
                    )
                )
            }

            // Parse & strictly validate Transactions
            val transactions = ArrayList<TransactionEntity>()
            val seenTxIds = HashSet<String>()
            var duplicateTxCount = 0
            val txArr = root.getJSONArray("transactions")

            for (i in 0 until txArr.length()) {
                val t = txArr.getJSONObject(i)
                val id = t.optString("id", "").trim()
                if (id.isEmpty()) {
                    return BackupValidationResult.Invalid(
                        "Backup validation failed: Transaction at index $i is missing a unique ID."
                    )
                }
                if (!seenTxIds.add(id)) {
                    duplicateTxCount++
                    continue
                }
                val amountPaisa = t.optLong("amountPaisa", -1L)
                if (amountPaisa <= 0L) {
                    return BackupValidationResult.Invalid(
                        "Backup validation failed: Transaction '$id' has an invalid amount."
                    )
                }
                val timestamp = t.optLong("timestamp", -1L)
                if (timestamp <= 0L) {
                    return BackupValidationResult.Invalid(
                        "Backup validation failed: Transaction '$id' has an invalid date."
                    )
                }
                val typeStr = t.optString("type", "")
                val parsedType = TransactionType.entries.find { it.name == typeStr }
                    ?: return BackupValidationResult.Invalid(
                        "Backup validation failed: Transaction '$id' has an unrecognized type '$typeStr'."
                    )
                val directionStr = t.optString("direction", parsedType.defaultDirection.name)
                val parsedDirection = TransactionDirection.entries.find { it.name == directionStr }
                    ?: parsedType.defaultDirection

                transactions.add(
                    TransactionEntity(
                        id = id,
                        type = parsedType.name,
                        direction = parsedDirection.name,
                        amountPaisa = amountPaisa,
                        timestamp = timestamp,
                        categoryId = t.optNullableString("categoryId"),
                        categoryName = t.optNullableString("categoryName"),
                        personId = t.optNullableString("personId"),
                        personName = t.optNullableString("personName"),
                        shopId = t.optNullableString("shopId"),
                        shopName = t.optNullableString("shopName"),
                        loanId = t.optNullableString("loanId"),
                        productOrDescription = t.optString("productOrDescription", ""),
                        note = t.optString("note", ""),
                        dueDateMillis = t.optNullableLong("dueDateMillis"),
                        referenceId = t.optNullableString("referenceId"),
                        createdAt = t.optLong("createdAt", timestamp)
                    )
                )
            }

            // Parse Smart Notes (optional for backward compatibility with v1 backups)
            val notes = ArrayList<SmartNoteEntity>()
            val seenNoteIds = HashSet<String>()
            val notesArr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until notesArr.length()) {
                val n = notesArr.getJSONObject(i)
                val id = n.optString("id", "").trim()
                val title = n.optString("title", "").trim()
                if (id.isEmpty() || title.isEmpty() || !seenNoteIds.add(id)) continue
                notes.add(
                    SmartNoteEntity(
                        id = id,
                        title = title,
                        content = n.optString("content", ""),
                        checklistJson = n.optString("checklistJson", "[]"),
                        colorHex = n.optString("colorHex", "#10B981"),
                        priority = n.optString("priority", "NORMAL"),
                        labelsCsv = n.optString("labelsCsv", ""),
                        isPinned = n.optBoolean("isPinned", false),
                        isArchived = n.optBoolean("isArchived", false),
                        isPinnedToNotification = n.optBoolean("isPinnedToNotification", false),
                        reminderMillis = n.optNullableLong("reminderMillis"),
                        repeatInterval = n.optString("repeatInterval", "NONE"),
                        linkedTransactionType = n.optNullableString("linkedTransactionType"),
                        linkedEntityId = n.optNullableString("linkedEntityId"),
                        linkedEntityName = n.optNullableString("linkedEntityName"),
                        targetBudgetPaisa = n.optLong("targetBudgetPaisa", 0L).coerceAtLeast(0L),
                        isCompleted = n.optBoolean("isCompleted", false),
                        updatedAt = n.optLong("updatedAt", exportedAtMillis),
                        createdAt = n.optLong("createdAt", exportedAtMillis)
                    )
                )
            }

            BackupValidationResult.Valid(
                ValidatedBackupPayload(
                    schemaVersion = schemaVersion,
                    exportedAtMillis = exportedAtMillis,
                    formattedDate = formattedDate,
                    settings = settings,
                    categories = categories,
                    people = people,
                    shops = shops,
                    loans = loans,
                    transactions = transactions,
                    notes = notes,
                    duplicateIdsIgnoredInFile = duplicateTxCount
                )
            )
        } catch (e: Exception) {
            BackupValidationResult.Invalid(
                "The backup file is corrupted or not a valid JSON file. Please verify the file and try again."
            )
        }
    }

    /**
     * Generates a RFC-4180 compliant CSV export of all transactions.
     */
    fun exportTransactionsToCsv(
        transactions: List<TransactionEntity>,
        currencySymbol: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Transaction ID,Date & Time,Type,Direction,Amount,Formatted Amount,Shop,Person,Category,Product / Description,Note")
        for (tx in transactions) {
            val dateStr = MoneyUtils.formatDateTime(tx.timestamp, "en")
            val editableAmt = MoneyUtils.paisaToEditableString(tx.amountPaisa)
            val formattedAmt = MoneyUtils.formatPaisa(tx.amountPaisa, currencySymbol)
            val row = listOf(
                tx.id,
                dateStr,
                tx.type,
                tx.direction,
                editableAmt,
                formattedAmt,
                tx.shopName ?: "",
                tx.personName ?: "",
                tx.categoryName ?: "",
                tx.productOrDescription,
                tx.note
            ).joinToString(",") { escapeCsvField(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Writes backup content to internal `files/backups/` and returns the File so it can be shared via FileProvider
     * or restored locally.
     */
    fun writeBackupFileToInternalStorage(context: Context, fileName: String, content: String): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun listSavedLocalBackups(context: Context): List<File> {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getDatabaseSizeBytes(context: Context, dbName: String): Long {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            (if (dbFile.exists()) dbFile.length() else 0L) +
                (if (walFile.exists()) walFile.length() else 0L) +
                (if (shmFile.exists()) shmFile.length() else 0L)
        } catch (e: Exception) {
            0L
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Handled gracefully if no share target exists in headless environments
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "").trim()
        return v.ifEmpty { null }
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val v = optLong(key, -1L)
        return if (v > 0L) v else null
    }
}
