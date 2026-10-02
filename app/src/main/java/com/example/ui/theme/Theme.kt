package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppSettingsEntity
import com.example.util.AppStrings
import com.example.util.HisabStrings

@Immutable
data class HisabThemeConfig(
    val isDark: Boolean = true,
    val isAmoled: Boolean = false,
    val accentColor: Color = AccentEmerald,
    val backgroundStyle: String = "HERO_ART",
    val glassTransparency: Int = 78,
    val glassBlurAmount: Int = 65,
    val cardOpacity: Int = 84,
    val isCompactDensity: Boolean = false,
    val currencySymbol: String = "৳",
    val currencyCode: String = "BDT",
    val languageCode: String = "bn"
)

val LocalHisabTheme = staticCompositionLocalOf { HisabThemeConfig() }
val LocalHisabStrings = staticCompositionLocalOf { AppStrings.Bangla }

@Composable
fun HisabTheme(
    settings: AppSettingsEntity = AppSettingsEntity(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode.uppercase()) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> systemDark
    }
    val isAmoled = settings.themeMode.equals("AMOLED", ignoreCase = true)
    val accentColor = parseHexColor(settings.accentColorHex, AccentEmerald)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF042F2E),
            primaryContainer = accentColor.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = AccentCyan,
            onSecondary = Color(0xFF083344),
            tertiary = AccentAmber,
            background = if (isAmoled) AmoledBackground else DarkBackground,
            onBackground = DarkOnBackground,
            surface = if (isAmoled) AmoledSurface else DarkSurface,
            onSurface = DarkOnBackground,
            surfaceVariant = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            error = FinanceExpense
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.16f),
            onPrimaryContainer = Color(0xFF064E3B),
            secondary = Color(0xFF0284C7),
            onSecondary = Color.White,
            tertiary = Color(0xFFD97706),
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnBackground,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            error = FinanceExpense
        )
    }

    val themeConfig = HisabThemeConfig(
        isDark = isDark,
        isAmoled = isAmoled,
        accentColor = accentColor,
        backgroundStyle = settings.backgroundStyle,
        glassTransparency = settings.glassTransparency.coerceIn(20, 100),
        glassBlurAmount = settings.glassBlurAmount.coerceIn(0, 100),
        cardOpacity = settings.cardOpacity.coerceIn(35, 100),
        isCompactDensity = settings.uiDensity.equals("COMPACT", ignoreCase = true),
        currencySymbol = settings.currencySymbol,
        currencyCode = settings.currencyCode,
        languageCode = settings.languageCode
    )

    val strings = AppStrings.forLanguage(settings.languageCode)

    CompositionLocalProvider(
        LocalHisabTheme provides themeConfig,
        LocalHisabStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HisabTheme(
        settings = AppSettingsEntity(themeMode = if (darkTheme) "DARK" else "LIGHT"),
        content = content
    )
}
