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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.core.Categorizer
import com.fintrack.core.CategorySource
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.PayeeMemory
import com.fintrack.core.Txn

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

/** A transaction in a list: name, what it is, amount. Tap to change what it is. */
@Composable
internal fun TxnRow(t: Txn, onClick: () -> Unit) {
    val c = Fin.colors
    val counted = t.effectiveFlow.counted
    Row(
        Modifier.fillMaxWidth().clip(TileShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(t.merchant)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.bodyLarge, color = if (counted) c.ink else c.inkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                WhatTag(t)
                val extra = listOfNotNull("cash".takeIf { t.source == "manual" }, "statement".takeIf { t.source == "statement" })
                if (extra.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Faint(extra.joinToString(" · "), maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
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
    }
}

/** The small label under a transaction's name: its category or kind; ✦ when FinTrack chose it. */
@Composable
private fun WhatTag(t: Txn) {
    val c = Fin.colors
    val spend = t.effectiveFlow == Flow.SPEND
    Row(
        Modifier.border(1.dp, c.border).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (spend) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(categoryColor(t.category)))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            labelOf(t) + (if (spend && CategorySource.isAutomatic(t.categorySource)) " ✦" else "") +
                (if (t.effectiveFlow.counted) "" else " · not counted"),
            style = MaterialTheme.typography.labelMedium, color = c.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The one place to change a transaction: "What is this?" (one tap, remembered for the payee),
 * plus fixing the amount or name, and delete.
 */
@Composable
fun TxnSheet(t: Txn, vm: FinTrackViewModel, onDismiss: () -> Unit) {
    val c = Fin.colors
    var fixing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(t.merchant) }
    var amountText by remember { mutableStateOf(if (t.amount % 1.0 == 0.0) t.amount.toLong().toString() else t.amount.toString()) }
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
                    Faint(t.date.long() + (t.account?.let { "  ·  A/c ••$it" } ?: "") + "  ·  " +
                        if (t.source == "manual") "added by you" else if (t.source == "statement") "statement" else "SMS")
                }
                Text(
                    (if (t.direction == Direction.CREDIT) "+" else "−") + rupees(t.amount),
                    style = MaterialTheme.typography.titleLarge.merge(MoneyStyle),
                    color = if (t.direction == Direction.CREDIT) c.gain else c.ink,
                )
            }

            Text("WHAT IS THIS?", style = CapsLabel, color = c.inkSoft)
            ChoiceChips(t.direction, selected = { isCurrent(t, it) }) { vm.classify(t, it); onDismiss() }
            Faint(
                if (t.merchant == "Unknown") "Applies to this payment."
                else "FinTrack remembers this for ${t.merchant}" +
                    if (t.direction == Direction.DEBIT && t.amount >= PayeeMemory.MIN_PATTERN_AMOUNT) {
                        " and for ${rupees(t.amount)} paid around the ${ordinal(t.date.dayOfMonth)} in later months."
                    } else ".",
            )
            why?.let { Faint("Why it's ${labelOf(t)} now: $it") }

            if (fixing) {
                PopTextField(name, { name = it }, "Name")
                PopTextField(amountText, { v -> amountText = v.filter { it.isDigit() || it == '.' }.take(12) }, "Amount (₹)",
                    keyboardType = KeyboardType.Decimal)
                PopButton("Save", onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) vm.editDetails(t, name, amount)
                    onDismiss()
                }, Modifier.fillMaxWidth())
            } else {
                Text("FIX AMOUNT OR NAME  →", style = CapsLabel, color = c.ink, modifier = Modifier.clickable { fixing = true })
            }
            t.raw?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.inkFaint,
                    modifier = Modifier.fillMaxWidth().border(1.dp, c.border).padding(14.dp))
            }
            TextButton(onClick = { vm.delete(t); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete transaction", color = c.loss)
            }
        }
    }
}

/** The "What is this?" options for money going out or coming in. */
@Composable
fun ChoiceChips(direction: Direction, selected: (Choice) -> Boolean, onPick: (Choice) -> Unit) {
    val c = Fin.colors
    val all = choicesFor(direction)
    val categories = all.filterIsInstance<Choice.Category>()
    val kinds = all.filterIsInstance<Choice.Kind>()
    if (categories.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { ch -> PopChip(ch.label, selected(ch)) { onPick(ch) } }
        }
        Text("NOT SPENDING", style = CapsLabel, color = c.inkSoft)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        kinds.forEach { ch -> PopChip(ch.label, selected(ch)) { onPick(ch) } }
    }
}

private fun ordinal(d: Int) = "$d" + when {
    d in 11..13 -> "th"
    d % 10 == 1 -> "st"
    d % 10 == 2 -> "nd"
    d % 10 == 3 -> "rd"
    else -> "th"
}
