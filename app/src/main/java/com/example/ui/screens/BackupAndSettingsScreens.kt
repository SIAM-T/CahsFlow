package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupManager
import com.example.data.backup.ValidatedBackupPayload
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BackupMetadataEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceReceivable
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor
import com.example.util.MoneyUtils
import java.io.File
import java.util.Locale

@Composable
fun BackupAndRestoreSubScreen(
    backupMetadata: BackupMetadataEntity,
    totalTransactionsCount: Int,
    databaseSizeBytes: Long,
    savedLocalBackups: List<File>,
    pendingPreview: ValidatedBackupPayload?,
    onBack: () -> Unit,
    onExportJson: (Boolean, ((String) -> Unit)?) -> Unit,
    onExportCsv: (Boolean, ((String) -> Unit)?) -> Unit,
    onInspectBackupJson: (String) -> Unit,
    onConfirmFullRestore: () -> Unit,
    onConfirmMergeImport: () -> Unit,
    onCancelPendingImport: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val strings = LocalHisabStrings.current
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var showDestructiveConfirmDialog by remember { mutableStateOf(false) }
    var pendingSaveContent by remember { mutableStateOf<String?>(null) }

    // SAF Document Creator for saving JSON externally
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingSaveContent
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {}
        }
    }

    // SAF Document Picker for importing a JSON backup file
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: ""
                onInspectBackupJson(text)
            } catch (_: Exception) {
                onInspectBackupJson("")
            }
        }
    }

    val formattedDbSize = remember(databaseSizeBytes) {
        val kb = databaseSizeBytes / 1024.0
        if (kb >= 1024.0) {
            String.format(Locale.US, "%.2f MB", kb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f KB", kb.coerceAtLeast(12.0))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "backup_topbar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = strings.backupSection,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. Backup Status & Metadata Card (Section 29)
        item(key = "backup_status_card") {
            GlassCard(
                tintColor = FinanceReceivable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Last backup:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = backupMetadata.lastBackupTimestamp?.let {
                                MoneyUtils.formatDateTime(it, "en")
                            } ?: "No backup created yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("last_backup_timestamp_text")
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = FinanceReceivable,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Number of transactions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalTransactionsCount",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Database size:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDbSize,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Primary Backup & Restore Actions (Export Full Backup, Import Backup, Export Transactions CSV)
        item(key = "backup_actions_card") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Portable Data Backup & Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Exports settings, categories, people, shops, loans, transactions & theme preferences into a validated portable JSON file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onExportJson(true, null)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_full_backup_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.exportFullBackup, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onExportJson(false) { jsonContent ->
                            pendingSaveContent = jsonContent
                            createJsonLauncher.launch(BackupManager.defaultBackupFileName())
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_backup_to_device_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Backup File to Device Storage")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinanceReceivable,
                        contentColor = Color(0xFF042F2E)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("import_backup_file_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.importBackup, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showPasteJsonDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_backup_json_button")
                ) {
                    Text("Validate & Import from JSON Text")
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onExportCsv(true, null) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.exportTransactionsCsv, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Saved Local Backups List (Allows 1-tap restore/preview of previously exported backups!)
        if (savedLocalBackups.isNotEmpty()) {
            item(key = "local_backups_card") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recent On-Device Backup Snapshots",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    for (file in savedLocalBackups.take(5)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = MoneyUtils.formatDateTime(file.lastModified()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    val raw = file.readText(Charsets.UTF_8)
                                    onInspectBackupJson(raw)
                                },
                                modifier = Modifier.testTag("restore_local_snapshot_${file.name}")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Inspect & Restore")
                            }
                        }
                    }
                }
            }
        }
    }

    // Paste JSON Dialog
    if (showPasteJsonDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = { Text("Import Hisab Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste exported HisabBackup JSON content") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_json_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasteJsonDialog = false
                        onInspectBackupJson(jsonInput)
                    },
                    modifier = Modifier.testTag("validate_pasted_json_button")
                ) {
                    Text("Validate Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Found Preview Dialog (Exact specification in Section 3 & 30!)
    if (pendingPreview != null && !showDestructiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = onCancelPendingImport,
            title = {
                Text(
                    text = "Backup found",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("backup_found_dialog_title")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Date: ${pendingPreview.formattedDate}", style = MaterialTheme.typography.bodyLarge)
                    Text("Transactions: ${pendingPreview.transactions.size}", style = MaterialTheme.typography.bodyLarge)
                    Text("Shops: ${pendingPreview.shops.size}", style = MaterialTheme.typography.bodyLarge)
                    Text("People: ${pendingPreview.people.size}", style = MaterialTheme.typography.bodyLarge)
                    Text("Loans: ${pendingPreview.loans.size}", style = MaterialTheme.typography.bodyLarge)
                    if (pendingPreview.duplicateIdsIgnoredInFile > 0) {
                        Text(
                            text = "(${pendingPreview.duplicateIdsIgnoredInFile} duplicate IDs inside file automatically filtered)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose how to import this validated backup:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onConfirmMergeImport,
                        modifier = Modifier.testTag("backup_merge_import_button")
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Merge")
                    }
                    Button(
                        onClick = { showDestructiveConfirmDialog = true },
                        modifier = Modifier.testTag("backup_restore_button")
                    ) {
                        Text("Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancelPendingImport,
                    modifier = Modifier.testTag("backup_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Destructive Full Restore Confirmation Dialog ("Before destructive replacement, ask for confirmation")
    if (showDestructiveConfirmDialog && pendingPreview != null) {
        AlertDialog(
            onDismissRequest = { showDestructiveConfirmDialog = false },
            title = { Text("Confirm Full Restore?") },
            text = {
                Text(
                    "Full Restore will replace your current on-device records with the ${pendingPreview.transactions.size} transactions from '${pendingPreview.formattedDate}'. Use 'Merge' if you want to keep existing records and only add new ones."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDestructiveConfirmDialog = false
                        onConfirmFullRestore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpense),
                    modifier = Modifier.testTag("confirm_destructive_restore_button")
                ) {
                    Text("Confirm Full Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDestructiveConfirmDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationSubScreen(
    settings: AppSettingsEntity,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettingsEntity) -> AppSettingsEntity) -> Unit
) {
    BackHandler { onBack() }
    val strings = LocalHisabStrings.current

    val accentSwatches = listOf(
        "#10B981" to "Emerald",
        "#06B6D4" to "Cyan",
        "#F59E0B" to "Amber Gold",
        "#6366F1" to "Indigo",
        "#F43F5E" to "Rose",
        "#8B5CF6" to "Violet"
    )

    val currencies = listOf(
        "৳" to "BDT",
        "$" to "USD",
        "₹" to "INR",
        "€" to "EUR",
        "£" to "GBP",
        "SAR" to "SAR",
        "RM" to "MYR"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customization_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "cust_topbar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("customization_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = strings.customizationSection,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. Theme Mode (Light, Dark, System, AMOLED)
        item(key = "cust_theme_mode") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (mode in listOf("DARK", "AMOLED", "LIGHT", "SYSTEM")) {
                        FilterChip(
                            selected = settings.themeMode.equals(mode, ignoreCase = true),
                            onClick = {
                                onUpdateSettings { it.copy(themeMode = mode) }
                            },
                            label = { Text(mode) },
                            modifier = Modifier.testTag("theme_mode_chip_${mode.lowercase()}")
                        )
                    }
                }
            }
        }

        // 2. Accent Color
        item(key = "cust_accent_color") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Accent Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for ((hex, label) in accentSwatches) {
                        val color = parseHexColor(hex)
                        val isSelected = settings.accentColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    onUpdateSettings { it.copy(accentColorHex = hex) }
                                }
                                .testTag("accent_swatch_${label.lowercase().replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = label, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3. Background & Glass Effect Controls (Transparency, Blur, Card Opacity)
        item(key = "cust_glass_controls") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Background & Glassmorphism", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = listOf("HERO_ART" to "Glass Art", "GRADIENT" to "Gradient", "SOLID" to "Solid")
                    for ((code, title) in styles) {
                        FilterChip(
                            selected = settings.backgroundStyle == code,
                            onClick = { onUpdateSettings { it.copy(backgroundStyle = code) } },
                            label = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Card Glass Frost: ${settings.cardOpacity}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.cardOpacity.toFloat().coerceIn(20f, 85f),
                    onValueChange = { v ->
                        onUpdateSettings { it.copy(cardOpacity = v.toInt()) }
                    },
                    valueRange = 20f..85f
                )

                Text(
                    text = "Glass Transparency: ${settings.glassTransparency}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.glassTransparency.toFloat(),
                    onValueChange = { v ->
                        onUpdateSettings { it.copy(glassTransparency = v.toInt()) }
                    },
                    valueRange = 20f..100f
                )

                Text(
                    text = "Glass Blur Intensity: ${settings.glassBlurAmount}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.glassBlurAmount.toFloat(),
                    onValueChange = { v ->
                        onUpdateSettings { it.copy(glassBlurAmount = v.toInt()) }
                    },
                    valueRange = 0f..100f
                )
            }
        }

        // 4. UI Density & Currency
        item(key = "cust_density_currency") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("UI Density", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.uiDensity == "COMFORTABLE",
                        onClick = { onUpdateSettings { it.copy(uiDensity = "COMFORTABLE") } },
                        label = { Text("Comfortable") },
                        modifier = Modifier.testTag("density_comfortable")
                    )
                    FilterChip(
                        selected = settings.uiDensity == "COMPACT",
                        onClick = { onUpdateSettings { it.copy(uiDensity = "COMPACT") } },
                        label = { Text("Compact") },
                        modifier = Modifier.testTag("density_compact")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Currency Symbol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((sym, code) in currencies) {
                        FilterChip(
                            selected = settings.currencySymbol == sym,
                            onClick = {
                                onUpdateSettings { it.copy(currencySymbol = sym, currencyCode = code) }
                            },
                            label = { Text("$sym $code") },
                            modifier = Modifier.testTag("cust_currency_$code")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndSecuritySubScreen(
    settings: AppSettingsEntity,
    isSecurityFocused: Boolean = false,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettingsEntity) -> AppSettingsEntity) -> Unit,
    onLoadDemoData: () -> Unit,
    onClearAllData: () -> Unit
) {
    BackHandler { onBack() }
    val strings = LocalHisabStrings.current
    var openingBalText by remember(settings.openingCashBalancePaisa) {
        mutableStateOf(MoneyUtils.paisaToEditableString(settings.openingCashBalancePaisa))
    }
    var pinInput by remember(settings.appLockPin) { mutableStateOf(settings.appLockPin) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onUpdateSettings { it.copy(notificationsEnabled = isGranted) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "settings_topbar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (isSecurityFocused) strings.securitySection else strings.settingsSection,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. Language Switcher (English / বাংলা) (Section 45)
        item(key = "settings_language") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Language / ভাষা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.languageCode == "en",
                        onClick = { onUpdateSettings { it.copy(languageCode = "en") } },
                        label = { Text("English") },
                        modifier = Modifier.testTag("lang_chip_en")
                    )
                    FilterChip(
                        selected = settings.languageCode == "bn",
                        onClick = { onUpdateSettings { it.copy(languageCode = "bn") } },
                        label = { Text("বাংলা (Bangla)") },
                        modifier = Modifier.testTag("lang_chip_bn")
                    )
                }
            }
        }

        // 2. Opening Cash Balance
        item(key = "settings_opening_balance") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.openingBalance,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = openingBalText,
                        onValueChange = { openingBalText = it },
                        label = { Text("Opening Cash (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_opening_balance_input")
                    )
                    Button(
                        onClick = {
                            val paisa = MoneyUtils.parseToPaisa(openingBalText) ?: 0L
                            onUpdateSettings { it.copy(openingCashBalancePaisa = paisa) }
                        },
                        modifier = Modifier.testTag("settings_save_opening_balance_button")
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }

        // 3. Optional Local App Lock (PIN & Biometric) (Section 33)
        item(key = "settings_app_lock") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Local App Lock (PIN)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Protect your হিসাব locally without any online account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && pinInput.length == 4) {
                                onUpdateSettings { it.copy(appLockEnabled = true, appLockPin = pinInput) }
                            } else if (!enabled) {
                                onUpdateSettings { it.copy(appLockEnabled = false) }
                            }
                        },
                        modifier = Modifier.testTag("switch_app_lock")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_app_lock_pin")
                    )
                    Button(
                        onClick = {
                            if (pinInput.length == 4) {
                                onUpdateSettings { it.copy(appLockPin = pinInput, appLockEnabled = true) }
                            }
                        },
                        enabled = pinInput.length == 4,
                        modifier = Modifier.testTag("save_pin_button")
                    ) {
                        Text("Set PIN")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allow Biometric Unlock", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.biometricEnabled,
                        onCheckedChange = { bio ->
                            onUpdateSettings { it.copy(biometricEnabled = bio) }
                        }
                    )
                }
            }
        }

        // 4. Optional Notifications & Reminders (Section 31)
        item(key = "settings_notifications") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Due Date Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Optional local reminders for loans, people & shop dues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onUpdateSettings { it.copy(notificationsEnabled = enabled) }
                            }
                        }
                    )
                }
            }
        }

        // 5. Data Management & Privacy (Section 32 & 34)
        item(key = "settings_data_management") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onLoadDemoData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("load_demo_data_button")
                ) {
                    Text("Load Sample হিসাব Data (Rahman Store, Rahim, Karim)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    border = BorderStroke(1.dp, FinanceExpense.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = FinanceExpense)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Transactions & Records", color = FinanceExpense)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = FinanceIncome)
                    Text(
                        text = "Hisab v1.0 • 100% Offline-First • All financial data stays strictly in your local device database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all transactions, shops, people, and loans on this device. Consider exporting a JSON backup first.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpense)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
