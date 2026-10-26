@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.app.ui

import android.Manifest
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.app.BuildConfig
import com.fintrack.core.Categorizer
import com.fintrack.core.Direction
import com.fintrack.core.MonthSummary
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

private enum class Tab(val label: String) { Dashboard("Home"), Transactions("Transactions"), Insights("AI insights"), Settings("Settings") }

@Composable
fun MainScreen(vm: FinTrackViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == Tab.Settings) "Settings" else vm.month.label()) },
                actions = {
                    if (tab != Tab.Settings) {
                        IconButton(onClick = { vm.changeMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                        }
                        IconButton(onClick = { vm.changeMonth(1) }, enabled = vm.month < YearMonth.now()) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.label) },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.Dashboard -> Icons.Filled.Home
                                    Tab.Transactions -> Icons.AutoMirrored.Filled.List
                                    Tab.Insights -> Icons.Filled.Star
                                    Tab.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.Transactions) {
                var adding by remember { mutableStateOf(false) }
                FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, "Add cash spend") }
                if (adding) AddCashDialog(onDismiss = { adding = false }) { date, amount, dir, what ->
                    vm.addManual(date, amount, dir, what)
                    adding = false
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (tab) {
                Tab.Dashboard -> Dashboard(vm, openTransactions = { tab = Tab.Transactions })
                Tab.Transactions -> Transactions(vm)
                Tab.Insights -> Insights(vm)
                Tab.Settings -> SettingsScreen(vm)
            }
        }
    }
}

// ---------------------------------------------------------------------- dashboard

@Composable
private fun Dashboard(vm: FinTrackViewModel, openTransactions: () -> Unit) {
    val s = vm.summary
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!vm.hasSmsPermission) item { PermissionCard(vm) }
        if (s == null) return@LazyColumn
        item { Kpis(s) }
        item {
            Section("Daily spending") {
                val sel = (vm.filter as? TxnFilter.Day)?.date
                DailyBars(s.byDay, sel, onSelect = { vm.filter = it?.let(TxnFilter::Day) })
                if (sel != null) {
                    val amount = s.byDay.firstOrNull { it.first == sel }?.second ?: 0.0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${sel.long()} · ${rupees(amount)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = openTransactions) { Text("See transactions") }
                    }
                } else {
                    Muted("Tap a day to see what you spent on it.")
                }
            }
        }
        item {
            Section("Where it went") {
                BarList(s.byCategory, (vm.filter as? TxnFilter.Category)?.name, showCount = false) {
                    vm.filter = TxnFilter.Category(it)
                    openTransactions()
                }
            }
        }
        item {
            Section("Top merchants") {
                BarList(s.topMerchants, (vm.filter as? TxnFilter.Merchant)?.name, showCount = true) {
                    vm.filter = TxnFilter.Merchant(it)
                    openTransactions()
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(vm: FinTrackViewModel) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionResult()
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Let FinTrack read your bank SMS", fontWeight = FontWeight.SemiBold)
            Text(
                "FinTrack reads transaction messages (debited, credited, UPI, card) to build your dashboard. " +
                    "Messages are processed on this phone and are never uploaded. OTPs and personal chats are ignored.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = {
                launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            }) { Text("Allow SMS access") }
        }
    }
}

