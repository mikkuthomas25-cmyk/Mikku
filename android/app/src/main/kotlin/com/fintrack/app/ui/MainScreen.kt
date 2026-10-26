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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NAV = listOf(
    NavItem("Home", Icons.Filled.Home),
    NavItem("Activity", Icons.AutoMirrored.Filled.List),
    NavItem("Insights", Icons.Filled.Star),
    NavItem("Profile", Icons.Filled.Person),
)

/** Content padding shared by every tab: room for the status bar on top, the floating nav below. */
val ScreenPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp)

@Composable
fun MainScreen(vm: FinTrackViewModel, startTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    // Back from any tab returns Home before leaving the app.
    BackHandler(enabled = tab != 0) { tab = 0 }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
    }

    GlossyBackground {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "tab",
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) { t ->
            when (t) {
                0 -> HomeScreen(vm, openActivity = { tab = 1 }, openProfile = { tab = 3 })
                1 -> ActivityScreen(vm)
                2 -> InsightsScreen(vm)
                else -> ProfileScreen(vm)
            }
        }
        if (vm.busy) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().statusBarsPadding().height(2.dp),
                color = Fin.colors.heroStart, trackColor = Color.Transparent,
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
        FloatingNavBar(
            NAV, tab, { tab = it },
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
        )
    }
}
