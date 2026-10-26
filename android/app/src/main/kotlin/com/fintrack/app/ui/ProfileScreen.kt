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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.fintrack.app.BuildConfig

@Composable
fun ProfileScreen(vm: FinTrackViewModel) {
    val c = Fin.colors
    var key by remember { mutableStateOf(vm.prefs.claudeApiKey) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Profile", style = MaterialTheme.typography.headlineMedium, color = c.ink) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
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
            GlassCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                SettingRow(
                    "Rescan messages",
                    if (vm.hasSmsPermission) "Re-read the last 6 months of bank SMS" else "Allow SMS access on Home first",
                    enabled = vm.hasSmsPermission && !vm.busy,
                ) { vm.scan(full = true) }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle("✦  Claude AI")
                Spacer(Modifier.height(6.dp))
                Faint(
                    "Paste an API key from console.anthropic.com to sort unknown merchants automatically and unlock " +
                        "Insights. Only merchant names and monthly totals are sent. The key stays on this phone.",
                )
                Spacer(Modifier.height(14.dp))
                FinTextField(key, { key = it }, "Claude API key", password = true)
                Spacer(Modifier.height(12.dp))
                GradientButton(if (vm.aiEnabled) "Update key" else "Save key", { vm.saveApiKey(key) }, Modifier.fillMaxWidth())
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), padding = 6.dp) {
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
        Modifier.fillMaxWidth().clip(CardShape).clickable(enabled = enabled, onClick = onClick).padding(14.dp),
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
