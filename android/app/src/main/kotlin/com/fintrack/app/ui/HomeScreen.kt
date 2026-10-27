package com.fintrack.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fintrack.app.sms.PermissionHelper
import androidx.compose.ui.unit.dp
import com.fintrack.core.Flow
import com.fintrack.core.MonthSummary
import com.fintrack.core.ReviewItem
import com.fintrack.core.Txn
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun HomeScreen(vm: FinTrackViewModel, openActivity: () -> Unit, openProfile: () -> Unit, openCategory: (String) -> Unit) {
    val s = vm.summary
    var editing by remember { mutableStateOf<Txn?>(null) }
    var picking by remember { mutableStateOf<Txn?>(null) }
    val show: (Kind, TxnFilter?) -> Unit = { k, f -> vm.kind = k; vm.filter = f; openActivity() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Greeting(vm.account ?: "", onAvatar = openProfile) }
        item {
            MonthSwitcher(vm.month.label(), vm.month < YearMonth.now(), { vm.changeMonth(-1) }, { vm.changeMonth(1) })
        }
        if (!vm.hasSmsPermission) item { PermissionHero(vm) }
        if (s == null) return@LazyColumn

        item { MonthHero(s, show) }
        if (vm.review.isNotEmpty()) item { ReviewCard(vm) }
        item { CategoryBreakdown(s, openCategory) }
        val unsorted = vm.txns.filter { it.effectiveFlow == Flow.SPEND && it.category == "Other" }
        if (unsorted.isNotEmpty()) item { SortOtherCard(unsorted) { picking = it } }
        val latest = vm.txns.filter { it.effectiveFlow == Flow.SPEND }.sortedByDescending { it.date }.take(5)
        if (latest.isNotEmpty()) {
            item {
                Tile(Modifier.fillMaxWidth(), padding = 6.dp) {
                    SectionTitle("Latest spends", Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp), action = "All", onAction = { show(Kind.Spent, null) })
                    Faint("Wrong category? Tap it. FinTrack learns it for next time.", Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    latest.forEach { t -> TxnRow(t, onCategory = { picking = t }) { editing = t } }
                }
            }
        }
    }
    editing?.let { t -> EditSheet(t, onDismiss = { editing = null }, vm = vm) }
    picking?.let { t ->
        CategoryPickerSheet(
            t, onDismiss = { picking = null }, why = vm.categoryReason(t),
            onPerson = { vm.markPerson(t); picking = null }.takeIf { t.direction == com.fintrack.core.Direction.DEBIT },
        ) { vm.quickCategory(t, it); picking = null }
    }
}

/**
 * The month in three numbers: money in, spent, left. Only real income and spending count;
 * money moved between your accounts and duplicates are listed separately, not added.
 */
@Composable
private fun MonthHero(s: MonthSummary, show: (Kind, TxnFilter?) -> Unit) {
    val white = Color.White
    val change = if (s.prevMonthSpent > 0) (s.spent - s.prevMonthSpent) / s.prevMonthSpent else null
    HeroTile(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().clickable { show(Kind.Spent, null) }) {
            Text("SPENT IN ${s.month.name().uppercase()}", style = CapsLabel, color = white.copy(alpha = 0.8f))
            AnimatedRupees(s.spent, MaterialTheme.typography.displaySmall, white, Modifier.padding(top = 4.dp))
            change?.let {
                Text(
                    "${if (it >= 0) "▲" else "▼"} ${"%.0f".format(abs(it * 100))}% vs ${s.month.minusMonths(1).name()}",
                    style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.85f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroNumber("Money in", s.received, Modifier.weight(1f)) { show(Kind.Received, null) }
            HeroNumber("Left", s.net, Modifier.weight(1f)) { show(Kind.All, null) }
        }
        val invested = s.byFlow[Flow.INVESTED] ?: 0.0
        val toPeople = s.byFlow[Flow.TO_PEOPLE] ?: 0.0
        if (invested > 0 || toPeople > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Left = money in − spent" + listOfNotNull(
                    invested.takeIf { it > 0 }?.let { " − invested ${rupees(it)}" },
                    toPeople.takeIf { it > 0 }?.let { " − sent to people ${rupees(it)}" },
                ).joinToString(""),
                style = MaterialTheme.typography.bodySmall, color = white.copy(alpha = 0.8f),
                modifier = Modifier.clickable { show(Kind.All, TxnFilter.OfFlow(if (toPeople > 0) Flow.TO_PEOPLE else Flow.INVESTED)) },
            )
        }
        val own = s.byFlow[Flow.OWN_TRANSFER] ?: 0.0
        val dup = s.byFlow[Flow.DUPLICATE] ?: 0.0
        val ignored = s.byFlow[Flow.IGNORED] ?: 0.0
        if (own > 0 || dup > 0 || ignored > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Not counted: " + listOfNotNull(
                    own.takeIf { it > 0 }?.let { "moved between your accounts ${rupees(it)}" },
                    dup.takeIf { it > 0 }?.let { "duplicates ${rupees(it)}" },
                    ignored.takeIf { it > 0 }?.let { "excluded by you ${rupees(it)}" },
                ).joinToString("  ·  ") + "  ›",
                style = MaterialTheme.typography.bodySmall, color = white.copy(alpha = 0.8f),
                modifier = Modifier.clickable {
                    show(Kind.All, TxnFilter.OfFlow(if (own > 0) Flow.OWN_TRANSFER else if (dup > 0) Flow.DUPLICATE else Flow.IGNORED))
                },
            )
        }
    }
}

