package com.fintrack.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fintrack.core.Amount
import com.fintrack.core.Direction
import com.fintrack.core.Patterns
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth

/** Everything about one category: this month, its usual level, its trend, merchants and payments. */
@Composable
fun CategoryScreen(vm: FinTrackViewModel, category: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val c = Fin.colors
    val color = categoryColor(category)
    val m = vm.month
    val today = LocalDate.now()
    val isCurrent = YearMonth.from(today) == m
    val inCat = vm.txns.filter { it.category == category && it.spendValue != 0.0 }
    val spent = inCat.sumOf { it.spendValue }.coerceAtLeast(0.0)
    val share = vm.summary?.spent?.takeIf { it > 0 }?.let { spent / it }
    val typical = Patterns.typicalMonthly(vm.history, m, category)
    val forecast = if (isCurrent) Patterns.forecast(vm.history, m, today, category) else null
    val months = (vm.history.minOfOrNull { YearMonth.from(it.date) }?.let { first ->
        generateSequence(maxOf(first, m.minusMonths(5))) { it.plusMonths(1) }.takeWhile { it <= m }.toList()
    } ?: listOf(m))
    val trend = Patterns.monthlyTotals(vm.history, months, category)
    val unusual = Patterns.unusual(vm.history, m).filter { it.txn.category == category }
    val merchants = inCat.groupBy { it.merchant }.map { (name, v) -> Amount(name, v.sumOf { it.spendValue }, v.size) }
        .filter { it.amount > 0 }
        .sortedByDescending { it.amount }
    val perDay = inCat.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.spendValue }.coerceAtLeast(0.0) }
    val days = (1..m.lengthOfMonth()).map { m.atDay(it) to (perDay[m.atDay(it)] ?: 0.0) }
    var editing by remember { mutableStateOf<Txn?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("‹", style = MaterialTheme.typography.headlineMedium, color = c.ink) }
                Spacer(Modifier.width(4.dp))
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(category, style = MaterialTheme.typography.headlineMedium, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
        item { MonthSwitcher(m.label(), m < YearMonth.now(), { vm.changeMonth(-1) }, { vm.changeMonth(1) }) }
        item {
            HeroTile(Modifier.fillMaxWidth()) {
                Text("$category in ${m.name()}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                AnimatedRupees(spent, MaterialTheme.typography.displaySmall, Color.White, Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(16.dp))
                Row {
                    Stat("Share of spending", share?.let { "${"%.0f".format(it * 100)}%" } ?: "—")
                    Stat("Usually", typical?.let { rupees(it) } ?: "—")
                    if (isCurrent) Stat("Month-end", forecast?.let { rupees(it.projected) } ?: "—")
                    else Stat("Payments", "${inCat.size}")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        typical == null -> "“Usually” appears once FinTrack has 2 months of your history."
                        isCurrent && forecast != null && forecast.projected > typical * 1.15 ->
                            "Heading ${"%.0f".format((forecast.projected / typical - 1) * 100)}% above your usual month."
                        isCurrent && forecast != null && forecast.projected < typical * 0.85 ->
                            "Heading ${"%.0f".format((1 - forecast.projected / typical) * 100)}% below your usual month."
                        spent > typical * 1.15 -> "${"%.0f".format((spent / typical - 1) * 100)}% above your usual month."
                        else -> "In line with your usual month."
                    },
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        item {
            Tile(Modifier.fillMaxWidth()) {
                SectionTitle("Last ${trend.size} months")
                Spacer(Modifier.height(14.dp))
                MonthlyBars(trend, m, typical, color)
            }
        }
        item {
            Tile(Modifier.fillMaxWidth()) {
                SectionTitle("Day by day")
                Spacer(Modifier.height(14.dp))
                DailyBars(days, if (spent > 0) spent / (if (isCurrent) today.dayOfMonth else m.lengthOfMonth()) else 0.0,
                    selected = null, onSelect = {}, color = color)
            }
        }
        if (unusual.isNotEmpty()) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    SectionTitle("Unusual this month")
                    Spacer(Modifier.height(4.dp))
                    Faint("Much bigger than what you normally pay in $category")
                    Spacer(Modifier.height(10.dp))
                    unusual.forEach { u ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(u.txn.merchant, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1)
                                Faint("${u.txn.date.short()}  ·  usually about ${rupees(u.typicalAmount)}")
                            }
                            Text(rupees(u.txn.amount), style = MaterialTheme.typography.titleSmall.merge(MoneyStyle), color = c.loss)
                            Text("  ${"%.0f".format(u.times)}×", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                        }
                    }
                }
            }
        }
        if (merchants.isNotEmpty()) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    SectionTitle("Where in $category")
                    Spacer(Modifier.height(16.dp))
                    RankedList(merchants.take(8), spent, leadingAvatar = true, onClick = {})
                }
            }
        }
        item {
            Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                if (inCat.isEmpty()) {
                    Faint("No $category spending in ${m.name()}.", Modifier.padding(12.dp))
                }
                inCat.forEachIndexed { i, t ->
                    TxnRow(t, onToggleCounted = { vm.toggleCounted(t) }) { editing = t }
                    if (i < inCat.lastIndex) HorizontalDivider(Modifier.padding(start = 66.dp, end = 12.dp), color = c.track)
                }
            }
        }
    }
    editing?.let { t -> EditSheet(t, onDismiss = { editing = null }, vm = vm) }
}
