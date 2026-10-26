package com.fintrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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
        item {
            HeroCard(Modifier.fillMaxWidth()) {
                Text("✦  Ask Claude", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "A plain-language read of ${vm.month.name()}: where the money went and three realistic ways to save. " +
                        "Only totals are shared, never your messages.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(18.dp))
                val ready = !vm.busy && (s?.txnCount ?: 0) > 0
                Text(
                    if (vm.busy) "Thinking…" else "Analyse ${vm.month.name()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (ready) c.heroStart else c.inkFaint,
                    modifier = Modifier.clip(PillShape).background(Color.White)
                        .clickable(enabled = ready, onClick = vm::generateInsights)
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                )
                if (!vm.aiEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Text("Add your Claude API key in Profile to turn this on.",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        vm.insights?.let { text ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = c.ink)
                }
            }
        }
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
