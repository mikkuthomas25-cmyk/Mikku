package com.fintrack.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrack.app.sms.PermissionHelper
import com.fintrack.core.Direction

private val STATEMENT_TYPES = arrayOf("application/pdf", "text/csv", "text/comma-separated-values", "text/plain")

/**
 * First run, statement first:
 * 1. import last month's bank statement;
 * 2. sort its spending payee by payee (each answer covers every payment to that payee, and later ones);
 * 3. add another month if there's only one (recommended, not required);
 * 4. then turn on SMS reading, which uses what was taught.
 */
@Composable
fun SetupScreen(vm: FinTrackViewModel) {
    vm.passwordFor?.let { PasswordDialog(it, vm) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importStatements(uris)
    }
    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
            StepHeader(vm.setup)
            Spacer(Modifier.height(16.dp))
            when (vm.setup) {
                SetupStep.Statement -> StatementStep(vm) { pick.launch(STATEMENT_TYPES) }
                SetupStep.Sort -> SortStep(vm)
                SetupStep.More -> MoreStep(vm) { pick.launch(STATEMENT_TYPES) }
                SetupStep.Sms -> SmsStep(vm)
                SetupStep.Done -> Unit
            }
        }
    }
}

@Composable
private fun StepHeader(step: SetupStep) {
    val c = Fin.colors
    val n = when (step) {
        SetupStep.Statement -> 1
        SetupStep.Sort, SetupStep.More -> 2
        else -> 3
    }
    Text("SET UP · STEP $n OF 3", style = CapsLabel, color = c.inkSoft)
}

@Composable
private fun StatementStep(vm: FinTrackViewModel, upload: () -> Unit) {
    val c = Fin.colors
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Start with last month's bank statement", style = MaterialTheme.typography.headlineSmall, color = c.ink) }
        item {
            Faint(
                "Download last month's statement from your bank (e.g. YONO or net banking) as a PDF or CSV and add it here. " +
                    "Have 2–3 months? Select them all at once: more months teach FinTrack your regular payments. " +
                    "FinTrack reads it on this phone; nothing is uploaded anywhere. Next, you'll sort its spending once, " +
                    "and FinTrack sorts similar payments by itself from then on.",
            )
        }
        item {
            PopButton(if (vm.importing) "Reading…" else "Add bank statement", onClick = upload, Modifier.fillMaxWidth(), enabled = !vm.importing)
        }
        items(vm.importReports) { r -> StatementReportTile(r, vm) }
        item { SkipLink(vm) }
    }
}

@Composable
private fun SortStep(vm: FinTrackViewModel) {
    val c = Fin.colors
    val plan = vm.plan ?: return
    val share = vm.sortedShare
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Sort your spending", style = MaterialTheme.typography.headlineSmall, color = c.ink) }
        if (plan.auto.isNotEmpty()) {
            item {
                Faint(
                    "FinTrack already sorted ${plan.auto.size} payee${if (plan.auto.size == 1) "" else "s"} " +
                        "(${rupees(plan.auto.sumOf { it.total })}) from shop names and UPI IDs. You only need to help with the rest.",
                )
            }
        }
        item {
            Column {
                LinearProgressIndicator(progress = { share.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.track)
                Spacer(Modifier.height(6.dp))
                Faint("${"%.0f".format(share * 100)}% of your spending is sorted")
            }
        }
        if (share >= com.fintrack.core.Onboarding.ENOUGH) {
            item { PopButton("That's enough, finish", onClick = vm::finishSorting, Modifier.fillMaxWidth()) }
        }
        if (!vm.smallDecided) {
            item { SmallPaymentsCard(plan.small, vm) }
        } else {
            vm.toSort.getOrNull(vm.sortIndex)?.let { payee -> item { PayeeCard(payee, vm) } }
        }
        item {
            Text("Finish later  →", style = MaterialTheme.typography.labelLarge, color = c.inkSoft,
                modifier = Modifier.clickable { vm.finishSorting() }.padding(vertical = 10.dp))
        }
    }
}

