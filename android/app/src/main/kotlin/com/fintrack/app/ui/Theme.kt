package com.fintrack.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Design tokens beyond Material's scheme: the gloss gradient, glass fills, money colors. */
@Immutable
data class FinColors(
    val background: Color,
    val glowA: Color,          // soft background wash, top-left
    val glowB: Color,          // soft background wash, top-right
    val glass: Color,          // translucent card fill
    val glassEdgeTop: Color,   // card rim highlight
    val glassEdgeBottom: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val track: Color,
    val gain: Color,
    val loss: Color,
    val accentSoft: Color,     // avatar / chip tint
)

private val LightFin = FinColors(
    background = Color(0xFFF3F5FB),
    glowA = Color(0x553B82F6),
    glowB = Color(0x447C3AED),
    glass = Color(0xD9FFFFFF),
    glassEdgeTop = Color(0xFFFFFFFF),
    glassEdgeBottom = Color(0x1A3B4A6B),
    heroStart = Color(0xFF2563EB),
    heroEnd = Color(0xFF6D28D9),
    ink = Color(0xFF0F172A),
    inkSoft = Color(0xFF475569),
    inkFaint = Color(0xFF94A3B8),
    track = Color(0xFFE6EAF3),
    gain = Color(0xFF15803D),
    loss = Color(0xFFDC2626),
    accentSoft = Color(0xFFE3EBFF),
)

private val DarkFin = FinColors(
    background = Color(0xFF0A0E17),
    glowA = Color(0x402563EB),
    glowB = Color(0x387C3AED),
    glass = Color(0x14FFFFFF),
    glassEdgeTop = Color(0x33FFFFFF),
    glassEdgeBottom = Color(0x0AFFFFFF),
    heroStart = Color(0xFF2563EB),
    heroEnd = Color(0xFF7C3AED),
    ink = Color(0xFFF1F5F9),
    inkSoft = Color(0xFFB4BFD0),
    inkFaint = Color(0xFF6B7891),
    track = Color(0x1FFFFFFF),
    gain = Color(0xFF4ADE80),
    loss = Color(0xFFF87171),
    accentSoft = Color(0x293B82F6),
)

private val LocalFin = staticCompositionLocalOf { LightFin }

object Fin {
    val colors: FinColors
        @Composable @ReadOnlyComposable get() = LocalFin.current
}

/** Tabular figures keep amounts from jiggling as they change. */
val MoneyStyle = TextStyle(fontFeatureSettings = "tnum")

private val FinTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun FinTrackTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val fin = if (dark) DarkFin else LightFin
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF60A5FA), onPrimary = Color(0xFF0A0E17),
            background = fin.background, surface = Color(0xFF121826), onSurface = fin.ink,
            onSurfaceVariant = fin.inkSoft, surfaceVariant = Color(0xFF1C2333), outlineVariant = Color(0x1FFFFFFF),
            error = fin.loss, surfaceContainerLow = Color(0xFF121826), surfaceContainer = Color(0xFF151C2B),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2563EB), onPrimary = Color.White,
            background = fin.background, surface = Color.White, onSurface = fin.ink,
            onSurfaceVariant = fin.inkSoft, surfaceVariant = Color(0xFFEFF2F8), outlineVariant = Color(0xFFE2E8F0),
            error = fin.loss, surfaceContainerLow = Color.White, surfaceContainer = Color(0xFFF8FAFD),
        )
    }
    CompositionLocalProvider(LocalFin provides fin) {
        MaterialTheme(colorScheme = scheme, typography = FinTypography, content = content)
    }
}

private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
fun rupees(amount: Double): String = inr.format(amount)

private val shortDate = DateTimeFormatter.ofPattern("d MMM")
private val longDate = DateTimeFormatter.ofPattern("d MMMM")
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthOnly = DateTimeFormatter.ofPattern("MMMM")
fun LocalDate.short(): String = format(shortDate)
fun LocalDate.long(): String = format(longDate)
fun YearMonth.label(): String = format(monthFmt)
fun YearMonth.name(): String = format(monthOnly)

fun LocalDate.relative(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> format(DateTimeFormatter.ofPattern("EEE, d MMM"))
}
