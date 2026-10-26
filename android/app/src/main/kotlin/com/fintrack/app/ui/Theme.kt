package com.fintrack.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Light = lightColorScheme(
    primary = Color(0xFF2A78D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE2FB),
    onPrimaryContainer = Color(0xFF0D366B),
    background = Color(0xFFF9F9F7),
    surface = Color(0xFFFCFCFB),
    surfaceVariant = Color(0xFFECEBE7),
    onSurface = Color(0xFF0B0B0B),
    onSurfaceVariant = Color(0xFF52514E),
    outlineVariant = Color(0xFFE4E3DF),
    error = Color(0xFFC22F2F),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF3987E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF184F95),
    onPrimaryContainer = Color(0xFFCDE2FB),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1A1A19),
    surfaceVariant = Color(0xFF2A2A28),
    onSurface = Color(0xFFF0EFEC),
    onSurfaceVariant = Color(0xFFC3C2B7),
    outlineVariant = Color(0xFF2E2E2C),
    error = Color(0xFFE66767),
)

/** Positive money (income, savings). Text color, so it stays readable on both themes. */
val Gain @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4CC34C) else Color(0xFF0A7D0A)

@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
fun rupees(amount: Double): String = inr.format(amount)

private val shortDate = DateTimeFormatter.ofPattern("d MMM")
private val longDate = DateTimeFormatter.ofPattern("d MMMM")
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
fun LocalDate.short(): String = format(shortDate)
fun LocalDate.long(): String = format(longDate)
fun YearMonth.label(): String = format(monthFmt)
