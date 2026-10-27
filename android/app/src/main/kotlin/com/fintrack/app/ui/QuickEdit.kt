@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.core.Categorizer
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.Txn
import android.app.DatePickerDialog
import java.time.LocalDate

/** Categories a spend can be put in. */
val SPEND_CATEGORIES = Categorizer.CATEGORIES.filter { it !in setOf("Income", "Self Transfer", "Transfers", "Investments") }

/**
 * One answer to "What is this?": a spending category (Food, Rent…) or a kind of money that isn't
 * spending (a person, your own account, an investment, don't count). One list instead of two
 * separate questions.
 */
sealed interface Choice {
    val label: String

    data class Category(val name: String) : Choice {
        override val label get() = name
    }

    data class Kind(val flow: Flow, override val label: String) : Choice
}

fun choicesFor(direction: Direction): List<Choice> = if (direction == Direction.DEBIT) {
    SPEND_CATEGORIES.map { Choice.Category(it) } + listOf(
        Choice.Kind(Flow.TO_PEOPLE, "Gave to a person"),
        Choice.Kind(Flow.INVESTED, "Investment / savings"),
        Choice.Kind(Flow.OWN_TRANSFER, "My own account"),
        Choice.Kind(Flow.DUPLICATE, "Duplicate"),
        Choice.Kind(Flow.IGNORED, "Don't count"),
    )
} else {
    listOf(
        Choice.Kind(Flow.INCOME, "Salary / income"),
        Choice.Kind(Flow.REFUND, "Refund / cashback"),
        Choice.Kind(Flow.FROM_PEOPLE, "From a person"),
        Choice.Kind(Flow.OWN_TRANSFER, "My own account"),
        Choice.Kind(Flow.DUPLICATE, "Duplicate"),
        Choice.Kind(Flow.IGNORED, "Don't count"),
    )
}

/** What a transaction is now, in the same words as the choices. */
fun labelOf(t: Txn): String = when (val f = t.effectiveFlow) {
    Flow.SPEND -> t.category
    else -> choicesFor(t.direction).filterIsInstance<Choice.Kind>().firstOrNull { it.flow == f }?.label ?: f.label
}

private fun isCurrent(t: Txn, choice: Choice) = when (choice) {
    is Choice.Category -> t.effectiveFlow == Flow.SPEND && t.category == choice.name
    is Choice.Kind -> t.effectiveFlow == choice.flow
}

/** Which part of the transaction panel is open. */
enum class SheetMode { Category, Edit, Delete }

/** A transaction the user opened, and on which part. */
data class Opened(val txn: Txn, val mode: SheetMode)

/**
 * A transaction in a list: name, its category (tap to change), amount, and ⋮ for edit/delete.
 * Everything that can be done to it is reachable from what's on screen.
 */
