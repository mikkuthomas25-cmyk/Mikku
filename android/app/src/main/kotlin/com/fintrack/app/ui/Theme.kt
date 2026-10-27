package com.fintrack.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Design tokens: "Obsidian & Emerald". A deep carbon canvas, charcoal cards with razor-thin
 * borders, one emerald accent (with a soft glow where it matters) and amber as the second colour.
 * Serif for balances, a clean sans-serif for everything else, tabular figures for every number.
 */
@Immutable
data class FinColors(
    val background: Color,
    val surface: Color,        // cards
    val surfaceRaised: Color,  // inputs, icon squircles
    val border: Color,         // hairlines
    val accent: Color,         // emerald: the active state, income, primary data series
    val accentEdgeRight: Color, // kept for older components: a deeper emerald
    val accentEdgeBottom: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val track: Color,          // empty bars, rings
    val gain: Color,
    val loss: Color,
    val buttonEdgeRight: Color,
    val buttonEdgeBottom: Color,
    val amber: Color,          // second series: spending pace, a second ring
)

private val Obsidian = FinColors(
    background = Color(0xFF0B0D10),
    surface = Color(0xFF14171C),
    surfaceRaised = Color(0xFF1B1F25),
    border = Color(0xFF242930),
    accent = Color(0xFF34D399),
    accentEdgeRight = Color(0xFF10B981),
    accentEdgeBottom = Color(0xFF059669),
    ink = Color(0xFFF2F4F5),
    inkSoft = Color(0xFF9AA3AD),
    inkFaint = Color(0xFF636B75),
    track = Color(0xFF20252C),
    gain = Color(0xFF34D399),
    loss = Color(0xFFF87171),
    buttonEdgeRight = Color(0xFF10B981),
    buttonEdgeBottom = Color(0xFF059669),
    amber = Color(0xFFF5B83D),
)

private val LocalFin = staticCompositionLocalOf { Obsidian }

object Fin {
    val colors: FinColors
        @Composable @ReadOnlyComposable get() = LocalFin.current
}

/** Tabular (monospaced) figures keep every amount aligned and steady as it changes. */
val MoneyStyle = TextStyle(fontFeatureSettings = "tnum")

/** Balances: a high-contrast serif, tabular figures. */
val BalanceStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum, lnum")

/** Small, muted, tracked-out uppercase label. */
val CapsLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)

private val AuraTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = FontFamily.Serif),
        displayMedium = displayMedium.copy(fontFamily = FontFamily.Serif),
        displaySmall = displaySmall.copy(fontFamily = FontFamily.Serif, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    )
}

@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    val c = Obsidian
    val scheme = darkColorScheme(
        primary = c.accent, onPrimary = c.background,
        background = c.background, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink, onSurfaceVariant = c.inkSoft,
        surfaceVariant = c.surfaceRaised, outlineVariant = c.border, outline = c.border,
        surfaceContainerLow = c.surface, surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceRaised,
        error = c.loss,
    )
    CompositionLocalProvider(LocalFin provides c) {
        MaterialTheme(colorScheme = scheme, typography = AuraTypography, content = content)
    }
}

/** Privacy mode (the eye on Home): every amount shows as ₹ •••• until it's turned off. */
object Privacy {
    var hidden by mutableStateOf(false)
}

private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
fun rupees(amount: Double): String = if (Privacy.hidden) "₹ ••••" else inr.format(amount)

private val inrPaise = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

/** "₹48,290" and ".50", for a balance whose paise are drawn smaller and dimmer. */
fun rupeesSplit(amount: Double): Pair<String, String> {
    if (Privacy.hidden) return "₹ ••••" to ""
    val full = inrPaise.format(amount)
    val dot = full.lastIndexOf('.')
    return if (dot < 0) full to "" else full.substring(0, dot) to full.substring(dot)
}

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
private val CATEGORY_COLORS = listOf(0xFF34D399, 0xFFF5B83D, 0xFF60A5FA, 0xFFA78BFA, 0xFFF472B6, 0xFF2DD4BF, 0xFFFB923C)

fun categoryColor(category: String): Color {
    val i = CATEGORY_SLOTS.indexOf(category)
    return if (i < 0) Color(0xFF7C8591) else Color(CATEGORY_COLORS[i])
}

/** A small symbol per category, shown in a rounded square beside each transaction. */
fun categorySymbol(category: String): String = when (category) {
    "Food & Dining" -> "☕"
    "Groceries" -> "🛒"
    "Shopping" -> "🛍"
    "Transport" -> "🚕"
    "Fuel" -> "⛽"
    "Bills & Utilities" -> "💡"
    "Rent" -> "🏠"
    "Entertainment" -> "🎬"
    "Health" -> "💊"
    "Education" -> "🎓"
    "Travel" -> "✈"
    "Cash Withdrawal" -> "💵"
    "Investments" -> "📈"
    "Daily small spends" -> "🧾"
    "Transfers" -> "👤"
    "Self Transfer" -> "🔁"
    "Income" -> "↓"
    else -> "•"
}
