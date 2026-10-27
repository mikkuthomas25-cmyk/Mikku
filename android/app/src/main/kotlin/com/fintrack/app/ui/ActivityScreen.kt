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
    var open by remember { mutableStateOf<Txn?>(null) }
    var adding by remember { mutableStateOf(false) }
    val rows = vm.filteredTxns
    val c = Fin.colors

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Transactions", style = MaterialTheme.typography.headlineMedium, color = c.ink, modifier = Modifier.weight(1f))
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
                    items(Kind.entries) { k -> PopChip(k.label, vm.kind == k) { vm.kind = k } }
                }
            }
            if (rows.isNotEmpty()) {
                item { Faint("Tap any transaction to say what it is. FinTrack remembers it for next time.", Modifier.padding(horizontal = 6.dp)) }
            } else {
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
                    // Money that actually left: spending, people, investments (less refunds).
                    val out = dayRows.sumOf { it.outValue }
                    Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
                        Text(date.relative(), style = MaterialTheme.typography.labelLarge, color = c.inkSoft, modifier = Modifier.weight(1f))
                        if (out > 0) Text("−" + rupees(out), style = MaterialTheme.typography.labelLarge.merge(MoneyStyle), color = c.inkFaint)
                    }
                }
                item(key = "d$date") {
                    Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                        dayRows.forEachIndexed { i, t ->
                            TxnRow(t) { open = t }
                            if (i < dayRows.lastIndex) HorizontalDivider(Modifier.padding(start = 66.dp, end = 12.dp), color = c.track)
                        }
                    }
                }
            }
        }

        AddButton(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 92.dp)) { adding = true }
    }

    open?.let { t -> TxnSheet(t, vm) { open = null } }
    if (adding) AddSheet(onDismiss = { adding = false }) { date, amount, dir, what, choice ->
        vm.addManual(date, amount, dir, what, choice)
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
private fun AddButton(modifier: Modifier, onClick: () -> Unit) {
    val c = Fin.colors
    Plunk(Color.White, c.buttonEdgeRight, c.buttonEdgeBottom, modifier, faceModifier = Modifier.size(56.dp), depth = 5.dp, onClick = onClick) {
        Icon(Icons.Filled.Add, "Add cash spend", tint = Color.Black, modifier = Modifier.size(28.dp).align(Alignment.Center))
    }
}

@Composable
private fun AddSheet(onDismiss: () -> Unit, onSave: (LocalDate, Double, Direction, String, Choice) -> Unit) {
    val c = Fin.colors
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var what by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    var choice by remember { mutableStateOf<Choice?>(null) }
    val value = amount.toDoubleOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add a transaction", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopChip("Money out", direction == Direction.DEBIT) { direction = Direction.DEBIT; choice = null }
                PopChip("Money in", direction == Direction.CREDIT) { direction = Direction.CREDIT; choice = null }
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
            PopTextField(what, { what = it }, "Name or note (optional)")
            Text("WHAT IS THIS?", style = CapsLabel, color = c.inkSoft)
            ChoiceChips(direction, selected = { it == choice }) { choice = it }
            PopChip("${date.relative()}  ▾", selected = false) {
                DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                    date.year, date.monthValue - 1, date.dayOfMonth).show()
            }
            PopButton(
                "Save", onClick = { onSave(date, value!!, direction, what.trim().ifBlank { choice!!.label }, choice!!) }, Modifier.fillMaxWidth(),
                enabled = value != null && value > 0 && choice != null,
            )
        }
    }
}
