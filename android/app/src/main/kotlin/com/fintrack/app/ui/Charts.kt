package com.fintrack.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.core.Amount
import java.time.LocalDate

/**
 * One bar per day with a ₹ axis. Tap a bar to select that day (tap again to clear).
 * A dashed line marks the daily average; today's bar is outlined.
 */
@Composable
fun DailyBars(
    days: List<Pair<LocalDate, Double>>,
    average: Double,
    selected: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val c = Fin.colors
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = c.inkFaint)
    val top = niceCeil(days.maxOfOrNull { it.second } ?: 0.0)
    val today = LocalDate.now()
    val grow = remember(days) { Animatable(0f) }
    LaunchedEffect(days) { grow.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val left = 44.dp

    Canvas(
        modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(days, selected) {
                detectTapGestures { pos ->
                    val l = left.toPx()
                    val slot = (size.width - l) / days.size
                    val day = days.getOrNull(((pos.x - l) / slot).toInt())?.first
                    onSelect(if (day == null || day == selected) null else day)
                }
            },
    ) {
        val l = left.toPx()
        val padB = 18.dp.toPx()
        val plotTop = 6.dp.toPx()
        val plotBottom = size.height - padB
        val plotH = plotBottom - plotTop
        for (f in listOf(0.0, 0.5, 1.0)) {
            val y = plotBottom - plotH * f.toFloat()
            drawLine(c.track, Offset(l, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val m = measurer.measure(shortRupees(top * f), axisStyle)
            drawText(m, topLeft = Offset(l - m.size.width - 6.dp.toPx(), y - m.size.height / 2))
        }
        val slot = (size.width - l) / days.size
        val bw = (slot * 0.6f).coerceIn(3.dp.toPx(), 12.dp.toPx())
        val brush = if (color != null) Brush.verticalGradient(listOf(color, color))
        else Brush.verticalGradient(listOf(c.accent, c.accent), startY = plotTop, endY = plotBottom)

        days.forEachIndexed { i, (date, amount) ->
            val x = l + i * slot + (slot - bw) / 2
            val h = (plotH * (amount / top).toFloat() * grow.value).coerceAtLeast(if (amount > 0) 3.dp.toPx() else 0f)
            if (h > 0) {
                val dim = selected != null && selected != date
                drawRoundRect(brush, Offset(x, plotBottom - h), Size(bw, h), CornerRadius(1.dp.toPx()), alpha = if (dim) 0.3f else 1f)
            }
            if (date == today) {
                drawCircle(c.accent, 2.5.dp.toPx(), Offset(x + bw / 2, plotBottom + 4.dp.toPx()))
            }
            val d = date.dayOfMonth
            if (d == 1 || d % 7 == 0) {
                val m = measurer.measure("$d", axisStyle)
                drawText(m, topLeft = Offset(x + bw / 2 - m.size.width / 2, size.height - m.size.height))
            }
        }
        if (average > 0) {
            val y = plotBottom - plotH * (average / top).toFloat()
            drawLine(
                c.inkSoft, Offset(l, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
    }
}

/** Ranked rows with a slim gradient progress line under each. */
@Composable
fun RankedList(items: List<Amount>, total: Double, onClick: (String) -> Unit, leadingAvatar: Boolean = false) {
    val c = Fin.colors
    if (items.isEmpty()) {
        Faint("Nothing spent yet this month.")
        return
    }
    val top = items.first().amount
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { item ->
            val frac by animateFloatAsState((item.amount / top).toFloat(), tween(600), label = "rank")
            Row(
                Modifier.fillMaxWidth().clip(TileShape).clickable { onClick(item.label) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingAvatar) {
                    Avatar(item.label, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.label, style = MaterialTheme.typography.bodyLarge, color = c.ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(rupees(item.amount), style = MaterialTheme.typography.bodyLarge.merge(MoneyStyle), color = c.ink)
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.weight(1f).height(4.dp)) {
                            drawRoundRect(c.track, cornerRadius = CornerRadius(size.height))
                            drawRoundRect(
                                Brush.horizontalGradient(listOf(c.accent, c.accent)),
                                size = Size(size.width * frac, size.height), cornerRadius = CornerRadius(size.height),
                            )
                        }
                        val pct = if (total > 0) item.amount / total * 100 else 0.0
                        Text(
                            if (leadingAvatar) "${item.count}×" else "${"%.0f".format(pct)}%",
                            style = MaterialTheme.typography.labelSmall, color = c.inkFaint,
                            modifier = Modifier.width(40.dp).padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Rupee amount that counts up to its new value. */
@Composable
fun AnimatedRupees(value: Double, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(value.toFloat(), tween(700, easing = FastOutSlowInEasing), label = "rupees")
    Text(rupees(shown.toDouble()), style = style.merge(MoneyStyle), color = color, modifier = modifier, maxLines = 1)
}
