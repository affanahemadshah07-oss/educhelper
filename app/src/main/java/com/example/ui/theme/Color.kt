package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial Academic Studio Palette — Warm Paper, Oxford Indigo, Emerald Safe, Amber Focus, Crimson Alert
val OxfordIndigo = Color(0xFF1B3A5C)
val OxfordIndigoLight = Color(0xFF2B547E)
val OxfordIndigoContainer = Color(0xFFD9E6F7)
val OnOxfordIndigoContainer = Color(0xFF0B223C)

val EmeraldSafe = Color(0xFF0E9F6E)
val EmeraldSafeContainer = Color(0xFFD5F5E9)
val OnEmeraldSafeContainer = Color(0xFF04432D)

val AmberFocus = Color(0xFFD97706)
val AmberFocusContainer = Color(0xFFFEF0D1)
val OnAmberFocusContainer = Color(0xFF572B00)

val CrimsonAlert = Color(0xFFDC2626)
val CrimsonAlertContainer = Color(0xFFFEE2E2)
val OnCrimsonAlertContainer = Color(0xFF690B0B)

// Light Surface Tokens (Warm Paper / Parchment White)
val PaperBackground = Color(0xFFF9F7F3)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceVariant = Color(0xFFF0ECE4)
val PaperSurfaceContainer = Color(0xFFF4F1EA)
val InkPrimary = Color(0xFF151C26)
val InkSecondary = Color(0xFF4F5D70)
val HairlineOutline = Color(0xFFDCD6CA)

// Dark Surface Tokens (Midnight Scholarly Slate)
val MidnightBackground = Color(0xFF0E141D)
val MidnightSurface = Color(0xFF161F2D)
val MidnightSurfaceVariant = Color(0xFF1F2B3D)
val MidnightSurfaceContainer = Color(0xFF1B2536)
val MidnightPrimary = Color(0xFF9AC5F4)
val OnMidnightPrimary = Color(0xFF0A2440)
val MidnightPrimaryContainer = Color(0xFF1E4168)
val OnMidnightPrimaryContainer = Color(0xFFDBEAFE)
val MidnightText = Color(0xFFF2F4F8)
val MidnightTextSecondary = Color(0xFF9FB0C4)
val MidnightOutline = Color(0xFF2E3C52)

val EmeraldDark = Color(0xFF34D399)
val EmeraldDarkContainer = Color(0xFF0A3D2C)
val AmberDark = Color(0xFFFBBF24)
val AmberDarkContainer = Color(0xFF4A3106)
val CrimsonDark = Color(0xFFF87171)
val CrimsonDarkContainer = Color(0xFF5B1616)

// Curated Subject Accent Swatches (Stored as hex strings in Room)
val SubjectColorHexes = listOf(
    "#1B3A5C", // Oxford Indigo
    "#0E9F6E", // Forest Emerald
    "#D97706", // Warm Amber
    "#7C3AED", // Scholarly Violet
    "#0284C7", // Cerulean Blue
    "#E11D48", // Academic Rose
    "#0D9488", // Deep Teal
    "#EA580C"  // Terracotta
)

fun parseHexColor(hex: String, fallback: Color = OxfordIndigo): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