@Composable
private fun HeroNumber(label: String, amount: Double, modifier: Modifier, onClick: () -> Unit) {
    val white = Color.White
    Column(modifier.clip(TileShape).background(white.copy(alpha = 0.14f)).clickable(onClick = onClick).padding(12.dp)) {
        Text(label.uppercase(), style = CapsLabel, color = white.copy(alpha = 0.8f))
        Text(
            (if (amount < 0) "−" else "") + rupees(abs(amount)),
            style = MaterialTheme.typography.titleLarge.merge(MoneyStyle), color = white, maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * "Other" grouped by payee, biggest first: one tap per payee sorts every payment to it, now and
 * later, and teaches the model about similar names.
 */
@Composable
private fun SortOtherCard(unsorted: List<Txn>, pick: (Txn) -> Unit) {
    val c = Fin.colors
    val payees = unsorted.groupBy { it.merchant }.entries.sortedByDescending { (_, v) -> v.sumOf { it.amount } }
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Sort ${rupees(unsorted.sumOf { it.amount })} in Other")
        Spacer(Modifier.height(4.dp))
        Faint("Optional: your totals are already right. One tap per payee sharpens the breakdown, and FinTrack remembers it for good.")
        payees.take(6).forEach { (name, list) ->
            Row(
                Modifier.fillMaxWidth().clip(TileShape).clickable { pick(list.first()) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1)
                    Faint("${list.size} payment${if (list.size == 1) "" else "s"}  ·  ${rupees(list.sumOf { it.amount })}")
                }
                Text("Choose  ▾", style = MaterialTheme.typography.labelLarge, color = c.ink,
                    modifier = Modifier.border(1.dp, c.border).padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
        if (payees.size > 6) Faint("+ ${payees.size - 6} more. They'll show here as you sort these.")
    }
}

/** Spending split into Food, Rent, Bills…: amount, share and number of payments. Tap to open one. */
@Composable
private fun CategoryBreakdown(s: MonthSummary, openCategory: (String) -> Unit) {
    val c = Fin.colors
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Spending by category")
        Spacer(Modifier.height(4.dp))
        if (s.byCategory.isEmpty()) {
            Faint("No spending this month yet.")
            return@Tile
        }
        Faint("Tap a category to see its payments")
        val top = s.byCategory.first().amount
        s.byCategory.forEach { a ->
            Column(Modifier.fillMaxWidth().clip(TileShape).clickable { openCategory(a.label) }.padding(vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(categoryColor(a.label)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, style = MaterialTheme.typography.bodyLarge, color = c.ink, maxLines = 1)
                        Faint("${a.count} payment${if (a.count == 1) "" else "s"}  ·  ${"%.0f".format(a.amount / s.spent * 100)}%")
                    }
                    Text(rupees(a.amount), style = MaterialTheme.typography.titleSmall.merge(MoneyStyle), color = c.ink)
                    Text("  ›", color = c.inkFaint)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).background(c.track)) {
                    Box(Modifier.fillMaxWidth((a.amount / top).toFloat().coerceIn(0.02f, 1f)).height(4.dp).background(categoryColor(a.label)))
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String, onAvatar: () -> Unit) {
    val hello = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(hello, style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft)
            Text(name.ifBlank { "there" }, style = MaterialTheme.typography.headlineMedium, color = Fin.colors.ink)
        }
        Column(Modifier.clip(CircleShape).clickable(onClick = onAvatar)) { Avatar(name, size = 44.dp, gradient = true) }
    }
}

/** One-tap questions whose answers fix the numbers and teach the model. */
@Composable
private fun ReviewCard(vm: FinTrackViewModel) {
    val c = Fin.colors
    Tile(Modifier.fillMaxWidth()) {
        SectionTitle("Needs your input · ${vm.review.size}")
        Spacer(Modifier.height(4.dp))
        Faint("Each answer fixes your totals and teaches FinTrack for next time.")
        vm.review.take(3).forEach { item ->
            Spacer(Modifier.height(14.dp))
            when (item) {
                is ReviewItem.ShopOrPerson -> {
                    Text(
                        "You paid ${item.payee} ${item.count}× (${rupees(item.total)}) to a personal UPI ID. Who is it?",
                        style = MaterialTheme.typography.bodyLarge, color = c.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PopChip("Shop", selected = true) { vm.answerShopOrPerson(item, Flow.SPEND) }
                        PopChip("Person", selected = false) { vm.answerShopOrPerson(item, Flow.TO_PEOPLE) }
                        PopChip("Me", selected = false) { vm.answerShopOrPerson(item, Flow.OWN_TRANSFER) }
                    }
                    Faint("Me = your own account elsewhere: not spending.", Modifier.padding(top = 6.dp))
                }
                is ReviewItem.PossibleOwnTransfer -> {
                    Text(
                        "${rupees(item.out.amount)} left${item.out.account?.let { " ••$it" } ?: ""} on ${item.out.date.short()} and the same arrived" +
                            "${item.into.account?.let { " in ••$it" } ?: ""} on ${item.into.date.short()}. Moved between your own accounts?",
                        style = MaterialTheme.typography.bodyLarge, color = c.ink,
                    )
                    Faint("If yes, neither counts as spending or income.", Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PopChip("Yes · my accounts", selected = true) { vm.answerOwnTransfer(item, isOwn = true) }
                        PopChip("No", selected = false) { vm.answerOwnTransfer(item, isOwn = false) }
                    }
                }
                is ReviewItem.PossibleDuplicate -> {
                    Text(
                        "Same payment reported twice? ${rupees(item.keep.amount)} · ${item.keep.merchant} on ${item.keep.date.short()}" +
                            " and ${item.duplicate.merchant} on ${item.duplicate.date.short()}.",
                        style = MaterialTheme.typography.bodyLarge, color = c.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PopChip("Yes · count once", selected = true) { vm.answerDuplicate(item, isDuplicate = true) }
                        PopChip("No · both real", selected = false) { vm.answerDuplicate(item, isDuplicate = false) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionHero(vm: FinTrackViewModel) {
    val context = LocalContext.current
    // Set once the request comes back without SMS access: the phone blocked it or the user said no.
    var blocked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionResult()
        blocked = !vm.hasSmsPermission
    }
    val white = Color.White
    HeroTile(Modifier.fillMaxWidth()) {
        if (!blocked) {
            Text("Connect your messages", style = MaterialTheme.typography.titleLarge, color = white)
            Spacer(Modifier.height(8.dp))
            Text(
                "FinTrack reads bank transaction SMS to build your dashboard. Everything stays on this phone. " +
                    "OTPs and personal chats are ignored.",
                style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(18.dp))
            HeroButton("Allow SMS access") { launcher.launch(PermissionHelper.SMS_PERMISSIONS) }
        } else {
            Text("Your phone blocked SMS access", style = MaterialTheme.typography.titleLarge, color = white)
            Spacer(Modifier.height(8.dp))
            Text(
                "Phones protect SMS for apps installed outside the Play Store. Allow it once in Settings:",
                style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(12.dp))
            val steps = if (PermissionHelper.isXiaomi) listOf(
                "Tap Open SMS permission below",
                "Set SMS (Read SMS) to Allow",
                "Greyed out? Tap Open App info, then ⋮ (top-right) → Allow restricted settings, and try again",
            ) else listOf(
                "Tap Open App info below",
                "Tap ⋮ (top-right) → Allow restricted settings, if you see it",
                "Permissions → SMS → Allow",
            )
            steps.forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("${i + 1}.", color = white, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(22.dp))
                    Text(step, color = white, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (PermissionHelper.isXiaomi) {
                    HeroButton("Open SMS permission") { PermissionHelper.openSmsSettings(context) }
                }
                HeroButton("Open App info", subtle = PermissionHelper.isXiaomi) { PermissionHelper.openAppInfo(context) }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Meanwhile: open any bank SMS → Share → FinTrack to add it by hand.",
                style = MaterialTheme.typography.bodySmall, color = white.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun HeroButton(text: String, subtle: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (subtle) Color.White else Fin.colors.accent,
        modifier = Modifier
            .clip(TileShape)
            .background(if (subtle) Color.White.copy(alpha = 0.18f) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
