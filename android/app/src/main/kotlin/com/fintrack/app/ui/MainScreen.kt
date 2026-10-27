package com.fintrack.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NAV = listOf(
    NavItem("Home", Icons.Filled.Home),
    NavItem("Charts", Icons.Filled.Star),
    NavItem("Activity", Icons.AutoMirrored.Filled.List),
    NavItem("Settings", Icons.Filled.Settings),
)

/** Content padding shared by every tab: room for the status bar on top, the floating nav below. */
val ScreenPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp)

@Composable
fun MainScreen(vm: FinTrackViewModel, startTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    // Category drill-down (e.g. Food) shown over the current tab.
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    // "+ Add" in the tab bar: a cash (or any missed) transaction, from any tab.
    var adding by rememberSaveable { mutableStateOf(false) }
    val openCategory: (String) -> Unit = { category = it }
    // Back from any tab returns Home before leaving the app.
    BackHandler(enabled = tab != 0 && category == null) { tab = 0 }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        val text = vm.message ?: return@LaunchedEffect
        val undo = vm.undo
        vm.message = null
        vm.undo = null
        val result = snackbar.showSnackbar(text, actionLabel = if (undo != null) "UNDO" else null, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) undo?.invoke()
    }

    AppBackground {
        AnimatedContent(
            targetState = tab to category,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "tab",
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) { (t, cat) ->
            when {
                cat != null -> CategoryScreen(vm, cat, onBack = { category = null })
                t == 0 -> HomeScreen(
                    vm, openActivity = { tab = 2 }, openProfile = { tab = 3 }, openCategory = openCategory,
                    onAdd = { adding = true }, openCharts = { tab = 1 },
                )
                t == 1 -> InsightsScreen(vm, openCategory, openActivity = { tab = 2 })
                t == 2 -> ActivityScreen(vm)
                else -> ProfileScreen(vm, openInsights = { tab = 1 }, openTransactions = { f ->
                    vm.kind = Kind.All
                    vm.filter = f
                    tab = 2
                })
            }
        }
        if (vm.busy) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().statusBarsPadding().height(3.dp),
                color = Fin.colors.accent, trackColor = Color.Transparent,
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
        PopNavBar(
            NAV, tab, { category = null; tab = it },
            Modifier.align(Alignment.BottomCenter),
            onAdd = { adding = true },
        )
    }
    if (adding) AddSheet(onDismiss = { adding = false }) { date, amount, dir, what, choice ->
        vm.addManual(date, amount, dir, what, choice)
        adding = false
    }
}
