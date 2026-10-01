package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Accent Color Presets
val AccentEmerald = Color(0xFF10B981)
val AccentCyan = Color(0xFF06B6D4)
val AccentAmber = Color(0xFFF59E0B)
val AccentIndigo = Color(0xFF6366F1)
val AccentRose = Color(0xFFF43F5E)
val AccentViolet = Color(0xFF8B5CF6)

// Semantic Finance Colors
val FinanceIncome = Color(0xFF10B981)
val FinanceExpense = Color(0xFFF43F5E)
val FinanceReceivable = Color(0xFF06B6D4)
val FinancePayable = Color(0xFFF97316)
val FinanceShopDue = Color(0xFFF59E0B)
val FinanceLoan = Color(0xFF8B5CF6)

// Dark Glass Palette
val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF151F32)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)

// AMOLED Glass Palette
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0F1A)
val AmoledSurfaceVariant = Color(0xFF131C2E)

// Light Glass Palette
val LightBackground = Color(0xFFF1F5F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)

fun parseHexColor(hex: String, fallback: Color = AccentEmerald): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val longVal = clean.toLong(16)
        if (clean.length == 6) {
            Color(longVal or 0xFF000000L)
        } else if (clean.length == 8) {
            Color(longVal)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
