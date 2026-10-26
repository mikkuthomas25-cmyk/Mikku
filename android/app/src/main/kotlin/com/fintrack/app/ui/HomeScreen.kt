package com.fintrack.app.ui

import android.Manifest
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
import androidx.compose.ui.unit.dp
import com.fintrack.core.MonthSummary
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun HomeScreen(vm: FinTrackViewModel, openActivity: () -> Unit, openProfile: () -> Unit) {
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
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle("Daily rhythm")
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
                    Faint("Tap a bar to see that day  ·  dashed line = daily average")
                }
            }
        }
        item {
            var all by rememberSaveable { mutableStateOf(false) }
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle(
                    "Categories",
                    action = if (s.byCategory.size > 5) (if (all) "Less" else "See all") else null,
                    onAction = { all = !all },
                )
                Spacer(Modifier.height(16.dp))
                RankedList(if (all) s.byCategory else s.byCategory.take(5), s.spent, onClick = {
                    vm.filter = TxnFilter.Category(it)
                    openActivity()
                })
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionResult()
    }
    HeroCard(Modifier.fillMaxWidth()) {
        Text("Connect your messages", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "FinTrack reads bank transaction SMS to build your dashboard. Everything stays on this phone. " +
                "OTPs and personal chats are ignored.",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.clip(PillShape).background(Color.White).clickable {
                launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            }.padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Allow SMS access", style = MaterialTheme.typography.labelLarge, color = Fin.colors.heroStart)
            Spacer(Modifier.width(6.dp))
            Text("›", color = Fin.colors.heroStart)
        }
    }
}
