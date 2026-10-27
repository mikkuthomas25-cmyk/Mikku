@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.fintrack.app.ui

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.core.Categorizer
import com.fintrack.core.CategorySource
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ActivityScreen(vm: FinTrackViewModel) {
    var editing by remember { mutableStateOf<Txn?>(null) }
    var picking by remember { mutableStateOf<Txn?>(null) }
    var adding by remember { mutableStateOf(false) }
    val rows = vm.filteredTxns
    val c = Fin.colors

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activity", style = MaterialTheme.typography.headlineMedium, color = c.ink, modifier = Modifier.weight(1f))
                    MonthSwitcher(vm.month.name(), vm.month < YearMonth.now(), { vm.changeMonth(-1) }, { vm.changeMonth(1) })
                }
            }
            item { SearchBar(vm.query) { vm.query = it } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.filter?.let { f ->
                        item {
                            PopChip(
                                when (f) {
                                    is TxnFilter.Day -> f.date.long()
                                    is TxnFilter.Category -> f.name
                                    is TxnFilter.Merchant -> f.name
                                    is TxnFilter.OfFlow -> f.flow.label
                                } + "  ✕",
                                selected = true,
                            ) { vm.filter = null }
                        }
                    }
                    items(Kind.entries) { k -> PopChip(k.name, vm.kind == k) { vm.kind = k } }
                }
            }
            item {
                val f = vm.filter
                if (f is TxnFilter.OfFlow) {
                    Tile(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.flow.label.uppercase(), style = CapsLabel, color = c.inkSoft, modifier = Modifier.weight(1f))
                            Text(rupees(rows.sumOf { it.amount }), style = MaterialTheme.typography.titleMedium.merge(MoneyStyle), color = c.ink)
                        }
                        Spacer(Modifier.height(4.dp))
                        Faint("Something here that shouldn't be? Untick it to leave it out of your totals.")
                    }
                } else if (rows.isNotEmpty()) {
                    Faint("Tap a category to change it (FinTrack remembers). ✓ = counted in your totals; untick anything that shouldn't count.",
                        Modifier.padding(horizontal = 6.dp))
                }
            }
            if (rows.isEmpty()) {
                item {
                    Tile(Modifier.fillMaxWidth()) {
                        Text("Nothing here", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Faint(
                            if (vm.hasSmsPermission) "No transactions match. Try another month or clear the filters."
                            else "Allow SMS access on Home to fill this in automatically.",
                        )
                    }
                }
            }
            rows.groupBy { it.date }.forEach { (date, dayRows) ->
                item(key = "h$date") {
                    // Only real spending: transfers, duplicates and excluded rows don't add to the day.
                    val spent = dayRows.sumOf { it.spendValue }
                    Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
                        Text(date.relative(), style = MaterialTheme.typography.labelLarge, color = c.inkSoft, modifier = Modifier.weight(1f))
                        if (spent > 0) Text("−" + rupees(spent), style = MaterialTheme.typography.labelLarge.merge(MoneyStyle), color = c.inkFaint)
                    }
                }
                item(key = "d$date") {
                    Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                        dayRows.forEachIndexed { i, t ->
                            TxnRow(t, onToggleCounted = { vm.toggleCounted(t) }, onCategory = { picking = t }) { editing = t }
                            if (i < dayRows.lastIndex) HorizontalDivider(Modifier.padding(start = 66.dp, end = 12.dp), color = c.track)
                        }
                    }
                }
            }
        }

        AddButton(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 92.dp)) { adding = true }
    }

    editing?.let { t -> EditSheet(t, onDismiss = { editing = null }, vm = vm) }
    picking?.let { t ->
        CategoryPickerSheet(
            t, onDismiss = { picking = null }, why = vm.categoryReason(t),
            onPerson = { vm.markPerson(t); picking = null }.takeIf { t.direction == com.fintrack.core.Direction.DEBIT },
        ) { vm.quickCategory(t, it); picking = null }
    }
    if (adding) AddSheet(onDismiss = { adding = false }) { date, amount, dir, what ->
        vm.addManual(date, amount, dir, what)
        adding = false
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit) {
    val c = Fin.colors
    Tile(Modifier.fillMaxWidth(), shape = TileShape, padding = 0.dp) {
        Row(Modifier.height(50.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = c.inkFaint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search merchant or category", color = c.inkFaint, style = MaterialTheme.typography.bodyLarge)
                androidx.compose.foundation.text.BasicTextField(
                    value = query, onValueChange = onChange, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Text("✕", color = c.inkFaint, modifier = Modifier.clip(CircleShape).clickable { onChange("") }.padding(6.dp))
            }
        }
    }
}

@Composable
internal fun TxnRow(t: Txn, onToggleCounted: (() -> Unit)? = null, onCategory: (() -> Unit)? = null, onClick: () -> Unit) {
    val c = Fin.colors
    Row(
        Modifier.fillMaxWidth().clip(TileShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(t.merchant)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val flow = t.effectiveFlow
            // Payments to people can be sorted too (₹3,200 to a person → Rent), which makes them spending.
            val hasCategory = flow == Flow.SPEND || flow == Flow.REFUND || flow == Flow.TO_PEOPLE
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasCategory && onCategory != null) {
                    if (flow == Flow.TO_PEOPLE) PersonTag(onCategory) else CategoryTag(t, onCategory)
                    Spacer(Modifier.width(8.dp))
                }
                Faint(
                    listOfNotNull(
                        t.category.takeIf { hasCategory && onCategory == null },
                        t.account?.let { "••$it" },
                        "cash".takeIf { t.source == "manual" },
                        "statement".takeIf { t.source == "statement" },
                    ).joinToString("  ·  "),
                    Modifier.weight(1f, fill = false),
                    maxLines = 1,
                )
            }
            // Anything that isn't plain spending or income says what it is.
            if (flow != Flow.SPEND && flow != Flow.INCOME) {
                Text(
                    (if (flow.counted) flow.label else "Not counted · ${flow.label}").uppercase(),
                    style = CapsLabel, fontSize = 9.sp, color = if (flow.counted) c.accent else c.inkFaint,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        val credit = t.direction == Direction.CREDIT
        Text(
            (if (credit) "+" else "−") + rupees(t.amount),
            style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
            color = when {
                !t.effectiveFlow.counted -> c.inkFaint
                credit -> c.gain
                else -> c.ink
            },
        )
        if (onToggleCounted != null) {
            Spacer(Modifier.width(12.dp))
            CountedBox(t.effectiveFlow.counted, onToggleCounted)
        }
    }
}

/** The one-tap fix: ticked = counted in your totals; tap to exclude (or include again). */
@Composable
private fun CountedBox(counted: Boolean, onToggle: () -> Unit) {
    val c = Fin.colors
    Box(
        Modifier
            .size(26.dp)
            .background(if (counted) Color.White else Color.Transparent)
            .border(1.5.dp, if (counted) Color.White else c.inkFaint)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (counted) Text("✓", color = Color.Black, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AddButton(modifier: Modifier, onClick: () -> Unit) {
    val c = Fin.colors
    Plunk(Color.White, c.buttonEdgeRight, c.buttonEdgeBottom, modifier, faceModifier = Modifier.size(56.dp), depth = 5.dp, onClick = onClick) {
        Icon(Icons.Filled.Add, "Add cash spend", tint = Color.Black, modifier = Modifier.size(28.dp).align(Alignment.Center))
    }
}

@Composable
internal fun EditSheet(txn: Txn, onDismiss: () -> Unit, vm: FinTrackViewModel) {
    val c = Fin.colors
    var category by remember { mutableStateOf(txn.category) }
    var always by remember { mutableStateOf(true) }
    var flow by remember { mutableStateOf(txn.effectiveFlow) }
    var moreTypes by remember { mutableStateOf(flow !in simpleFlows(txn.direction)) }
    var fixing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(txn.merchant) }
    var amountText by remember { mutableStateOf(if (txn.amount % 1.0 == 0.0) txn.amount.toLong().toString() else txn.amount.toString()) }
    val decision = vm.flowDecisions[txn.id]
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(txn.merchant, size = 52.dp, gradient = true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(txn.merchant, style = MaterialTheme.typography.titleLarge, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Faint(txn.date.long() + (txn.account?.let { "  ·  A/c ••$it" } ?: ""))
                }
                Text(
                    (if (txn.direction == Direction.CREDIT) "+" else "−") + rupees(txn.amount),
                    style = MaterialTheme.typography.titleLarge.merge(MoneyStyle),
                    color = if (txn.direction == Direction.CREDIT) c.gain else c.ink,
                )
            }

            // 1. The one decision that matters for the totals.
            Text("COUNT THIS AS", style = CapsLabel, color = c.inkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                simpleFlows(txn.direction).forEach { f -> PopChip(choiceLabel(f), f == flow) { flow = f; moreTypes = false } }
                PopChip("More…", moreTypes) { moreTypes = !moreTypes }
            }
            if (moreTypes) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    flowsFor(txn.direction).filter { it !in simpleFlows(txn.direction) }.forEach { f -> PopChip(choiceLabel(f), f == flow) { flow = f } }
                }
            }
            decision?.reason?.takeIf { it.isNotBlank() }?.let { Faint("FinTrack's reason: $it") }

            // 2. Category, only where it matters (spending and refunds).
            if (flow == Flow.SPEND || flow == Flow.REFUND) {
                Text("CATEGORY", style = CapsLabel, color = c.inkSoft)
                if (CategorySource.isAutomatic(txn.categorySource)) {
                    Faint("✦ Learned from your past choices. Pick the right one and it learns from that too.")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categorizer.CATEGORIES.filter { it !in setOf("Income", "Self Transfer", "Transfers", "Investments") }
                        .forEach { cat -> PopChip(cat, cat == category) { category = cat } }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Same for all of ${txn.merchant}", style = MaterialTheme.typography.bodyLarge, color = c.ink)
                    Faint("Future payments here are treated the same way")
                }
                Switch(always, { always = it }, colors = SwitchDefaults.colors(checkedTrackColor = c.accent))
            }

            // 3. Fix what the reader got wrong.
            if (fixing) {
                PopTextField(name, { name = it }, "Name")
                PopTextField(amountText, { v -> amountText = v.filter { it.isDigit() || it == '.' }.take(12) }, "Amount (₹)",
                    keyboardType = KeyboardType.Decimal)
            } else {
                Text("FIX AMOUNT OR NAME  →", style = CapsLabel, color = c.ink, modifier = Modifier.clickable { fixing = true })
            }

            PopButton("Save", onClick = {
                if (category != txn.category && (flow == Flow.SPEND || flow == Flow.REFUND)) vm.setCategory(txn, category, always)
                if (flow != txn.effectiveFlow) vm.setFlow(txn, flow, forPayee = always)
                val newAmount = amountText.toDoubleOrNull()
                if (fixing && newAmount != null && newAmount > 0 && (newAmount != txn.amount || name.trim() != txn.merchant)) {
                    vm.editDetails(txn, name, newAmount)
                }
                onDismiss()
            }, Modifier.fillMaxWidth())
            txn.raw?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.inkFaint,
                    modifier = Modifier.fillMaxWidth().border(1.dp, c.border).padding(14.dp))
            }
            TextButton(onClick = { vm.delete(txn); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete transaction", color = c.loss)
            }
        }
    }
}

