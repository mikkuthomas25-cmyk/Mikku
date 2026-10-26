package com.fintrack.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.core.Amount
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** One bar per day of the month. Tapping a bar selects that day. */
@Composable
fun DailyBars(
    days: List<Pair<LocalDate, Double>>,
    selected: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bar = MaterialTheme.colorScheme.primary
    val barDim = MaterialTheme.colorScheme.primaryContainer
    val grid = MaterialTheme.colorScheme.outlineVariant
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = axis)
    val max = days.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val top = remember(max) { val p = 10.0.pow(floor(log10(max))); ceil(max / p) * p }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(days, selected) {
                detectTapGestures { pos ->
                    val padL = 40.dp.toPx()
                    val slot = (size.width - padL) / days.size
                    val i = ((pos.x - padL) / slot).toInt()
                    val day = days.getOrNull(i)?.first
                    onSelect(if (day == null || day == selected) null else day)
                }
            },
    ) {
        val padL = 40.dp.toPx()
        val padB = 18.dp.toPx()
        val padT = 6.dp.toPx()
        val plotH = size.height - padB - padT
        fun y(v: Double) = padT + plotH * (1 - v / top).toFloat()

        for (f in listOf(0.0, 0.5, 1.0)) {
            val yy = y(top * f)
            drawLine(grid, Offset(padL, yy), Offset(size.width, yy), strokeWidth = 1.dp.toPx())
            val v = top * f
            val label = if (v >= 1000) "₹${"%.${if (v % 1000 == 0.0) 0 else 1}f".format(v / 1000)}k" else "₹${v.toInt()}"
            val m = measurer.measure(label, axisStyle)
            drawText(m, topLeft = Offset(padL - m.size.width - 4.dp.toPx(), yy - m.size.height / 2))
        }

        val slot = (size.width - padL) / days.size
        val bw = (slot - 2.dp.toPx()).coerceIn(2f, 18.dp.toPx())
        days.forEachIndexed { i, (date, amount) ->
            val x = padL + i * slot + (slot - bw) / 2
            if (amount > 0) {
                val t = y(amount)
                val base = size.height - padB
                val r = minOf(4.dp.toPx(), bw / 2, base - t)
                // Rounded top, square baseline.
                val path = Path().apply {
                    moveTo(x, base)
                    lineTo(x, t + r)
                    quadraticTo(x, t, x + r, t)
                    lineTo(x + bw - r, t)
                    quadraticTo(x + bw, t, x + bw, t + r)
                    lineTo(x + bw, base)
                    close()
                }
                drawPath(path, if (selected == null || selected == date) bar else barDim)
            }
            val d = date.dayOfMonth
            if (d == 1 || d % 5 == 0) {
                val m = measurer.measure("$d", axisStyle)
                drawText(m, topLeft = Offset(x + bw / 2 - m.size.width / 2, size.height - m.size.height))
            }
        }
    }
}

/** Ranked horizontal bars (categories, merchants). */
@Composable
fun BarList(items: List<Amount>, selected: String?, showCount: Boolean, onClick: (String) -> Unit) {
    if (items.isEmpty()) {
        Text("No spending yet", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        return
    }
    val max = items.first().amount
    val fill = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            val isSel = item.label == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) track else Color.Transparent)
                    .clickable { onClick(item.label) }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        item.label + if (showCount) " · ${item.count}×" else "",
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSel) FontWeight.SemiBold else null,
                    )
                    Text(rupees(item.amount), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Canvas(Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp)) {
                    val w = (size.width * (item.amount / max).toFloat()).coerceAtLeast(4.dp.toPx())
                    drawRoundRect(fill, size = Size(w, size.height), cornerRadius = CornerRadius(3.dp.toPx()))
                }
            }
        }
    }
}
