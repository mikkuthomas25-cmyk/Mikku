package com.fintrack.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.fintrack.app.BuildConfig
import com.fintrack.app.sms.PermissionHelper

@Composable
fun ProfileScreen(vm: FinTrackViewModel, openInsights: () -> Unit) {
    val c = Fin.colors
    var showMissed by remember { mutableStateOf(false) }
    if (showMissed) MissedSheet(vm) { showMissed = false }
    var showImport by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    if (showAccounts) AccountsSheet(vm) { showAccounts = false }
    if (showImport) ImportSheet(vm) { showImport = false }
    vm.passwordFor?.let { PasswordDialog(it, vm) }
    val pickStatements = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            vm.importStatements(uris)
            showImport = true
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, color = c.ink) }
        item {
            Tile(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(vm.account ?: "", size = 56.dp, gradient = true)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(vm.account ?: "", style = MaterialTheme.typography.titleLarge, color = c.ink)
                        Faint(vm.accountEmail)
                    }
                }
            }
        }
        item {
            val context = LocalContext.current
            Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                SettingRow(
                    "SMS access",
                    if (vm.hasSmsPermission) "Allowed · new bank SMS are added automatically"
                    else "Blocked · tap to allow it in Settings",
                    danger = !vm.hasSmsPermission,
                ) { PermissionHelper.openSmsSettings(context) }
                if (PermissionHelper.isXiaomi) {
                    SettingRow(
                        "Autostart",
                        "Turn on so Xiaomi doesn't stop FinTrack catching SMS in the background",
                    ) { PermissionHelper.openAutostartSettings(context) }
                }
                SettingRow(
                    "Accounts to track",
                    if (vm.untracked.isEmpty()) "${vm.accounts.size} found in your messages · all tracked"
                    else "${vm.accounts.size - vm.accounts.count { it.key in vm.untracked }} of ${vm.accounts.size} tracked",
                ) { showAccounts = true }
                SettingRow(
                    "Charts and checks",
                    "Spending pace, daily spending, trends, unusual spends, bank balance check",
                ) { openInsights() }
                SettingRow(
                    "Import bank statements",
                    "Add PDFs or CSVs (several at once) to fill gaps like small UPI payments and check the balance",
                ) { pickStatements.launch(arrayOf("application/pdf", "text/csv", "text/comma-separated-values", "text/plain")) }
                SettingRow(
                    "Missed messages",
                    "Bank SMS FinTrack couldn't read. Review them or share them to improve it",
                    enabled = vm.hasSmsPermission,
                ) { vm.loadMissed(); showMissed = true }
                SettingRow(
                    "Check my numbers",
                    "Totals look wrong? Share ${vm.month.name()}'s list (where each payment came from and how it was counted). Account numbers and UPI IDs are hidden",
                ) { PermissionHelper.shareText(context, "FinTrack check", vm.checkReport()) }
                SettingRow(
                    "Rescan messages",
                    if (vm.hasSmsPermission) "Re-read the last 6 months of bank SMS" else "Allow SMS access on Home first",
                    enabled = vm.hasSmsPermission && !vm.busy,
                ) { vm.scan(full = true) }
            }
        }
        item {
            Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                SettingRow("Sign out", "You can sign back in any time", danger = true) { vm.signOut() }
            }
        }
        item {
            Faint("FinTrack ${BuildConfig.VERSION_NAME}  ·  your data stays on this device",
                Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    val c = Fin.colors
    Row(
        Modifier.fillMaxWidth().clip(TileShape).clickable(enabled = enabled, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = when {
                    danger -> c.loss
                    enabled -> c.ink
                    else -> c.inkFaint
                })
            Faint(subtitle)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = if (enabled) c.inkFaint else Color.Transparent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissedSheet(vm: FinTrackViewModel, onDismiss: () -> Unit) {
    val c = Fin.colors
    val context = LocalContext.current
    val missed = vm.missed
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            Modifier.padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Missed messages", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Faint(
                    when {
                        missed == null -> "Looking through the last 6 months…"
                        missed.isEmpty() -> "Nothing missed. Every bank-looking SMS was read."
                        else -> "${missed.size} bank-looking SMS couldn't be read. Share them so the reader can learn " +
                            "your banks' formats. Account, card, reference and phone numbers are hidden first."
                    },
                )
            }
            if (!missed.isNullOrEmpty()) {
                item {
                    PopButton("Share for fixing", onClick = {
                        val text = missed.take(60).joinToString("\n\n---\n\n") { m ->
                            "[${m.sender.take(12)}] " + com.fintrack.core.SmsParser.redact(m.body)
                        }
                        PermissionHelper.shareText(context, "FinTrack: messages it couldn't read", text)
                    }, modifier = Modifier.fillMaxWidth())
                }
                items(missed.take(100)) { m ->
                    Tile(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Text(
                            m.sender + "  ·  " + java.time.Instant.ofEpochMilli(m.millis)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().short(),
                            style = MaterialTheme.typography.labelMedium, color = c.inkSoft,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(m.body, style = MaterialTheme.typography.bodySmall, color = c.ink, maxLines = 6)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(vm: FinTrackViewModel, onDismiss: () -> Unit) {
    val c = Fin.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            Modifier.padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Bank statements", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Faint(
                    when {
                        vm.importing -> "Reading… everything stays on this phone."
                        vm.passwordFor != null -> "Waiting for a password."
                        else -> "Done. New transactions are in Activity, and categories have been re-learned."
                    },
                )
            }
            items(vm.importReports) { r -> StatementReportTile(r) }
        }
    }
}

/** One file's outcome: how many rows, what was new, and the mass-balance check. */
@Composable
private fun StatementReportTile(r: ImportReport) {
    val c = Fin.colors
    Tile(Modifier.fillMaxWidth(), padding = 16.dp) {
        Text(r.fileName, style = MaterialTheme.typography.titleSmall, color = c.ink, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        val result = r.result
        when {
            r.error != null -> Faint(r.error)
            result == null || result.empty -> Faint(
                "No transactions found. If this is a scanned (photo) PDF, FinTrack can't read it yet; " +
                    "download the statement again from net banking as a regular PDF or CSV.",
            )
            else -> {
                Faint("${result.rows.size} transactions  ·  ${r.added} new  ·  ${r.alreadyKnown} already in FinTrack")
                Spacer(Modifier.height(12.dp))
                val check = result.check
                Text(
                    if (check.balanced) "✓  BALANCED" else check.difference?.let { "⚠  OFF BY ${rupees(kotlin.math.abs(it))}" } ?: "⚠  CHECK ROWS",
                    style = CapsLabel, color = if (check.balanced) c.gain else c.loss,
                )
                Spacer(Modifier.height(8.dp))
                BalanceLine("Opening", check.opening)
                BalanceLine("+ Money in", check.totalIn)
                BalanceLine("− Money out", check.totalOut)
                BalanceLine("= Expected closing", check.expectedClosing)
                BalanceLine("Statement closing", check.closing)
                if (check.brokenRows.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Faint("These rows didn't match the running balance, so they may be misread:")
                    check.brokenRows.take(5).forEach { row ->
                        Faint("${row.date.short()}  ·  ${row.merchant}  ·  ${rupees(row.amount)}", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceLine(label: String, value: Double?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft, modifier = Modifier.weight(1f))
        Text(value?.let { rupees(it) } ?: "—", style = MaterialTheme.typography.bodyMedium.merge(MoneyStyle), color = Fin.colors.ink)
    }
}

@Composable
private fun PasswordDialog(ask: PasswordAsk, vm: FinTrackViewModel) {
    var pw by remember(ask) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = vm::skipPassword,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (ask.wrongPassword) "Wrong password, try again" else "Password needed") },
        text = {
            Column {
                Faint("${ask.fileName} is protected. Banks usually use your customer ID, date of birth (DDMMYYYY), " +
                    "or the first letters of your name plus date of birth. It's used once and never saved.")
                Spacer(Modifier.height(14.dp))
                PopTextField(pw, { pw = it }, "Statement password", password = true)
            }
        },
        confirmButton = { TextButton(onClick = { vm.submitPassword(pw) }, enabled = pw.isNotEmpty()) { Text("OPEN") } },
        dismissButton = { TextButton(onClick = vm::skipPassword) { Text("SKIP FILE") } },
    )
}
