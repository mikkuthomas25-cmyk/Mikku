package com.fintrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/**
 * A dropdown: a bordered box showing the current value and ▾; tapping it opens the list.
 * [label] is a small caption above the box.
 */
@Composable
fun <T> PopDropdown(
    value: String,
    options: List<T>,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val c = Fin.colors
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        if (label != null) {
            Text(label.uppercase(), style = CapsLabel, color = c.inkSoft)
            Spacer(Modifier.padding(top = 6.dp))
        }
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(0.5.dp, c.border, RoundedCornerShape(14.dp))
                    .background(c.surface).clickable { open = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("▾", style = MaterialTheme.typography.bodyLarge, color = c.inkSoft)
            }
            DropdownMenu(
                expanded = open, onDismissRequest = { open = false },
                modifier = Modifier.background(c.surfaceRaised).heightIn(max = 420.dp),
            ) {
                options.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(text(o), color = if (text(o) == value) c.accent else c.ink) },
                        onClick = { open = false; onSelect(o) },
                    )
                }
            }
        }
    }
}

/** The month: ‹ and › step through months; tapping the name opens a dropdown of the last 12. */
@Composable
fun MonthPicker(vm: FinTrackViewModel, long: Boolean = false) {
    val c = Fin.colors
    val now = YearMonth.now()
    var open by remember { mutableStateOf(false) }
    Row(Modifier.clip(PillShape).border(0.5.dp, c.border, PillShape).background(c.surface), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", style = MaterialTheme.typography.titleLarge, color = c.ink,
            modifier = Modifier.clickable { vm.changeMonth(-1) }.padding(horizontal = 12.dp, vertical = 6.dp))
        Box {
            Text(
                (if (long) vm.month.label() else vm.month.name()).uppercase() + " ▾",
                style = CapsLabel, color = c.ink,
                modifier = Modifier.clickable { open = true }.padding(horizontal = 4.dp, vertical = 10.dp),
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(c.surfaceRaised)) {
                (0L until 12L).map { now.minusMonths(it) }.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.label(), color = if (m == vm.month) c.accent else c.ink) },
                        onClick = { open = false; vm.pickMonth(m) },
                    )
                }
            }
        }
        val canNext = vm.month < now
        Text("›", style = MaterialTheme.typography.titleLarge, color = if (canNext) c.ink else c.track,
            modifier = Modifier.clickable(enabled = canNext) { vm.changeMonth(1) }.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
