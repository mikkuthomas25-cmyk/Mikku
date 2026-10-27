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
    NavItem("Transactions", Icons.AutoMirrored.Filled.List),
    NavItem("Settings", Icons.Filled.Settings),
)

/** Content padding shared by every tab: room for the status bar on top, the floating nav below. */
val ScreenPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp)

@Composable
fun MainScreen(vm: FinTrackViewModel, startTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    // Category drill-down (e.g. Food) shown over the current tab.
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    // Charts and checks, opened from Settings.
    var insights by rememberSaveable { mutableStateOf(false) }
    val openCategory: (String) -> Unit = { category = it }
    // Back from any tab returns Home before leaving the app.
    BackHandler(enabled = insights && category == null) { insights = false }
    BackHandler(enabled = tab != 0 && category == null && !insights) { tab = 0 }

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
            targetState = Triple(tab, category, insights),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "tab",
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) { (t, cat, ins) ->
            when {
                cat != null -> CategoryScreen(vm, cat, onBack = { category = null })
                ins -> InsightsScreen(vm, openCategory, openActivity = { insights = false; tab = 1 }, onBack = { insights = false })
                t == 0 -> HomeScreen(vm, openActivity = { tab = 1 }, openProfile = { tab = 2 }, openCategory = openCategory)
                t == 1 -> ActivityScreen(vm)
                else -> ProfileScreen(vm, openInsights = { insights = true }, openTransactions = { f ->
                    vm.kind = Kind.All
                    vm.filter = f
                    tab = 1
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
            NAV, tab, { category = null; insights = false; tab = it },
            Modifier.align(Alignment.BottomCenter),
        )
    }
}
