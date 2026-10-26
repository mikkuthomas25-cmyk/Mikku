package com.fintrack.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.core.Amount
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

/** Compact rupees for axis labels: ₹800, ₹1.5k, ₹12k, ₹1.2L. */
fun shortRupees(v: Double): String = when {
    v >= 100_000 -> "₹${"%.1f".format(v / 100_000).removeSuffix(".0")}L"
    v >= 1_000 -> "₹${"%.1f".format(v / 1_000).removeSuffix(".0")}k"
    else -> "₹${v.toInt()}"
}

/** A round number at or above [max] for the top of an axis. */
fun niceCeil(max: Double): Double {
    if (max <= 0) return 1.0
    val p = 10.0.pow(floor(log10(max)))
    val m = max / p
    val step = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0).first { it >= m - 1e-9 }
    return step * p
}

private fun DrawScope.yAxis(top: Double, plotTop: Float, plotBottom: Float, left: Float, measurer: TextMeasurer, style: TextStyle, grid: Color) {
    for (f in listOf(0.0, 0.5, 1.0)) {
        val y = plotBottom - (plotBottom - plotTop) * f.toFloat()
        drawLine(grid, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        val m = measurer.measure(shortRupees(top * f), style)
        drawText(m, topLeft = Offset(left - m.size.width - 6.dp.toPx(), y - m.size.height / 2))
    }
}

data class Slice(val label: String, val amount: Double, val color: Color)

/**
 * Where the money went: a donut with a labelled legend underneath. Tapping a slice or a legend
 * row opens that category. Up to six categories get their own slice; the rest are grouped.
 */
@Composable
fun CategoryDonut(categories: List<Amount>, total: Double, onOpen: (String) -> Unit) {
    val c = Fin.colors
    if (categories.isEmpty() || total <= 0) {
        Faint("Nothing spent yet this month.")
        return
    }
    val top = categories.take(6)
    val rest = categories.drop(6)
    val slices = top.map { Slice(it.label, it.amount, categoryColor(it.label)) } +
        if (rest.isNotEmpty()) listOf(Slice("${rest.size} more", rest.sumOf { it.amount }, c.inkFaint)) else emptyList()
    val sweep = remember(categories) { Animatable(0f) }
    LaunchedEffect(categories) { sweep.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val surface = MaterialTheme.colorScheme.surface

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(slices) {
                    detectTapGestures { pos ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.width / 2f
                        val d = hypot(pos.x - cx, pos.y - cy)
                        if (d < r * 0.55f || d > r) return@detectTapGestures
                        var angle = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())) + 90
                        if (angle < 0) angle += 360
                        var acc = 0.0
                        for (s in slices) {
                            acc += s.amount / total * 360
                            if (angle <= acc) {
                                if (!s.label.endsWith(" more")) onOpen(s.label)
                                return@detectTapGestures
                            }
                        }
                    }
                },
            ) {
                val stroke = size.width * 0.16f
                val inset = stroke / 2
                var start = -90f
                for (s in slices) {
                    val angle = (s.amount / total * 360).toFloat() * sweep.value
                    // A thin surface-coloured gap separates neighbouring slices.
                    drawArc(
                        s.color, start, angle, useCenter = false,
                        topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                    drawArc(
                        surface, start + angle - 0.6f, 1.2f, useCenter = false,
                        topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke + 2f),
                    )
                    start += angle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rupees(total), style = MaterialTheme.typography.titleLarge.merge(MoneyStyle), color = c.ink)
                Text("spent", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            categories.forEach { cat ->
                Row(
                    Modifier.fillMaxWidth().clip(CardShape).clickable { onOpen(cat.label) }.padding(vertical = 9.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(categoryColor(cat.label)))
                    Spacer(Modifier.width(12.dp))
                    Text(cat.label, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${"%.0f".format(cat.amount / total * 100)}%", style = MaterialTheme.typography.labelLarge,
                        color = c.inkSoft, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                    Text(rupees(cat.amount), style = MaterialTheme.typography.bodyLarge.merge(MoneyStyle), color = c.ink,
                        modifier = Modifier.width(96.dp), textAlign = TextAlign.End)
                    Text("  ›", color = c.inkFaint)
                }
            }
        }
    }
}

/**
 * Running total through the month: this month (solid), last month (dashed grey) and, for the
 * current month, a dotted line to the forecast month-end total. Tap to read any day.
 */
