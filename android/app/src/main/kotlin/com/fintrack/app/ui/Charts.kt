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
 * One gradient bar per day. Tap a bar to select the day; tap again to clear.
 * A dashed line marks the daily average.
 */
@Composable
fun DailyBars(
    days: List<Pair<LocalDate, Double>>,
    average: Double,
    selected: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Fin.colors
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = c.inkFaint)
    val max = days.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    // Bars grow in when the month changes.
    val grow = remember(days) { Animatable(0f) }
    LaunchedEffect(days) { grow.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(days, selected) {
                detectTapGestures { pos ->
                    val slot = size.width.toFloat() / days.size
                    val day = days.getOrNull((pos.x / slot).toInt())?.first
                    onSelect(if (day == null || day == selected) null else day)
                }
            },
    ) {
        val padB = 18.dp.toPx()
        val plotH = size.height - padB
        val slot = size.width / days.size
        val bw = (slot * 0.56f).coerceIn(3.dp.toPx(), 12.dp.toPx())
        val gradient = Brush.verticalGradient(listOf(c.heroEnd, c.heroStart), startY = 0f, endY = plotH)

        days.forEachIndexed { i, (date, amount) ->
            val x = i * slot + (slot - bw) / 2
            val h = (plotH * (amount / max).toFloat() * grow.value).coerceAtLeast(if (amount > 0) 3.dp.toPx() else 0f)
            // Faint full-height track keeps the rhythm of the month visible on quiet days.
            drawRoundRect(c.track, Offset(x, 0f), Size(bw, plotH), CornerRadius(bw / 2))
            if (h > 0) {
                val dim = selected != null && selected != date
                drawRoundRect(
                    gradient, Offset(x, plotH - h), Size(bw, h), CornerRadius(bw / 2),
                    alpha = if (dim) 0.3f else 1f,
                )
            }
            val d = date.dayOfMonth
            if (d == 1 || d % 7 == 0) {
                val m = measurer.measure("$d", axisStyle)
                drawText(m, topLeft = Offset(x + bw / 2 - m.size.width / 2, size.height - m.size.height))
            }
        }
        if (average > 0) {
            val y = plotH - plotH * (average / max).toFloat()
            drawLine(
                c.inkFaint, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
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
                Modifier.fillMaxWidth().clip(CardShape).clickable { onClick(item.label) },
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
                                Brush.horizontalGradient(listOf(c.heroStart, c.heroEnd)),
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
