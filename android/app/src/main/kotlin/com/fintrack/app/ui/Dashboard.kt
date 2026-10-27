package com.fintrack.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.MonthSummary
import com.fintrack.core.Patterns
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/** FinTrack mark on the left; privacy eye and profile on the right. */
@Composable
fun DashboardTopBar(onProfile: () -> Unit) {
    val c = Fin.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(16.dp)) {
            val p = Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(p, c.accent, style = Stroke(width = 1.6.dp.toPx()))
        }
        Spacer(Modifier.width(8.dp))
        Text("FINTRACK", style = CapsLabel.copy(fontFamily = FontFamily.Serif, fontSize = 13.sp, letterSpacing = 3.sp), color = c.ink,
            modifier = Modifier.weight(1f))
        EyeToggle()
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(36.dp).clip(CircleShape).border(0.5.dp, c.border, CircleShape).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Person, "Settings", tint = c.inkSoft, modifier = Modifier.size(20.dp)) }
    }
}

/** Privacy mode: hides every amount until tapped again. A crossed eye while hidden. */
@Composable
private fun EyeToggle() {
    val c = Fin.colors
    val hidden = Privacy.hidden
    Box(
        Modifier.size(36.dp).clip(CircleShape).border(0.5.dp, if (hidden) c.accent else c.border, CircleShape)
            .clickable { Privacy.hidden = !Privacy.hidden },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val col = if (hidden) c.accent else c.inkSoft
            val w = size.width
            val h = size.height
            val eye = Path().apply {
                moveTo(0f, h / 2)
                quadraticTo(w / 2, -h * 0.15f, w, h / 2)
                quadraticTo(w / 2, h * 1.15f, 0f, h / 2)
                close()
            }
            drawPath(eye, col, style = Stroke(1.4.dp.toPx()))
            drawCircle(col, radius = h * 0.16f, center = center)
            if (hidden) drawLine(col, Offset(w * 0.1f, h * 0.9f), Offset(w * 0.9f, h * 0.1f), 1.6.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/**
 * The headline: total bank balance in a large serif with the paise smaller and dimmer, and a glowing
 * badge for what's left this month (money in − money out). Without a known balance, "left this
 * month" is the headline.
 */
@Composable
fun BalanceHeader(vm: FinTrackViewModel, s: MonthSummary) {
    val c = Fin.colors
    val total = vm.balances.takeIf { it.isNotEmpty() }?.sumOf { it.amount }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (total != null) "TOTAL BALANCE" else "LEFT IN ${s.month.name().uppercase()}", style = CapsLabel, color = c.inkSoft)
        Spacer(Modifier.height(6.dp))
        val (whole, paise) = rupeesSplit(abs(total ?: s.net))
        Row(verticalAlignment = Alignment.Bottom) {
            if ((total ?: s.net) < 0) Text("−", style = BalanceStyle, fontSize = 44.sp, color = c.ink)
            Text(whole, style = BalanceStyle, fontSize = 46.sp, color = c.ink, maxLines = 1)
            Text(paise, style = BalanceStyle, fontSize = 24.sp, color = c.inkFaint, modifier = Modifier.padding(bottom = 7.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (total != null) {
            val up = s.net >= 0
            val col = if (up) c.gain else c.loss
            Text(
                (if (up) "+" else "−") + rupees(abs(s.net)) + " this month",
                style = MaterialTheme.typography.labelMedium.merge(MoneyStyle), color = col,
                modifier = Modifier.glow(col, 8.dp).clip(PillShape).background(col.copy(alpha = 0.12f))
                    .border(0.5.dp, col.copy(alpha = 0.45f), PillShape).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        } else {
            Text("Add a bank statement to see your balance", style = MaterialTheme.typography.labelMedium, color = c.inkFaint)
        }
    }
}

/**
 * Pacing: this month's spending against your usual month (learned from your history), with a
 * TODAY marker where you'd normally be by now. Falls back to money in when there's no history.
 */
@Composable
fun PacingCard(vm: FinTrackViewModel, s: MonthSummary) {
    val c = Fin.colors
    val m = vm.month
    val usual = Patterns.typicalMonthly(vm.history, m)
    val limit = usual ?: s.received.takeIf { it > 0 } ?: return
    val today = LocalDate.now()
    val dayShare = if (YearMonth.from(today) == m) today.dayOfMonth.toFloat() / m.lengthOfMonth() else 1f
    val used = (s.spent / limit).toFloat()
    val onTrack = s.spent <= limit * dayShare * 1.05
    Tile(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pacing", style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
            if (dayShare < 1f) Text("TODAY", style = CapsLabel.copy(fontSize = 9.sp), color = c.inkSoft)
        }
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val r = size.height / 2
            drawRoundRect(c.track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
            val w = size.width * used.coerceIn(0f, 1f)
            if (w > 0) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0.6f), if (onTrack) c.accent else c.amber)),
                    size = Size(w, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                )
            }
            if (dayShare < 1f) {
                val x = size.width * dayShare
                drawLine(c.ink, Offset(x, -4.dp.toPx()), Offset(x, size.height + 4.dp.toPx()), 1.5.dp.toPx())
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${m.name()} · ${if (usual != null) "your usual ${rupees(usual)}" else "money in ${rupees(limit)}"}",
                style = MaterialTheme.typography.bodySmall, color = c.inkSoft, modifier = Modifier.weight(1f), maxLines = 1,
            )
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (onTrack) c.accent else c.amber))
            Spacer(Modifier.width(6.dp))
            Text(
                if (onTrack) "On track (${rupees((limit - s.spent).coerceAtLeast(0.0))} left)" else "Ahead of your usual pace",
                style = MaterialTheme.typography.bodySmall.merge(MoneyStyle), color = if (onTrack) c.accent else c.amber,
            )
        }
    }
}

