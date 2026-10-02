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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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

/**
 * Multi-layered atmospheric background with rich aurora light spheres, hero artwork,
 * and subtle mesh depth so all transparent glass components float over a vivid backdrop.
 */
@Composable
fun GlassmorphicBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalHisabTheme.current
    val baseBg = if (theme.isDark) {
        if (theme.isAmoled) Color(0xFF020409) else Color(0xFF070D19)
    } else {
        Color(0xFFEEF4FA)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        // Layer 1: Hero artwork if enabled
        if (theme.backgroundStyle.equals("HERO_ART", ignoreCase = true)) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1790867035704),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (theme.isDark) {
                    if (theme.isAmoled) 0.42f else 0.58f
                } else {
                    0.22f
                }
            )
        }

        // Layer 2: Luminous Aurora Light Orbs (visible through all transparent glass cards!)
        if (!theme.backgroundStyle.equals("SOLID", ignoreCase = true)) {
            val accent = theme.accentColor
            val isDark = theme.isDark
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val w = size.width
                        val h = size.height

                        // Top-left Emerald/Accent Aurora Orb
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = if (isDark) 0.34f else 0.24f),
                                    accent.copy(alpha = if (isDark) 0.10f else 0.06f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.15f, h * 0.12f),
                                radius = w * 0.78f
                            ),
                            radius = w * 0.78f,
                            center = Offset(w * 0.15f, h * 0.12f)
                        )

                        // Center-right Electric Cyan Orb
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF06B6D4).copy(alpha = if (isDark) 0.28f else 0.20f),
                                    Color(0xFF3B82F6).copy(alpha = if (isDark) 0.08f else 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.88f, h * 0.42f),
                                radius = w * 0.72f
                            ),
                            radius = w * 0.72f,
                            center = Offset(w * 0.88f, h * 0.42f)
                        )

                        // Bottom-left Royal Violet & Warm Amber Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.24f else 0.16f),
                                    Color(0xFFF59E0B).copy(alpha = if (isDark) 0.07f else 0.04f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.22f, h * 0.82f),
                                radius = w * 0.80f
                            ),
                            radius = w * 0.80f,
                            center = Offset(w * 0.22f, h * 0.82f)
                        )
                    }
            )
        }

        // Layer 3: Subtle atmospheric vignette for guaranteed text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (theme.isDark) {
                            listOf(
                                Color(0xFF050A14).copy(alpha = 0.25f),
                                Color(0xFF070E1C).copy(alpha = 0.45f),
                                Color(0xFF050A14).copy(alpha = 0.68f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color(0xFFF1F5F9).copy(alpha = 0.55f),
                                Color(0xFFE2E8F0).copy(alpha = 0.65f)
                            )
                        }
                    )
                )
        )

        content()
    }
}

