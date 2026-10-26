@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.fintrack.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ActivityScreen(vm: FinTrackViewModel) {
    var editing by remember { mutableStateOf<Txn?>(null) }
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
                                } + "  ✕",
                                selected = true,
                            ) { vm.filter = null }
                        }
                    }
                    items(Kind.entries) { k -> PopChip(k.name, vm.kind == k) { vm.kind = k } }
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
                    val spent = dayRows.filter { it.direction == Direction.DEBIT }.sumOf { it.amount }
                    Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
                        Text(date.relative(), style = MaterialTheme.typography.labelLarge, color = c.inkSoft, modifier = Modifier.weight(1f))
                        if (spent > 0) Text("−" + rupees(spent), style = MaterialTheme.typography.labelLarge.merge(MoneyStyle), color = c.inkFaint)
                    }
                }
                item(key = "d$date") {
                    Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                        dayRows.forEachIndexed { i, t ->
                            TxnRow(t) { editing = t }
                            if (i < dayRows.lastIndex) HorizontalDivider(Modifier.padding(start = 66.dp, end = 12.dp), color = c.track)
                        }
                    }
                }
            }
        }

        AddButton(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 92.dp)) { adding = true }
    }

    editing?.let { t -> EditSheet(t, onDismiss = { editing = null }, vm = vm) }
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
internal fun TxnRow(t: Txn, onClick: () -> Unit) {
    val c = Fin.colors
    Row(
        Modifier.fillMaxWidth().clip(TileShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(t.merchant)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Faint(
                listOfNotNull(
                    t.category + if (t.categorySource == CategorySource.LEARNED) " ✦" else "",
                    t.account?.let { "••$it" },
                    "cash".takeIf { t.source == "manual" },
                ).joinToString("  ·  "),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(10.dp))
        val credit = t.direction == Direction.CREDIT
        Text(
            (if (credit) "+" else "−") + rupees(t.amount),
            style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
            color = if (credit) c.gain else c.ink,
        )
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
            txn.raw?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.inkSoft,
                    modifier = Modifier.fillMaxWidth().clip(TileShape).background(c.track.copy(alpha = 0.5f)).padding(14.dp))
            }
            Text("Category", style = MaterialTheme.typography.titleSmall, color = c.ink)
            if (txn.categorySource == CategorySource.LEARNED) {
                Faint("✦ FinTrack learned this from your past categories. Pick the right one and it learns from that too.")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Categorizer.CATEGORIES.forEach { cat -> PopChip(cat, cat == category) { category = cat } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Always use for ${txn.merchant}", style = MaterialTheme.typography.bodyLarge, color = c.ink)
                    Faint("Future payments here get this category too")
                }
                Switch(always, { always = it }, colors = SwitchDefaults.colors(checkedTrackColor = c.accent))
            }
            PopButton("Save", onClick = { vm.setCategory(txn, category, always); onDismiss() }, Modifier.fillMaxWidth())
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
