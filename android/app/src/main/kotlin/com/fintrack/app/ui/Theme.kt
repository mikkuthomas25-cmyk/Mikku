package com.fintrack.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

/**
 * Design tokens, in the spirit of the NeoPOP design system: a near-black canvas, flat dark
 * tiles with hairline borders, white primary actions, one bright accent, and hard 3D edges on
 * anything you can press.
 */
@Immutable
data class FinColors(
    val background: Color,
    val surface: Color,        // tiles
    val surfaceRaised: Color,  // inputs, avatars
    val border: Color,         // tile hairlines
    val accent: Color,         // the one bright colour: hero tiles, primary data series
    val accentEdgeRight: Color,
    val accentEdgeBottom: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val track: Color,          // gridlines, empty bars
    val gain: Color,
    val loss: Color,
    val buttonEdgeRight: Color,  // white button's 3D edges
    val buttonEdgeBottom: Color,
)

private val Pop = FinColors(
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF161616),
    surfaceRaised = Color(0xFF222222),
    border = Color(0xFF2A2A2A),
    accent = Color(0xFF7A5CFA),
    accentEdgeRight = Color(0xFF5A3FD6),
    accentEdgeBottom = Color(0xFF4430A8),
    ink = Color(0xFFFFFFFF),
    inkSoft = Color(0xFFB3B3B3),
    inkFaint = Color(0xFF737373),
    track = Color(0xFF262626),
    gain = Color(0xFF3DDC97),
    loss = Color(0xFFFF6B6B),
    buttonEdgeRight = Color(0xFFA6A6A6),
    buttonEdgeBottom = Color(0xFF6E6E6E),
)

private val LocalFin = staticCompositionLocalOf { Pop }

object Fin {
    val colors: FinColors
        @Composable @ReadOnlyComposable get() = LocalFin.current
}

/** Tabular figures keep amounts from jiggling as they change. */
val MoneyStyle = TextStyle(fontFeatureSettings = "tnum")

/** Small uppercase label with tracking: NeoPOP's signature caption. */
val CapsLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)

private val PopTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
    )
}

/** Always dark: the NeoPOP look is built for a black canvas. */
@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    val c = Pop
    val scheme = darkColorScheme(
        primary = Color.White, onPrimary = Color.Black,
        background = c.background, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink, onSurfaceVariant = c.inkSoft,
        surfaceVariant = c.surfaceRaised, outlineVariant = c.border, outline = c.border,
        surfaceContainerLow = c.surface, surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceRaised,
        error = c.loss,
    )
    CompositionLocalProvider(LocalFin provides c) {
        MaterialTheme(colorScheme = scheme, typography = PopTypography, content = content)
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

/**
 * One fixed colour per main spending category, so "Food" is the same blue in every chart.
 * Validated for colour-blind separation on a dark surface; categories beyond these seven share
 * a neutral grey and are always shown with their name, never by colour alone.
 */
private val CATEGORY_SLOTS = listOf("Food & Dining", "Shopping", "Groceries", "Bills & Utilities", "Rent", "Health", "Transport")
private val CATEGORY_COLORS = listOf(0xFF3987E5, 0xFFD95926, 0xFF199E70, 0xFFC98500, 0xFFD55181, 0xFF008300, 0xFF9085E9)

fun categoryColor(category: String): Color {
    val i = CATEGORY_SLOTS.indexOf(category)
    return if (i < 0) Color(0xFF8A8A8A) else Color(CATEGORY_COLORS[i])
}
