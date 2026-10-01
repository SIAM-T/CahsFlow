package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupValidationResult
import com.example.data.local.AppSettingsEntity
import com.example.data.local.HisabDatabase
import com.example.data.local.PersonEntity
import com.example.data.local.ShopEntity
import com.example.data.local.TransactionDirection
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hisab", appName)
    }

    @Test
    fun `backup export and validation roundtrip preserves all entities and detects corruption`() {
        val now = 1790867000000L
        val settings = AppSettingsEntity(
            currencySymbol = "৳",
            currencyCode = "BDT",
            openingCashBalancePaisa = 15000_00L,
            themeMode = "AMOLED"
        )
        val categories = HisabDatabase.defaultCategories()
        val shops = listOf(ShopEntity(id = "shop_rahman", name = "Rahman Store"))
        val people = listOf(PersonEntity(id = "person_rahim", name = "Rahim"))
        val txs = listOf(
            TransactionEntity(
                id = "tx_rice_500",
                type = TransactionType.SHOP_DUE.name,
                direction = TransactionDirection.CREDIT_EXTENDED.name,
                amountPaisa = 500_00L,
                timestamp = now,
                shopId = "shop_rahman",
                shopName = "Rahman Store",
                productOrDescription = "Rice"
            )
        )

        val json = BackupManager.exportToJsonString(
            settings = settings,
            categories = categories,
            people = people,
            shops = shops,
            loans = emptyList(),
            transactions = txs,
            nowMillis = now
        )

        val result = BackupManager.validateAndParseBackup(json)
        assertTrue(result is BackupValidationResult.Valid)
        val payload = (result as BackupValidationResult.Valid).payload
        assertEquals(15000_00L, payload.settings.openingCashBalancePaisa)
        assertEquals("AMOLED", payload.settings.themeMode)
        assertEquals(1, payload.shops.size)
        assertEquals("Rahman Store", payload.shops.first().name)
        assertEquals(1, payload.transactions.size)
        assertEquals(500_00L, payload.transactions.first().amountPaisa)

        // Verify corrupted JSON is safely rejected
        val corrupted = BackupManager.validateAndParseBackup("{invalid_json")
        assertTrue(corrupted is BackupValidationResult.Invalid)
    }
}
