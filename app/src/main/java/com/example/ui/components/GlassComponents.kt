package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TransactionType
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceLoan
import com.example.ui.theme.FinancePayable
import com.example.ui.theme.FinanceReceivable
import com.example.ui.theme.FinanceShopDue
import com.example.ui.theme.LocalHisabStrings
import com.example.ui.theme.LocalHisabTheme
import com.example.ui.theme.parseHexColor

/**
 * Background container that supports:
 * - HERO_ART (generated abstract glassmorphic background with atmospheric overlay)
 * - GRADIENT (multi-stop radial/linear mesh gradient)
 * - SOLID (clean high-contrast surface)
 */
@Composable
fun GlassmorphicBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalHisabTheme.current
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when (theme.backgroundStyle.uppercase()) {
            "HERO_ART" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1790867035704),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = if (theme.isDark) {
                        if (theme.isAmoled) 0.18f else 0.30f
                    } else {
                        0.12f
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    bgColor.copy(alpha = if (theme.isDark) 0.55f else 0.78f),
                                    bgColor.copy(alpha = if (theme.isDark) 0.88f else 0.94f),
                                    bgColor
                                )
                            )
                        )
                )
            }

            "GRADIENT" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (theme.isDark) {
                                    listOf(
                                        theme.accentColor.copy(alpha = 0.18f),
                                        bgColor,
                                        Color(0xFF06B6D4).copy(alpha = 0.12f)
                                    )
                                } else {
                                    listOf(
                                        theme.accentColor.copy(alpha = 0.12f),
                                        bgColor,
                                        Color(0xFF0284C7).copy(alpha = 0.08f)
                                    )
                                }
                            )
                        )
                )
            }

            else -> {
                // SOLID background
            }
        }
        content()
    }
}