@Composable
private fun Kpis(s: MonthSummary) {
    val change = if (s.prevMonthSpent > 0) (s.spent - s.prevMonthSpent) / s.prevMonthSpent else null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Spent", rupees(s.spent), Modifier.weight(1f),
                sub = change?.let { "${if (it >= 0) "▲" else "▼"} ${"%.0f".format(abs(it * 100))}% vs last month" }
                    ?: "${s.txnCount} transactions")
            Kpi("Received", rupees(s.received), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Today", rupees(s.todaySpent), Modifier.weight(1f), sub = "spent today")
            Kpi("Net", rupees(s.net), Modifier.weight(1f),
                sub = if (s.net >= 0) "saved this month" else "spent more than received",
                positive = s.net >= 0)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Daily average", rupees(s.avgPerDay), Modifier.weight(1f))
            Kpi("Small payments", rupees(s.smallPaymentsTotal), Modifier.weight(1f),
                sub = "${s.smallPaymentsCount} payments of ₹200 or less")
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier, sub: String? = null, positive: Boolean? = null) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = when (positive) {
                    true -> Gain
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
            if (sub != null) Muted(sub, maxLines = 2)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun Muted(text: String, maxLines: Int = Int.MAX_VALUE) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

// ---------------------------------------------------------------------- transactions

@Composable
private fun Transactions(vm: FinTrackViewModel) {
    var editing by remember { mutableStateOf<Txn?>(null) }
    val rows = vm.filteredTxns
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        vm.filter?.let { f ->
            item {
                InputChip(
                    selected = true,
                    onClick = { vm.filter = null },
                    label = {
                        Text(when (f) {
                            is TxnFilter.Day -> f.date.long()
                            is TxnFilter.Category -> f.name
                            is TxnFilter.Merchant -> f.name
                        } + "  ✕")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (rows.isEmpty()) {
            item {
                Muted(
                    if (vm.hasSmsPermission) "No transactions here yet." else "Allow SMS access on the Home tab to get started.",
                )
            }
        }
        items(rows, key = { it.id }) { t ->
            TxnRow(t) { editing = t }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
    editing?.let { t ->
        EditTxnDialog(
            txn = t,
            onDismiss = { editing = null },
            onSave = { cat, all -> vm.setCategory(t, cat, all); editing = null },
            onDelete = { vm.delete(t); editing = null },
        )
    }
}

@Composable
private fun TxnRow(t: Txn, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(t.merchant, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Muted(listOfNotNull(t.date.short(), t.category, t.account?.let { "••$it" }, t.source.takeIf { it == "manual" })
                .joinToString(" · "), maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        val credit = t.direction == Direction.CREDIT
        Text(
            (if (credit) "+" else "−") + rupees(t.amount),
            color = if (credit) Gain else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EditTxnDialog(txn: Txn, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit, onDelete: () -> Unit) {
    var category by remember { mutableStateOf(txn.category) }
    var applyToAll by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${txn.merchant} · ${rupees(txn.amount)}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                txn.raw?.let { Muted(it); Spacer(Modifier.height(8.dp)) }
                Categorizer.CATEGORIES.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = c == category, onClick = { category = c }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = c == category, onClick = { category = c })
                        Text(c)
                    }
                }
                Row(Modifier.clickable { applyToAll = !applyToAll }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    Text("Always use this for ${txn.merchant}")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(category, applyToAll) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun AddCashDialog(onDismiss: () -> Unit, onSave: (LocalDate, Double, Direction, String) -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var what by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    val value = amount.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add cash / manual entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == Direction.DEBIT, { direction = Direction.DEBIT }, { Text("Spent") })
                    FilterChip(direction == Direction.CREDIT, { direction = Direction.CREDIT }, { Text("Received") })
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(what, { what = it }, label = { Text("What for") }, singleLine = true)
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                        date.year, date.monthValue - 1, date.dayOfMonth).show()
                }) { Text("Date: ${date.long()}") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(date, value!!, direction, what) },
                enabled = value != null && value > 0 && what.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------- AI insights

@Composable
private fun Insights(vm: FinTrackViewModel) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Where did my money go?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Muted(
            "Claude reads this month's totals (categories and top merchants, never your messages) " +
                "and suggests where you could save.",
        )
        Button(onClick = vm::generateInsights, enabled = !vm.busy && (vm.summary?.txnCount ?: 0) > 0) {
            Text("Analyse ${vm.month.label()}")
        }
        if (!vm.aiEnabled) Muted("Add your Claude API key in Settings to turn this on.")
        vm.insights?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------- settings

@Composable
private fun SettingsScreen(vm: FinTrackViewModel) {
    var key by remember { mutableStateOf(vm.prefs.claudeApiKey) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("Account") {
            Text(vm.account ?: "")
            OutlinedButton(onClick = vm::signOut) { Text("Sign out") }
        }
        Section("SMS") {
            Muted("New transaction SMS are captured automatically. Rescan if something looks missing.")
            OutlinedButton(onClick = { vm.scan(full = true) }, enabled = vm.hasSmsPermission && !vm.busy) {
                Text("Rescan last 6 months")
            }
        }
        Section("AI (optional)") {
            Muted(
                "Paste a Claude API key from console.anthropic.com to auto-categorize unknown merchants and get " +
                    "monthly insights. Only merchant names and monthly totals are sent; never your SMS. " +
                    "The key is stored only on this phone.",
            )
            OutlinedTextField(
                key, { key = it }, label = { Text("Claude API key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { vm.saveApiKey(key) }) { Text("Save key") }
        }
        Muted("FinTrack ${BuildConfig.VERSION_NAME}")
    }
}