/** Cash flow: income and spending over the last six months as smooth glowing curves. */
@Composable
fun CashFlowCard(vm: FinTrackViewModel) {
    val c = Fin.colors
    val months = (5 downTo 0).map { vm.month.minusMonths(it.toLong()) }
    val income = months.map { ym -> vm.history.filter { YearMonth.from(it.date) == ym && it.effectiveFlow.countsAsMoneyIn }.sumOf { it.amount } }
    val spent = months.map { ym -> vm.history.filter { YearMonth.from(it.date) == ym }.sumOf { it.outValue }.coerceAtLeast(0.0) }
    val top = (income + spent).maxOrNull()?.takeIf { it > 0 } ?: return
    Tile(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cash Flow", style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
            Text(
                "Income vs. Spent", style = MaterialTheme.typography.labelSmall, color = c.inkSoft,
                modifier = Modifier.clip(PillShape).border(0.5.dp, c.border, PillShape).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            fun points(values: List<Double>) = values.mapIndexed { i, v ->
                Offset(size.width * i / (values.size - 1).coerceAtLeast(1), size.height - (v / top).toFloat() * size.height * 0.9f)
            }
            fun curve(p: List<Offset>) = Path().apply {
                moveTo(p.first().x, p.first().y)
                for (i in 1 until p.size) {
                    val midX = (p[i - 1].x + p[i].x) / 2
                    cubicTo(midX, p[i - 1].y, midX, p[i].y, p[i].x, p[i].y)
                }
            }
            val inP = points(income)
            val outP = points(spent)
            val inPath = curve(inP)
            // Soft fill under the income curve, then a wide faint stroke for the glow.
            drawPath(Path().apply { addPath(inPath); lineTo(size.width, size.height); lineTo(0f, size.height); close() },
                Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.18f), Color.Transparent)))
            drawPath(inPath, c.accent.copy(alpha = 0.18f), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            drawPath(inPath, c.accent, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
            drawPath(curve(outP), c.amber.copy(alpha = 0.85f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            inP.forEach { drawCircle(c.accent, 2.6.dp.toPx(), it) }
            outP.forEach { drawCircle(c.amber, 2.dp.toPx(), it) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            months.forEach { ym ->
                Text(ym.name().take(3), style = MaterialTheme.typography.labelSmall, color = if (ym == vm.month) c.ink else c.inkFaint,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(c.accent, "Income ${rupees(income.last())}")
            Spacer(Modifier.width(16.dp))
            LegendDot(c.amber, "Spent ${rupees(spent.last())}")
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(6.dp))
    Text(text, style = MaterialTheme.typography.labelSmall.merge(MoneyStyle), color = Fin.colors.inkSoft)
}

/** Bento row: quick actions on the left, the top two categories as rings on the right. */
@Composable
fun BentoRow(s: MonthSummary, onAdd: () -> Unit, onImport: () -> Unit, onCharts: () -> Unit, openCategory: (String) -> Unit) {
    val c = Fin.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, color = c.ink)
            Spacer(Modifier.height(10.dp))
            QuickAction("+", "Add expense", onAdd)
            QuickAction("⇩", "Import statement", onImport)
            QuickAction("▥", "Charts", onCharts)
        }
        Tile(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
            Text("Top Categories", style = MaterialTheme.typography.titleSmall, color = c.ink)
            Spacer(Modifier.height(12.dp))
            val top = s.byCategory.take(2)
            if (top.isEmpty()) {
                Faint("Nothing spent yet")
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    top.forEachIndexed { i, a ->
                        Ring(a.label, (a.amount / s.spent).toFloat(), if (i == 0) c.accent else c.amber) { openCategory(a.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(symbol: String, label: String, onClick: () -> Unit) {
    val c = Fin.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(c.surfaceRaised).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, color = c.accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Ring(label: String, share: Float, color: Color, onClick: () -> Unit) {
    val c = Fin.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(58.dp)) {
                val stroke = 5.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(c.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(color.copy(alpha = 0.25f), -90f, 360f * share.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
                drawArc(color, -90f, 360f * share.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("${"%.0f".format(share * 100)}%", style = MaterialTheme.typography.labelSmall.merge(MoneyStyle), color = c.ink)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Recent activity, grouped by day, with a category symbol per row and income in green. */
@Composable
fun RecentActivity(txns: List<Txn>, onOpen: (Txn) -> Unit, onSeeAll: () -> Unit) {
    val c = Fin.colors
    val recent = txns.filter { it.effectiveFlow.counted }.sortedWith(compareByDescending<Txn> { it.date }.thenByDescending { it.id }).take(6)
    if (recent.isEmpty()) return
    Tile(Modifier.fillMaxWidth(), padding = 14.dp) {
        SectionTitle("Recent Activity", action = "See all", onAction = onSeeAll)
        recent.groupBy { it.date }.forEach { (date, rows) ->
            Text(date.relative(), style = MaterialTheme.typography.labelMedium, color = c.inkFaint, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            rows.forEachIndexed { i, t ->
                ActivityRow(t) { onOpen(t) }
                if (i < rows.lastIndex) HorizontalDivider(Modifier.padding(start = 54.dp), thickness = 0.5.dp, color = c.border)
            }
        }
    }
}

@Composable
private fun ActivityRow(t: Txn, onClick: () -> Unit) {
    val c = Fin.colors
    val incoming = t.direction == Direction.CREDIT
    val income = t.effectiveFlow.countsAsMoneyIn
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (income) Avatar(t.merchant, size = 40.dp, symbol = "↙", tint = c.accent)
        else Avatar(t.merchant, size = 40.dp, symbol = categorySymbol(if (t.effectiveFlow == Flow.SPEND) t.category else "Transfers"), tint = categoryColor(t.category))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(labelOf(t), style = MaterialTheme.typography.bodySmall, color = c.inkFaint, maxLines = 1)
        }
        Text(
            (if (incoming) "+" else "") + rupees(t.amount),
            style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
            color = if (income) c.gain else c.ink,
        )
    }
}