/**
 * Reusable frosted glass card that respects user-configured cardOpacity, glassTransparency,
 * and UI density (Compact vs Comfortable) while guaranteeing strong text contrast.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalHisabTheme.current
    val baseAlpha = (theme.cardOpacity / 100f).coerceIn(0.55f, 0.98f)
    val surfaceColor = if (theme.isDark) {
        if (theme.isAmoled) {
            Color(0xFF0D1322).copy(alpha = baseAlpha)
        } else {
            Color(0xFF162238).copy(alpha = baseAlpha)
        }
    } else {
        Color.White.copy(alpha = (baseAlpha + 0.08f).coerceAtMost(0.98f))
    }

    val borderBrush = Brush.linearGradient(
        colors = if (tintColor != null) {
            listOf(
                tintColor.copy(alpha = if (theme.isDark) 0.50f else 0.40f),
                tintColor.copy(alpha = 0.12f)
            )
        } else if (theme.isDark) {
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f)
            )
        } else {
            listOf(
                Color(0xFFCBD5E1).copy(alpha = 0.85f),
                Color(0xFFE2E8F0).copy(alpha = 0.45f)
            )
        }
    )

    val defaultPad = if (theme.isCompactDensity) 12.dp else 16.dp
    val pad = contentPadding ?: PaddingValues(defaultPad)
    val shape = RoundedCornerShape(cornerRadius)

    val cardModifier = modifier
        .clip(shape)
        .background(
            brush = if (tintColor != null) {
                Brush.linearGradient(
                    colors = listOf(
                        tintColor.copy(alpha = if (theme.isDark) 0.16f else 0.10f),
                        surfaceColor
                    )
                )
            } else {
                Brush.linearGradient(listOf(surfaceColor, surfaceColor))
            }
        )
        .border(BorderStroke(1.dp, borderBrush), shape)
        .let { mod ->
            if (onClick != null) {
                mod.clickable(onClick = onClick)
            } else {
                mod
            }
        }
        .padding(pad)

    Column(
        modifier = cardModifier,
        content = content
    )
}

@Composable
fun QuickActionGlassButton(
    label: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHisabTheme.current
    val shape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (theme.isDark) color.copy(alpha = 0.14f) else color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.32f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = if (theme.isCompactDensity) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FriendlyEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionTestTag: String = "empty_state_action",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag(actionTestTag)
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun UndoToastBanner(
    visible: Boolean,
    message: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHisabStrings.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("undo_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onUndo,
                    modifier = Modifier.testTag("undo_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = strings.undo,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.undo,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AppLockScreenOverlay(
    expectedPin: String,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val triggerBiometric = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val prompt = BiometricPrompt.Builder(context)
                    .setTitle("Unlock Hisab")
                    .setSubtitle("Verify your identity to access your personal হিসাব")
                    .setNegativeButton(
                        "Use PIN",
                        context.mainExecutor
                    ) { _, _ -> }
                    .build()
                prompt.authenticate(
                    CancellationSignal(),
                    context.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            onUnlocked()
                        }
                    }
                )
            } catch (_: Exception) {
                errorText = "Biometric sensor unavailable; please enter your PIN."
            }
        }
    }

    GlassmorphicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hisab Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Enter your 4-digit PIN to view your finances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText!!,
                    color = FinanceExpense,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (key in row) {
                            when (key) {
                                "BIO" -> {
                                    if (biometricEnabled) {
                                        OutlinedButton(
                                            onClick = triggerBiometric,
                                            modifier = Modifier.size(72.dp),
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock"
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(72.dp))
                                    }
                                }

                                "DEL" -> {
                                    OutlinedButton(
                                        onClick = {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorText = null
                                            }
                                        },
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace"
                                        )
                                    }
                                }

                                else -> {
                                    Button(
                                        onClick = {
                                            if (enteredPin.length < 4) {
                                                val next = enteredPin + key
                                                enteredPin = next
                                                errorText = null
                                                if (next.length == 4) {
                                                    if (next == expectedPin) {
                                                        onUnlocked()
                                                    } else {
                                                        errorText = "Incorrect PIN. Try again."
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .testTag("pin_key_$key"),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun iconForName(iconName: String): ImageVector = when (iconName) {
    "Work" -> Icons.Default.Work
    "Store" -> Icons.Default.Store
    "Storefront" -> Icons.Default.Storefront
    "Laptop" -> Icons.Default.Laptop
    "CardGiftcard" -> Icons.Default.CardGiftcard
    "Savings" -> Icons.Default.Savings
    "Restaurant" -> Icons.Default.Restaurant
    "DirectionsBus" -> Icons.Default.DirectionsBus
    "ShoppingBag" -> Icons.Default.ShoppingBag
    "ReceiptLong" -> Icons.AutoMirrored.Filled.ReceiptLong
    "School" -> Icons.Default.School
    "LocalHospital" -> Icons.Default.LocalHospital
    "Movie" -> Icons.Default.Movie
    "FamilyRestroom" -> Icons.Default.FamilyRestroom
    "Wifi" -> Icons.Default.Wifi
    "Person" -> Icons.Default.Person
    "AccountBalance" -> Icons.Default.AccountBalance
    "Payments" -> Icons.Default.Payments
    "Handshake" -> Icons.Default.Handshake
    "MoreHoriz" -> Icons.Default.MoreHoriz
    else -> Icons.Default.Category
}

fun iconForTransactionType(type: TransactionType): ImageVector = when (type) {
    TransactionType.INCOME -> Icons.Default.Add
    TransactionType.EXPENSE -> Icons.Default.Remove
    TransactionType.LEND -> Icons.AutoMirrored.Filled.CallMade
    TransactionType.BORROW -> Icons.AutoMirrored.Filled.CallReceived
    TransactionType.RECEIVE_PAYMENT -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionType.MAKE_PAYMENT -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.SHOP_DUE -> Icons.Default.Storefront
    TransactionType.SHOP_PAYMENT -> Icons.Default.Payments
    TransactionType.LOAN_GIVEN, TransactionType.LOAN_RECEIVED -> Icons.Default.AccountBalance
    TransactionType.LOAN_REPAYMENT, TransactionType.LOAN_PAYMENT -> Icons.Default.Handshake
    TransactionType.ADJUSTMENT -> Icons.Default.SwapHoriz
}

fun colorForTransactionType(type: TransactionType): Color = when (type) {
    TransactionType.INCOME,
    TransactionType.RECEIVE_PAYMENT,
    TransactionType.SHOP_PAYMENT,
    TransactionType.LOAN_REPAYMENT -> FinanceIncome

    TransactionType.EXPENSE,
    TransactionType.MAKE_PAYMENT,
    TransactionType.LOAN_PAYMENT -> FinanceExpense

    TransactionType.LEND,
    TransactionType.LOAN_GIVEN -> FinanceReceivable

    TransactionType.BORROW,
    TransactionType.LOAN_RECEIVED -> FinancePayable

    TransactionType.SHOP_DUE -> FinanceShopDue
    TransactionType.ADJUSTMENT -> FinanceLoan
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
