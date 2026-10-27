@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "Which accounts are yours to track?": every account found in the messages, with a switch. Accounts
 * switched off (a friend's, a family member's) are left out of every total and list.
 */
@Composable
fun AccountsSheet(vm: FinTrackViewModel, onDismiss: () -> Unit) {
    val c = Fin.colors
    var skip by remember { mutableStateOf(vm.untracked) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Which accounts are yours to track?", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Faint("FinTrack found these in your messages. Switch off any that aren't your money, like a friend's or family member's account. Switched-off accounts are left out of everything.")
            vm.accounts.forEach { a ->
                val on = a.key !in skip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.label, style = MaterialTheme.typography.titleMedium, color = if (on) c.ink else c.inkFaint)
                        Faint(
                            listOfNotNull(
                                "${a.count} transaction${if (a.count == 1) "" else "s"}",
                                a.balance?.let { b -> "balance ${rupees(b)}" + (a.balanceDate?.let { " on ${it.short()}" } ?: "") },
                            ).joinToString("  ·  "),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        on, { skip = if (it) skip - a.key else skip + a.key },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            PopButton("Save", onClick = { vm.chooseAccounts(skip); onDismiss() }, Modifier.fillMaxWidth())
        }
    }
}

/** Asked once on Home when the messages mention two or more accounts. */
@Composable
fun AccountsPrompt(vm: FinTrackViewModel, onChoose: () -> Unit) {
    val white = Color.White
    HeroTile(Modifier.fillMaxWidth()) {
        Text("FinTrack found ${vm.accounts.size} accounts in your messages", style = MaterialTheme.typography.titleLarge, color = white)
        Spacer(Modifier.height(6.dp))
        Text(
            vm.accounts.take(4).joinToString("  ·  ") { it.label },
            style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose the ones that are your money, so your totals aren't mixed with someone else's.",
            style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(14.dp))
        PopButton("Choose accounts", onClick = onChoose, Modifier.fillMaxWidth())
    }
}