/** All the small payees in one question, instead of one each. */
@Composable
private fun SmallPaymentsCard(small: List<com.fintrack.core.Onboarding.Payee>, vm: FinTrackViewModel) {
    val c = Fin.colors
    val count = small.sumOf { it.txns.size }
    Tile(Modifier.fillMaxWidth()) {
        Text("$count small payments", style = MaterialTheme.typography.titleLarge, color = c.ink)
        Text(
            "${rupees(small.sumOf { it.total })} to ${small.size} payees, each under ${rupees(com.fintrack.core.Onboarding.SMALL_PAYEE)}",
            style = MaterialTheme.typography.bodyMedium, color = c.inkSoft, modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Faint(small.take(6).joinToString(", ") { it.name } + if (small.size > 6) "…" else "")
        Spacer(Modifier.height(12.dp))
        Text("Tea, snacks, local shops?", style = MaterialTheme.typography.bodyLarge, color = c.ink)
        Spacer(Modifier.height(10.dp))
        PopButton("Yes, daily small spends", onClick = { vm.answerSmall(dailySpends = true) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PopOutlineButton("Let me sort them one by one", { vm.answerSmall(dailySpends = false) }, Modifier.fillMaxWidth())
    }
}

/**
 * One payee, with everything the statement says about them, and quick answers that fit: "who is
 * this?" for a person, shop categories for a shop, and a nudge when it's the same amount monthly.
 */
@Composable
private fun PayeeCard(payee: com.fintrack.core.Onboarding.Payee, vm: FinTrackViewModel) {
    val c = Fin.colors
    val kind = payee.kind
    val quick: List<Choice> = buildList {
        payee.suggestion?.let { add(Choice.Category(it)) }
        when (kind) {
            com.fintrack.core.Onboarding.Kind.PERSON -> {
                add(Choice.Category("Rent"))
                if (payee.monthly) add(Choice.Category("Education"))
                add(Choice.Kind(com.fintrack.core.Flow.TO_PEOPLE, "Friend / family"))
                add(Choice.Kind(com.fintrack.core.Flow.OWN_TRANSFER, "My own account"))
                add(Choice.Category("Food & Dining"))
            }
            else -> {
                if (payee.monthly) add(Choice.Category("Bills & Utilities"))
                add(Choice.Category("Food & Dining"))
                add(Choice.Category("Groceries"))
                add(Choice.Category("Shopping"))
                add(Choice.Category(com.fintrack.core.Categorizer.SMALL))
            }
        }
    }.distinctBy { it.label }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Faint("Payee ${vm.sortIndex + 1} of ${vm.toSort.size}")
        Tile(Modifier.fillMaxWidth()) {
            Text(payee.name, style = MaterialTheme.typography.titleLarge, color = c.ink, maxLines = 2)
            payee.upi?.let { Faint("UPI ID: $it", maxLines = 1) }
            Text(
                "${payee.txns.size} payment${if (payee.txns.size == 1) "" else "s"}  ·  ${rupees(payee.total)}",
                style = MaterialTheme.typography.titleMedium.merge(MoneyStyle), color = c.ink, modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(6.dp))
            payee.txns.take(4).forEach { t -> Faint("${t.date.short()}  ·  ${rupees(t.amount)}", maxLines = 1) }
            if (payee.txns.size > 4) Faint("+ ${payee.txns.size - 4} more", maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    payee.monthly -> "Same amount every month: rent, fees, an EMI or a bill?"
                    kind == com.fintrack.core.Onboarding.Kind.PERSON -> "A person (personal UPI ID). Who is this?"
                    kind == com.fintrack.core.Onboarding.Kind.SHOP -> "A shop (QR code payment). What kind of shop?"
                    else -> "What was this for?"
                },
                style = MaterialTheme.typography.bodyLarge, color = c.ink,
            )
        }
        quick.forEach { ch ->
            PopOutlineButton(
                if (ch is Choice.Category && ch.name == "Rent") "Rent (my landlord)" else ch.label,
                { vm.sortAnswer(ch) }, Modifier.fillMaxWidth(),
            )
        }
        WhatDropdown(Direction.DEBIT, current = "Something else") { vm.sortAnswer(it) }
        Text("Skip this one", style = MaterialTheme.typography.labelLarge, color = c.inkSoft,
            modifier = Modifier.clickable { vm.sortAnswer(null) }.padding(vertical = 10.dp))
    }
}

@Composable
private fun MoreStep(vm: FinTrackViewModel, upload: () -> Unit) {
    val c = Fin.colors
    val enough = vm.statementMonths >= com.fintrack.core.Onboarding.RECOMMENDED_MONTHS
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                if (enough) "Your spending is sorted" else "One more month helps",
                style = MaterialTheme.typography.headlineSmall, color = c.ink,
            )
        }
        item {
            Faint(
                if (enough) "FinTrack has ${vm.statementMonths} months of your statements to learn from."
                else "You've added ${vm.statementMonths} month. A second month's statement shows FinTrack your regular payments " +
                    "(rent, bills, salary, EMIs), so it can recognise them every month.",
            )
        }
        if (!enough) {
            item { PopButton(if (vm.importing) "Reading…" else "Add another month", onClick = upload, Modifier.fillMaxWidth(), enabled = !vm.importing) }
        }
        items(vm.importReports) { r -> StatementReportTile(r, vm) }
        item {
            if (enough) PopButton("Continue", onClick = vm::toSmsStep, Modifier.fillMaxWidth())
            else PopOutlineButton("Continue without it", vm::toSmsStep, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SmsStep(vm: FinTrackViewModel) {
    val c = Fin.colors
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Finishing setup reads the messages (the last 6 months the first time).
        vm.finishSetup()
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Now keep it up to date", style = MaterialTheme.typography.headlineSmall, color = c.ink)
        Faint(
            "FinTrack can read your bank SMS to add new payments as they happen, sorted the way you just taught it. " +
                "Messages stay on this phone. You'll choose which accounts to track next.",
        )
        PopButton(
            if (vm.hasSmsPermission) "Continue" else "Allow SMS reading",
            onClick = { if (vm.hasSmsPermission) vm.finishSetup() else launcher.launch(PermissionHelper.SMS_PERMISSIONS) },
            Modifier.fillMaxWidth(),
        )
        PopOutlineButton("Not now", vm::finishSetup, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SkipLink(vm: FinTrackViewModel) {
    Text(
        "Skip setup", style = MaterialTheme.typography.labelLarge, color = Fin.colors.inkSoft,
        modifier = Modifier.clickable(onClick = vm::finishSetup).padding(vertical = 12.dp),
    )
}
