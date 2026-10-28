@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrack.core.Direction
import com.fintrack.core.MissingMoney
import kotlin.math.abs

/**
 * "₹500 came into SBI between 25 and 27 Oct that FinTrack didn't see": a balance moved by more than
 * the payments seen, e.g. a Google Pay alert swiped away. Add it (what was it?) or ignore it.
 */
@Composable
fun MissingMoneyCard(vm: FinTrackViewModel) {
    val c = Fin.colors
    var recording by remember { mutableStateOf<MissingMoney.Gap?>(null) }
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Not seen by FinTrack")
        Spacer(Modifier.height(4.dp))
        Faint("Your balance moved by more than the payments FinTrack saw. Usually a payment with no SMS, or a notification that was swiped away.")
        vm.gaps.take(3).forEach { g ->
            val name = vm.accounts.firstOrNull { it.key == g.accountKey }?.label ?: g.accountKey.substringAfter(':')
            Spacer(Modifier.height(14.dp))
            Text(
                "${rupees(abs(g.amount))} ${if (g.incoming) "came into" else "went out of"} $name" +
                    if (g.from == g.to) " on ${g.to.short()}" else " between ${g.from.short()} and ${g.to.short()}",
                style = MaterialTheme.typography.bodyLarge.merge(MoneyStyle), color = if (g.incoming) c.gain else c.ink,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PopChip("Add it", selected = true) { recording = g }
                PopChip("Ignore", selected = false) { vm.ignoreGap(g) }
            }
        }
        if (vm.gaps.size > 3) Faint("+ ${vm.gaps.size - 3} more", Modifier.padding(top = 10.dp))
    }
    recording?.let { g -> RecordGapSheet(g, vm) { recording = null } }
}

@Composable
private fun RecordGapSheet(gap: MissingMoney.Gap, vm: FinTrackViewModel, onDismiss: () -> Unit) {
    val c = Fin.colors
    var name by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf<Choice?>(null) }
    val direction = if (gap.incoming) Direction.CREDIT else Direction.DEBIT
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "${if (gap.incoming) "+" else "−"}${rupees(abs(gap.amount))}",
                style = BalanceStyle, fontSize = androidx.compose.ui.unit.TextUnit(34f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = if (gap.incoming) c.gain else c.ink,
            )
            Faint("Between ${gap.from.long()} and ${gap.to.long()}. Who was it ${if (gap.incoming) "from" else "to"}, and what was it?")
            PopTextField(name, { name = it }, if (gap.incoming) "From (e.g. Rahul)" else "To (e.g. a shop)")
            WhatDropdown(direction, current = choice?.label) { choice = it }
            PopButton("Record it", onClick = { vm.recordGap(gap, name, choice!!); onDismiss() }, Modifier.fillMaxWidth(), enabled = choice != null)
        }
    }
}