@Composable
fun PaceChart(thisMonth: List<Double>, lastMonth: List<Double>, daysInMonth: Int, forecastEnd: Double?) {
    val c = Fin.colors
    val measurer = rememberTextMeasurer()
    val axis = TextStyle(fontSize = 10.sp, color = c.inkFaint)
    var selected by remember(thisMonth) { mutableStateOf<Int?>(null) }
    val top = niceCeil(listOfNotNull(thisMonth.maxOrNull(), lastMonth.maxOrNull(), forecastEnd).maxOrNull() ?: 1.0)
    val padL = 44.dp
    val primary = c.heroStart

    Column {
        Canvas(
            Modifier.fillMaxWidth().height(170.dp).pointerInput(daysInMonth) {
                detectTapGestures { pos ->
                    val left = padL.toPx()
                    val day = (((pos.x - left) / (size.width - left)) * (daysInMonth - 1)).toInt().coerceIn(0, daysInMonth - 1)
                    selected = if (selected == day) null else day
                }
            },
        ) {
            val left = padL.toPx()
            val padB = 18.dp.toPx()
            val plotTop = 6.dp.toPx()
            val plotBottom = size.height - padB
            fun x(day: Int) = left + (size.width - left) * day / (daysInMonth - 1).coerceAtLeast(1)
            fun y(v: Double) = plotBottom - (plotBottom - plotTop) * (v / top).toFloat()

            yAxis(top, plotTop, plotBottom, left, measurer, axis, c.track)
            for (d in listOf(1, 8, 15, 22, 29).filter { it <= daysInMonth }) {
                val m = measurer.measure("$d", axis)
                drawText(m, topLeft = Offset(x(d - 1) - m.size.width / 2, size.height - m.size.height))
            }
            fun line(values: List<Double>, color: Color, effect: PathEffect?, width: Float) {
                if (values.isEmpty()) return
                val p = Path()
                values.forEachIndexed { i, v -> if (i == 0) p.moveTo(x(i), y(v)) else p.lineTo(x(i), y(v)) }
                drawPath(p, color, style = Stroke(width = width, cap = StrokeCap.Round, pathEffect = effect))
            }
            line(lastMonth.take(daysInMonth), c.inkFaint, PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())), 2.dp.toPx())
            if (forecastEnd != null && thisMonth.isNotEmpty() && thisMonth.size < daysInMonth) {
                val from = thisMonth.size - 1
                drawLine(primary.copy(alpha = 0.6f), Offset(x(from), y(thisMonth.last())), Offset(x(daysInMonth - 1), y(forecastEnd)),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())))
                drawCircle(primary.copy(alpha = 0.6f), 4.dp.toPx(), Offset(x(daysInMonth - 1), y(forecastEnd)))
            }
            line(thisMonth, primary, null, 2.5.dp.toPx())
            if (thisMonth.isNotEmpty()) {
                drawCircle(Color.White, 5.dp.toPx(), Offset(x(thisMonth.size - 1), y(thisMonth.last())))
                drawCircle(primary, 3.5.dp.toPx(), Offset(x(thisMonth.size - 1), y(thisMonth.last())))
            }
            selected?.let { d ->
                drawLine(c.inkSoft, Offset(x(d), plotTop), Offset(x(d), plotBottom), strokeWidth = 1.dp.toPx())
                thisMonth.getOrNull(d)?.let { drawCircle(primary, 4.dp.toPx(), Offset(x(d), y(it))) }
                lastMonth.getOrNull(d)?.let { drawCircle(c.inkFaint, 4.dp.toPx(), Offset(x(d), y(it))) }
            }
        }
        Spacer(Modifier.height(8.dp))
        val d = selected
        if (d != null) {
            Text(
                "Day ${d + 1}:  " + listOfNotNull(
                    thisMonth.getOrNull(d)?.let { "this month ${rupees(it)}" },
                    lastMonth.getOrNull(d)?.let { "last month ${rupees(it)}" },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge.merge(MoneyStyle), color = c.ink,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendKey("This month", primary, dashed = false)
                LegendKey("Last month", c.inkFaint, dashed = true)
                if (forecastEnd != null) LegendKey("Forecast", primary.copy(alpha = 0.6f), dashed = true)
            }
        }
    }
}

@Composable
private fun LegendKey(label: String, color: Color, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(18.dp).height(8.dp)) {
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Fin.colors.inkSoft)
    }
}

private val MONTH_SHORT = DateTimeFormatter.ofPattern("MMM")

/** One bar per month, the selected month highlighted, a dashed line at the user's typical month. */
@Composable
fun MonthlyBars(months: List<Pair<YearMonth, Double>>, highlight: YearMonth, typical: Double?, color: Color = Fin.colors.heroStart) {
    val c = Fin.colors
    val measurer = rememberTextMeasurer()
    val axis = TextStyle(fontSize = 10.sp, color = c.inkFaint)
    val value = TextStyle(fontSize = 10.sp, color = c.ink, fontWeight = FontWeight.SemiBold)
    val top = niceCeil(listOfNotNull(months.maxOfOrNull { it.second }, typical).maxOrNull() ?: 1.0)
    val grow = remember(months) { Animatable(0f) }
    LaunchedEffect(months) { grow.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }

    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val left = 44.dp.toPx()
            val padB = 18.dp.toPx()
            val plotTop = 14.dp.toPx()
            val plotBottom = size.height - padB
            yAxis(top, plotTop, plotBottom, left, measurer, axis, c.track)
            val slot = (size.width - left) / months.size
            val bw = (slot * 0.5f).coerceAtMost(28.dp.toPx())
            months.forEachIndexed { i, (m, v) ->
                val x = left + i * slot + (slot - bw) / 2
                val h = (plotBottom - plotTop) * (v / top).toFloat() * grow.value
                val isSel = m == highlight
                if (h > 0) {
                    drawRoundRect(color.copy(alpha = if (isSel) 1f else 0.35f), Offset(x, plotBottom - h), Size(bw, h),
                        CornerRadius(4.dp.toPx()))
                }
                val label = measurer.measure(m.format(MONTH_SHORT), axis)
                drawText(label, topLeft = Offset(x + bw / 2 - label.size.width / 2, size.height - label.size.height))
                if (isSel && v > 0) {
                    val t = measurer.measure(shortRupees(v), value)
                    drawText(t, topLeft = Offset(x + bw / 2 - t.size.width / 2, plotBottom - h - t.size.height - 2.dp.toPx()))
                }
            }
            if (typical != null && typical > 0) {
                val y = plotBottom - (plotBottom - plotTop) * (typical / top).toFloat()
                drawLine(c.inkSoft, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
            }
        }
        if (typical != null) {
            Spacer(Modifier.height(6.dp))
            Faint("Dashed line: your usual month, ${rupees(typical)} (learned from your history)")
        }
    }
}
