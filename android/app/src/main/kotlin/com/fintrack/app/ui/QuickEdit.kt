@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.fintrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.core.Categorizer
import com.fintrack.core.CategorySource
import com.fintrack.core.Flow
import com.fintrack.core.PayeeMemory
import com.fintrack.core.Txn

/** Categories a spend can be put in (the rest are money types, set with "Count this as"). */
val SPEND_CATEGORIES = Categorizer.CATEGORIES.filter { it !in setOf("Income", "Self Transfer", "Transfers", "Investments") }

/**
 * A payment's category as a small label you can tap to change. ✦ marks one the app learned
 * itself; ▾ says it can be changed.
 */
@Composable
fun CategoryTag(t: Txn, onClick: () -> Unit) {
    val c = Fin.colors
    Row(
        Modifier.clip(TileShape).border(1.dp, c.border).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(categoryColor(t.category)))
        Spacer(Modifier.width(6.dp))
        Text(
            t.category + (if (CategorySource.isAutomatic(t.categorySource)) " ✦" else "") + "  ▾",
            style = MaterialTheme.typography.labelMedium, color = c.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** On a payment to a person: "what was it for?", so rent or fees paid to a person can be sorted. */
@Composable
fun PersonTag(onClick: () -> Unit) {
    val c = Fin.colors
    Text(
        "What for?  ▾", style = MaterialTheme.typography.labelMedium, color = c.inkSoft, maxLines = 1,
        modifier = Modifier.clip(TileShape).border(1.dp, c.border).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Pick a category in one tap. The choice sticks for this payee and teaches the model. */
@Composable
fun CategoryPickerSheet(t: Txn, onDismiss: () -> Unit, why: String? = null, onPerson: (() -> Unit)? = null, onPick: (String) -> Unit) {
    val c = Fin.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("What was ${t.merchant}?", style = MaterialTheme.typography.titleLarge, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Faint(
                "${rupees(t.amount)} · ${t.date.long()}. FinTrack will use this for ${t.merchant} from now on" +
                    (if (t.amount >= PayeeMemory.MIN_PATTERN_AMOUNT) ", and for ${rupees(t.amount)} paid around the ${ordinal(t.date.dayOfMonth)} in later months." else "."),
            )
            why?.let { Faint("✦ Now: $it") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPEND_CATEGORIES.forEach { cat -> PopChip(cat, cat == t.category && t.effectiveFlow == Flow.SPEND) { onPick(cat) } }
            }
            if (onPerson != null) {
                Text("NOT SPENDING?", style = CapsLabel, color = c.inkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PopChip("Just a person (gift, split, loan)", t.effectiveFlow == Flow.TO_PEOPLE) { onPerson() }
                }
            }
        }
    }
}

private fun ordinal(d: Int) = "$d" + when {
    d in 11..13 -> "th"
    d % 10 == 1 -> "st"
    d % 10 == 2 -> "nd"
    d % 10 == 3 -> "rd"
    else -> "th"
}