@Composable
internal fun TxnRow(t: Txn, onOpen: (Opened) -> Unit) {
    val c = Fin.colors
    val counted = t.effectiveFlow.counted
    Row(
        Modifier.fillMaxWidth().clip(TileShape).clickable { onOpen(Opened(t, SheetMode.Category)) }
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(t.merchant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.bodyLarge, color = if (counted) c.ink else c.inkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                WhatTag(t) { onOpen(Opened(t, SheetMode.Category)) }
                if (t.source == "manual") {
                    Spacer(Modifier.width(8.dp))
                    Faint("cash", maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        val credit = t.direction == Direction.CREDIT
        Text(
            (if (credit) "+" else "−") + rupees(t.amount),
            style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
            color = when {
                !counted -> c.inkFaint
                credit -> c.gain
                else -> c.ink
            },
            textDecoration = if (counted) null else TextDecoration.LineThrough,
        )
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable { onOpen(Opened(t, SheetMode.Edit)) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.MoreVert, "Edit or delete", tint = c.inkSoft, modifier = Modifier.size(20.dp)) }
    }
}

/** The category under a transaction's name, as a button: ▾ to change it. */
@Composable
private fun WhatTag(t: Txn, onClick: () -> Unit) {
    val c = Fin.colors
    val spend = t.effectiveFlow == Flow.SPEND
    Row(
        Modifier.clip(TileShape).border(1.dp, c.border).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (spend) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(categoryColor(t.category)))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            labelOf(t) + (if (t.effectiveFlow.counted) "" else " · not counted") + "  ▾",
            style = MaterialTheme.typography.labelMedium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The transaction panel: three clear buttons, Category · Edit · Delete, and the part that's open.
 * Category is one tap and remembered for the payee; Edit changes amount, name, date, in/out;
 * Delete asks once and the transaction never comes back from a rescan.
 */
@Composable
fun TxnSheet(opened: Opened, vm: FinTrackViewModel, onDismiss: () -> Unit) {
    val t = opened.txn
    val c = Fin.colors
    var mode by remember { mutableStateOf(opened.mode) }
    var name by remember { mutableStateOf(t.merchant) }
    var amountText by remember { mutableStateOf(if (t.amount % 1.0 == 0.0) t.amount.toLong().toString() else t.amount.toString()) }
    var date by remember { mutableStateOf(t.date) }
    var direction by remember { mutableStateOf(t.direction) }
    val context = LocalContext.current
    val why = vm.categoryReason(t) ?: vm.flowDecisions[t.id]?.reason?.takeIf { it.isNotBlank() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.merchant, style = MaterialTheme.typography.titleLarge, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Faint("${t.date.long()}  ·  now: ${labelOf(t)}")
                }
                Text(
                    (if (t.direction == Direction.CREDIT) "+" else "−") + rupees(t.amount),
                    style = MaterialTheme.typography.titleLarge.merge(MoneyStyle),
                    color = if (t.direction == Direction.CREDIT) c.gain else c.ink,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeTab("Category", mode == SheetMode.Category, Modifier.weight(1f)) { mode = SheetMode.Category }
                ModeTab("Edit", mode == SheetMode.Edit, Modifier.weight(1f)) { mode = SheetMode.Edit }
                ModeTab("Delete", mode == SheetMode.Delete, Modifier.weight(1f), danger = true) { mode = SheetMode.Delete }
            }

            when (mode) {
                SheetMode.Category -> {
                    WhatDropdown(t.direction, current = labelOf(t)) { vm.classify(t, it); onDismiss() }
                    Faint(if (t.merchant == "Unknown") "Applies to this payment." else "Remembered for ${t.merchant} next time.")
                }
                SheetMode.Edit -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PopChip("Money out", direction == Direction.DEBIT) { direction = Direction.DEBIT }
                        PopChip("Money in", direction == Direction.CREDIT) { direction = Direction.CREDIT }
                    }
                    PopTextField(name, { name = it }, "Name")
                    PopTextField(amountText, { v -> amountText = v.filter { it.isDigit() || it == '.' }.take(12) }, "Amount (₹)",
                        keyboardType = KeyboardType.Decimal)
                    PopChip("Date: ${date.long()}  ▾", selected = false) {
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year, date.monthValue - 1, date.dayOfMonth).show()
                    }
                    val amount = amountText.toDoubleOrNull()
                    PopButton("Save changes", onClick = {
                        vm.editDetails(t, name, amount!!, date, direction)
                        onDismiss()
                    }, Modifier.fillMaxWidth(), enabled = amount != null && amount > 0)
                    why?.let { Faint("How FinTrack chose \"${labelOf(t)}\": $it") }
                    t.raw?.let {
                        Text("ORIGINAL MESSAGE", style = CapsLabel, color = c.inkSoft)
                        Text(it, style = MaterialTheme.typography.bodySmall, color = c.inkFaint,
                            modifier = Modifier.fillMaxWidth().border(1.dp, c.border).padding(14.dp))
                    }
                }
                SheetMode.Delete -> {
                    Text("Delete this transaction?", style = MaterialTheme.typography.titleMedium, color = c.ink)
                    Faint(
                        "It disappears from your totals and lists" +
                            (if (t.source == "manual") "." else ", and won't come back when FinTrack reads your messages or statements again.") +
                            " To keep it but leave it out of totals, choose Category → Don't count instead.",
                    )
                    TextButton(
                        onClick = { vm.delete(t); onDismiss() },
                        modifier = Modifier.fillMaxWidth().border(1.dp, c.loss),
                    ) { Text("Delete ${rupees(t.amount)} · ${t.merchant}", color = c.loss, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, modifier: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val c = Fin.colors
    val fg = when {
        selected -> androidx.compose.ui.graphics.Color.Black
        danger -> c.loss
        else -> c.ink
    }
    Box(
        modifier.clip(TileShape)
            .background(if (selected) (if (danger) c.loss else androidx.compose.ui.graphics.Color.White) else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (danger) c.loss else c.border)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = fg) }
}

/**
 * "What was this for?" as a dropdown: the spending categories first, then the kinds of money that
 * aren't spending (a person, your own account, investment, duplicate, don't count).
 */
@Composable
fun WhatDropdown(direction: Direction, current: String?, onPick: (Choice) -> Unit) {
    PopDropdown(
        value = current ?: "Choose",
        options = choicesFor(direction),
        text = { it.label },
        onSelect = onPick,
        label = if (direction == Direction.DEBIT) "What was this for?" else "What is this money?",
    )
}
