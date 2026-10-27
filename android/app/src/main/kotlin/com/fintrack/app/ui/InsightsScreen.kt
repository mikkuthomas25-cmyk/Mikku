package com.fintrack.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fintrack.core.MonthSummary
import com.fintrack.core.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun InsightsScreen(vm: FinTrackViewModel, openCategory: (String) -> Unit, openActivity: () -> Unit) {
    val c = Fin.colors
    val s = vm.summary
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Charts", style = MaterialTheme.typography.headlineMedium, color = c.ink, modifier = Modifier.weight(1f))
                MonthPicker(vm)
            }
        }
        if (s != null && s.txnCount > 0) item { QuickFacts(s) }
        if (s != null) item { PaceCard(vm, s) }
        item { TrendCard(vm) }
        if (s != null && s.spent > 0) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    SectionTitle("Daily spending")
                    Spacer(Modifier.height(14.dp))
                    val sel = (vm.filter as? TxnFilter.Day)?.date
                    DailyBars(s.byDay, s.avgPerDay, sel, onSelect = { vm.filter = it?.let(TxnFilter::Day) })
                    Spacer(Modifier.height(10.dp))
                    if (sel != null) {
                        val amount = s.byDay.firstOrNull { it.first == sel }?.second ?: 0.0
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).clickable(onClick = openActivity).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${sel.long()}  ·  ${rupees(amount)}", style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
                                color = c.ink, modifier = Modifier.weight(1f))
                            Text("View  ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Faint("Tap a bar to see that day  ·  dashed line = your daily average  ·  dot = today")
                    }
                }
            }
            item {
                Tile(Modifier.fillMaxWidth()) {
                    SectionTitle("Top merchants")
                    Spacer(Modifier.height(16.dp))
                    RankedList(s.topMerchants.take(5), s.spent, leadingAvatar = true, onClick = {
                        vm.kind = Kind.All
                        vm.filter = TxnFilter.Merchant(it)
                        openActivity()
                    })
                }
            }
        }
        if (vm.accountChecks.isNotEmpty()) item { AccountChecksCard(vm.accountChecks) }
        val unusual = Patterns.unusual(vm.history, vm.month)
        if (unusual.isNotEmpty()) {
            item {
                Tile(Modifier.fillMaxWidth()) {
                    SectionTitle("Unusual spends")
                    Spacer(Modifier.height(4.dp))
                    Faint("Much bigger than what you normally pay in that category. Learned from your history.")
                    Spacer(Modifier.height(10.dp))
                    unusual.take(6).forEach { u ->
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).clickable { openCategory(u.txn.category) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(u.txn.merchant, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1)
                                Faint("${u.txn.category}  ·  ${u.txn.date.short()}  ·  usually ~${rupees(u.typicalAmount)}")
                            }
                            Text(rupees(u.txn.amount), style = MaterialTheme.typography.titleSmall.merge(MoneyStyle), color = c.loss)
                        }
                    }
                }
            }
        }
        item { LearningCard(vm.learning) }
    }
}

/** Facts computed on the phone; no AI or network needed. */
@Composable
private fun QuickFacts(s: MonthSummary) {
    val c = Fin.colors
    val biggest = s.byDay.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val top = s.byCategory.firstOrNull()
    val facts = buildList {
        biggest?.let { add("Biggest day" to "${it.first.long()}  ·  ${rupees(it.second)}") }
        top?.let { add("Top category" to "${it.label}  ·  ${"%.0f".format(it.amount / s.spent * 100)}% of spending") }
        if (s.smallPaymentsCount > 0) {
            add("Small payments" to "${s.smallPaymentsCount} under ₹200 add up to ${rupees(s.smallPaymentsTotal)}")
        }
        if (s.prevMonthSpent > 0) {
            val diff = s.spent - s.prevMonthSpent
            add("vs ${s.month.minusMonths(1).name()}" to "${if (diff >= 0) "Up" else "Down"} ${rupees(abs(diff))}")
        }
        add("Saved" to if (s.net >= 0) rupees(s.net) else "Overspent by ${rupees(-s.net)}")
    }
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("At a glance")
        Spacer(Modifier.height(8.dp))
        facts.forEachIndexed { i, (label, value) ->
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                Text(value, style = MaterialTheme.typography.bodyLarge.merge(MoneyStyle), color = c.ink)
            }
            if (i < facts.lastIndex) HorizontalDivider(color = c.track)
        }
    }
}