/**
 * True translucent frosted glass card with:
 * - Translucent frosted fill that lets the aurora background shine through
 * - Top-left to bottom-right diagonal specular reflection sheen
 * - Crisp 1.dp gradient glass rim border (bright white/tint at top-left, subtle at bottom-right)
 * - Inner top specular highlight line
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalHisabTheme.current
    // Map user cardOpacity (20..100) into a true translucent glass range (0.18f..0.68f in dark mode)
    val userFactor = (theme.cardOpacity / 100f).coerceIn(0.20f, 0.95f)
    val glassAlpha = if (theme.isDark) {
        (0.22f + userFactor * 0.36f).coerceIn(0.22f, 0.62f)
    } else {
        (0.48f + userFactor * 0.32f).coerceIn(0.48f, 0.82f)
    }

    val frostTopColor = if (theme.isDark) {
        if (tintColor != null) {
            tintColor.copy(alpha = 0.20f)
        } else {
            Color(0xFF1E293B).copy(alpha = glassAlpha)
        }
    } else {
        if (tintColor != null) {
            tintColor.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = glassAlpha)
        }
    }

    val frostBottomColor = if (theme.isDark) {
        Color(0xFF0B1324).copy(alpha = (glassAlpha * 0.85f).coerceIn(0.18f, 0.54f))
    } else {
        Color.White.copy(alpha = (glassAlpha * 0.78f).coerceIn(0.38f, 0.72f))
    }

    val borderBrush = Brush.linearGradient(
        colors = if (tintColor != null) {
            listOf(
                Color.White.copy(alpha = if (theme.isDark) 0.38f else 0.85f),
                tintColor.copy(alpha = if (theme.isDark) 0.55f else 0.45f),
                tintColor.copy(alpha = 0.14f)
            )
        } else if (theme.isDark) {
            listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.50f)
            )
        }
    )

    val defaultPad = if (theme.isCompactDensity) 13.dp else 18.dp
    val pad = contentPadding ?: PaddingValues(defaultPad)
    val shape = RoundedCornerShape(cornerRadius)

    val cardModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(frostTopColor, frostBottomColor)
            )
        )
        .drawWithContent {
            // Subtle top-left specular glass reflection sheen
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (theme.isDark) 0.09f else 0.35f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.7f, size.height * 0.7f)
                )
            )
            // Top edge luminous rim line
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        (tintColor ?: Color.White).copy(alpha = if (theme.isDark) 0.35f else 0.75f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.1f, 0f),
                end = Offset(size.width * 0.9f, 0f),
                strokeWidth = 1.5f
            )
            drawContent()
        }
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

/**
 * Translucent glass pill/chip replacing standard opaque Material FilterChips.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    leadingIcon: ImageVector? = null
) {
    val theme = LocalHisabTheme.current
    val shape = RoundedCornerShape(50)
    val bgBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = if (theme.isDark) 0.32f else 0.24f),
                accentColor.copy(alpha = if (theme.isDark) 0.16f else 0.12f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                if (theme.isDark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.55f),
                if (theme.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.35f)
            )
        )
    }

    val borderBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                accentColor.copy(alpha = 0.75f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                if (theme.isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.85f),
                if (theme.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFCBD5E1).copy(alpha = 0.45f)
            )
        )
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                if (theme.isDark) Color.White else accentColor
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Translucent circular glass icon button for top bars and actions.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val theme = LocalHisabTheme.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = if (theme.isDark) {
                        listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.45f))
                    }
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (theme.isDark) 0.35f else 0.9f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                ),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Translucent glass styling for OutlinedTextFields so forms never look opaque or boxy.
 */
@Composable
fun glassTextFieldColors(accentColor: Color = MaterialTheme.colorScheme.primary): TextFieldColors {
    val theme = LocalHisabTheme.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f),
        unfocusedContainerColor = if (theme.isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.45f),
        focusedBorderColor = accentColor.copy(alpha = 0.85f),
        unfocusedBorderColor = if (theme.isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFCBD5E1),
        focusedLabelColor = accentColor,
        cursorColor = accentColor
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
    val shape = RoundedCornerShape(20.dp)
    val bgBrush = Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = if (theme.isDark) 0.22f else 0.16f),
            if (theme.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.50f)
        )
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (theme.isDark) 0.32f else 0.85f),
            color.copy(alpha = 0.45f),
            color.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(bgBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = if (theme.isCompactDensity) 10.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.35f),
                                color.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.dp, color.copy(alpha = 0.45f), CircleShape),
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
                fontWeight = FontWeight.SemiBold,
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
        tintColor = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
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
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag(actionTestTag)
                ) {
                    Text(text = actionLabel, fontWeight = FontWeight.Bold)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.85f),
                            Color(0xFF1E293B).copy(alpha = 0.78f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            )
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
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
                    fontWeight = FontWeight.Medium,
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
            GlassCard(
                tintColor = MaterialTheme.colorScheme.primary,
                cornerRadius = 32.dp,
                contentPadding = PaddingValues(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hisab Vault Locked",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Enter your 4-digit PIN to view your finances",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
                                        else Color.White.copy(alpha = 0.12f)
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
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

                    // Translucent Glass Keypad
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
                                                GlassIconButton(
                                                    icon = Icons.Default.Fingerprint,
                                                    contentDescription = "Biometric Unlock",
                                                    onClick = triggerBiometric,
                                                    modifier = Modifier.size(68.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(68.dp))
                                            }
                                        }

                                        "DEL" -> {
                                            GlassIconButton(
                                                icon = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                onClick = {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorText = null
                                                    }
                                                },
                                                modifier = Modifier.size(68.dp)
                                            )
                                        }

                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.10f))
                                                    .border(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.25f),
                                                        CircleShape
                                                    )
                                                    .clickable {
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
                                                    }
                                                    .testTag("pin_key_$key"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
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