@Composable
private fun AddSheet(onDismiss: () -> Unit, onSave: (LocalDate, Double, Direction, String) -> Unit) {
    val c = Fin.colors
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var what by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    val value = amount.toDoubleOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add cash", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopChip("Spent", direction == Direction.DEBIT) { direction = Direction.DEBIT }
                PopChip("Received", direction == Direction.CREDIT) { direction = Direction.CREDIT }
            }
            // Big amount entry, the focus of this sheet.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹", fontSize = 36.sp, color = c.inkFaint)
                Spacer(Modifier.width(6.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' }.take(10) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 40.sp, color = c.ink).merge(MoneyStyle),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (amount.isEmpty()) Text("0", fontSize = 40.sp, color = c.track)
                        inner()
                    },
                )
            }
            PopTextField(what, { what = it }, "What for")
            PopChip("${date.relative()}  ▾", selected = false) {
                DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                    date.year, date.monthValue - 1, date.dayOfMonth).show()
            }
            PopButton(
                "Save", onClick = { onSave(date, value!!, direction, what) }, Modifier.fillMaxWidth(),
                enabled = value != null && value > 0 && what.isNotBlank(),
            )
        }
    }
}

private fun choiceLabel(f: Flow) = if (f == Flow.IGNORED) "Don't count" else f.label

/** The everyday choices: Spending (or Income) and Don't count. */
private fun simpleFlows(direction: Direction): List<Flow> =
    listOf(if (direction == Direction.DEBIT) Flow.SPEND else Flow.INCOME, Flow.IGNORED)

/** The types that make sense for money going out or coming in. */
private fun flowsFor(direction: Direction): List<Flow> = if (direction == Direction.DEBIT) {
    listOf(Flow.SPEND, Flow.IGNORED, Flow.INVESTED, Flow.TO_PEOPLE, Flow.OWN_TRANSFER, Flow.DUPLICATE)
} else {
    listOf(Flow.INCOME, Flow.IGNORED, Flow.REFUND, Flow.FROM_PEOPLE, Flow.OWN_TRANSFER, Flow.DUPLICATE)
}