/** What the on-device model has learned. Everything here is computed on the phone. */
@Composable
private fun LearningCard(stats: LearningStats?) {
    val white = Color.White
    HeroTile(Modifier.fillMaxWidth()) {
        Text("✦  Smart categories", style = MaterialTheme.typography.titleLarge, color = white)
        Spacer(Modifier.height(6.dp))
        Text(
            "FinTrack learns your categories on this phone, with nothing sent anywhere. Each time you correct one, " +
                "it applies the lesson to similar transactions.",
            style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.85f),
        )
        if (stats != null) {
            Spacer(Modifier.height(18.dp))
            Row {
                Stat("Taught by you", "${stats.taught}")
                Stat("Sorted by itself", "${stats.autoSorted}")
                Stat("Accuracy", stats.accuracy?.let { "${"%.0f".format(it * 100)}%" } ?: "—")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (stats.accuracy == null) "Correct at least 10 transactions in Activity to see how accurate it is on your spending."
                else "Accuracy = how often it would have guessed your own corrections right without being told.",
                style = MaterialTheme.typography.bodySmall, color = white.copy(alpha = 0.8f),
            )
        }
    }
}

/** Total spending for the last six months, with the learned "usual month" line. */
@Composable
private fun TrendCard(vm: FinTrackViewModel) {
    val m = vm.month
    val first = vm.history.minOfOrNull { YearMonth.from(it.date) } ?: return
    val months = generateSequence(maxOf(first, m.minusMonths(5))) { it.plusMonths(1) }.takeWhile { it <= m }.toList()
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Month by month")
        Spacer(Modifier.height(14.dp))
        MonthlyBars(Patterns.monthlyTotals(vm.history, months), m, Patterns.typicalMonthly(vm.history, m))
    }
}

/**
 * Bank balance check per account: the balances in bank SMS must be explained by the captured
 * transactions. A gap means a missed transaction (or a duplicate still counted).
 */
@Composable
private fun AccountChecksCard(checks: List<com.fintrack.core.AccountCheck>) {
    val c = Fin.colors
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Bank balance check")
        Spacer(Modifier.height(4.dp))
        Faint("Your bank SMS include your balance. FinTrack checks its transactions explain every change.")
        checks.forEach { a ->
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A/c ••${a.account}", style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
                Text(rupees(a.latestBalance), style = MaterialTheme.typography.titleSmall.merge(MoneyStyle), color = c.ink)
            }
            Faint("Balance as of ${a.latestDate.short()}")
            Spacer(Modifier.height(4.dp))
            if (a.ok) {
                Text(
                    if (a.checkedSteps > 0) "✓  ALL ${a.checkedSteps} BALANCE CHANGES EXPLAINED" else "NOT ENOUGH BALANCE SMS YET",
                    style = CapsLabel, color = if (a.checkedSteps > 0) c.gain else c.inkFaint,
                )
            } else {
                a.gaps.take(3).forEach { g ->
                    Text(
                        "⚠  ${rupees(abs(g.unexplained))} " + (if (g.missedIncome) "arrived" else "left") +
                            " between ${g.from.short()} and ${g.to.short()} that FinTrack didn't see" +
                            (if (g.missedIncome) "" else " (a missed payment, or a duplicate still counted)"),
                        style = MaterialTheme.typography.bodySmall, color = c.loss,
                    )
                }
            }
        }
    }
}

/** "Am I spending faster than usual?": running total vs last month, plus the learned forecast. */
@Composable
private fun PaceCard(vm: FinTrackViewModel, s: MonthSummary) {
    val c = Fin.colors
    val m = vm.month
    val today = LocalDate.now()
    val isCurrent = YearMonth.from(today) == m
    val thisMonth = Patterns.cumulative(vm.history, m, untilDay = if (isCurrent) today.dayOfMonth else null)
    val lastMonth = Patterns.cumulative(vm.history, m.minusMonths(1))
    val forecast = if (isCurrent) Patterns.forecast(vm.history, m, today) else null
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Spending pace")
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                forecast != null && forecast.basedOnMonths >= 2 ->
                    "On track for ${rupees(forecast.projected)} by month-end, based on how your last ${forecast.basedOnMonths} months went."
                forecast != null -> "At this pace: about ${rupees(forecast.projected)} by month-end."
                isCurrent -> "A forecast appears after a week of data."
                else -> "${rupees(s.spent)} spent in ${m.name()}."
            },
            style = MaterialTheme.typography.bodyMedium, color = c.inkSoft,
        )
        if (isCurrent && lastMonth.isNotEmpty() && thisMonth.isNotEmpty()) {
            val sameDayLast = lastMonth.getOrNull(thisMonth.size - 1) ?: lastMonth.last()
            val diff = thisMonth.last() - sameDayLast
            if (sameDayLast > 0) {
                Text(
                    "${if (diff > 0) "▲" else "▼"} ${rupees(kotlin.math.abs(diff))} ${if (diff > 0) "more" else "less"} than by this day last month",
                    style = MaterialTheme.typography.labelLarge, color = if (diff > 0) c.loss else c.gain,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PaceChart(thisMonth, lastMonth, m.lengthOfMonth(), forecast?.projected)
    }
}
