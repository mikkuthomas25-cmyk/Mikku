package com.fintrack.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fintrack.app.sms.PermissionHelper
import androidx.compose.ui.unit.dp
import com.fintrack.core.MonthSummary
import com.fintrack.core.Patterns
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun HomeScreen(vm: FinTrackViewModel, openActivity: () -> Unit, openProfile: () -> Unit, openCategory: (String) -> Unit) {
    val s = vm.summary
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

        item { SpendHero(s) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Today", s.todaySpent, Modifier.weight(1f))
                MiniStat("Daily average", s.avgPerDay, Modifier.weight(1f))
            }
        }
        item { PaceCard(vm, s) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle("Where it went")
                Spacer(Modifier.height(4.dp))
                Faint("Tap a category to see everything in it")
                Spacer(Modifier.height(16.dp))
                CategoryDonut(s.byCategory, s.spent, onOpen = openCategory)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle("Daily spending")
                Spacer(Modifier.height(14.dp))
                val sel = (vm.filter as? TxnFilter.Day)?.date
                DailyBars(s.byDay, s.avgPerDay, sel, onSelect = { vm.filter = it?.let(TxnFilter::Day) })
                Spacer(Modifier.height(10.dp))
                if (sel != null) {
                    val amount = s.byDay.firstOrNull { it.first == sel }?.second ?: 0.0
                    Row(
                        Modifier.fillMaxWidth().clip(PillShape).clickable(onClick = openActivity).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${sel.long()}  ·  ${rupees(amount)}", style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
                            color = Fin.colors.ink, modifier = Modifier.weight(1f))
                        Text("View  ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Faint("Tap a bar to see that day  ·  dashed line = your daily average  ·  dot = today")
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle("Top merchants")
                Spacer(Modifier.height(16.dp))
                RankedList(s.topMerchants.take(5), s.spent, leadingAvatar = true, onClick = {
                    vm.filter = TxnFilter.Merchant(it)
                    openActivity()
                })
            }
        }
    }
}

/** "Am I spending faster than usual?": running total vs last month, plus the learned forecast. */
@Composable
private fun PaceCard(vm: FinTrackViewModel, s: MonthSummary) {
    val c = Fin.colors
    val m = vm.month
    val today = LocalDate.now()
    val isCurrent = YearMonth.from(today) == m
    val thisMonth = Patterns.cumulative(vm.history, m, untilDay = if (isCurrent) today.dayOfMonth else null)
    val lastMonth = Patterns.cumulative(vm.history, m.minusMonths(1))
    val forecast = if (isCurrent) Patterns.forecast(vm.history, m, today) else null
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle("Spending pace")
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                forecast != null && forecast.basedOnMonths >= 2 ->
                    "On track for ${rupees(forecast.projected)} by month-end, based on how your last ${forecast.basedOnMonths} months went."
                forecast != null -> "At this pace: about ${rupees(forecast.projected)} by month-end."
                isCurrent -> "A forecast appears after a week of data."
                else -> "${rupees(s.spent)} spent in ${m.name()}."
            },
            style = MaterialTheme.typography.bodyMedium, color = c.inkSoft,
        )
        if (isCurrent && lastMonth.isNotEmpty() && thisMonth.isNotEmpty()) {
            val sameDayLast = lastMonth.getOrNull(thisMonth.size - 1) ?: lastMonth.last()
            val diff = thisMonth.last() - sameDayLast
            if (sameDayLast > 0) {
                Text(
                    "${if (diff > 0) "▲" else "▼"} ${rupees(kotlin.math.abs(diff))} ${if (diff > 0) "more" else "less"} than by this day last month",
                    style = MaterialTheme.typography.labelLarge, color = if (diff > 0) c.loss else c.gain,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PaceChart(thisMonth, lastMonth, m.lengthOfMonth(), forecast?.projected)
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

@Composable
private fun SpendHero(s: MonthSummary) {
    val white = Color.White
    val change = if (s.prevMonthSpent > 0) (s.spent - s.prevMonthSpent) / s.prevMonthSpent else null
    HeroCard(Modifier.fillMaxWidth()) {
        Text("Spent in ${s.month.name()}", style = MaterialTheme.typography.bodyMedium, color = white.copy(alpha = 0.8f))
        AnimatedRupees(s.spent, MaterialTheme.typography.displaySmall, white, Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                change == null -> "${s.txnCount} transactions"
                change >= 0 -> "▲ ${"%.0f".format(abs(change * 100))}% vs ${s.month.minusMonths(1).name()}"
                else -> "▼ ${"%.0f".format(abs(change * 100))}% vs ${s.month.minusMonths(1).name()}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = white,
            modifier = Modifier.clip(PillShape).background(white.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = white.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))
        Row {
            Stat("Received", rupees(s.received))
            Stat(if (s.net >= 0) "Saved" else "Overspent", rupees(abs(s.net)))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Double, modifier: Modifier) {
    GlassCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Fin.colors.inkSoft)
        AnimatedRupees(value, MaterialTheme.typography.titleLarge, Fin.colors.ink, Modifier.padding(top = 4.dp))
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
    HeroCard(Modifier.fillMaxWidth()) {
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
        color = if (subtle) Color.White else Fin.colors.heroStart,
        modifier = Modifier
            .clip(PillShape)
            .background(if (subtle) Color.White.copy(alpha = 0.18f) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
