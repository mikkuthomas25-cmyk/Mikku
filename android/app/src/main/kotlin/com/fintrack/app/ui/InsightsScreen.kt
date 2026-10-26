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
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun InsightsScreen(vm: FinTrackViewModel) {
    val c = Fin.colors
    val s = vm.summary
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Insights", style = MaterialTheme.typography.headlineMedium, color = c.ink, modifier = Modifier.weight(1f))
                MonthSwitcher(vm.month.name(), vm.month < YearMonth.now(), { vm.changeMonth(-1) }, { vm.changeMonth(1) })
            }
        }
        if (s != null && s.txnCount > 0) item { QuickFacts(s) }
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
    GlassCard(Modifier.fillMaxWidth()) {
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
    HeroCard(Modifier.fillMaxWidth()) {
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
