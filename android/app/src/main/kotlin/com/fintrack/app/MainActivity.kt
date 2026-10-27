package com.fintrack.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fintrack.app.ui.FinTrackTheme
import com.fintrack.app.ui.FinTrackViewModel
import com.fintrack.app.ui.LoginScreen
import com.fintrack.app.ui.MainScreen
import com.fintrack.app.ui.SetupScreen
import com.fintrack.app.ui.SetupStep

class MainActivity : ComponentActivity() {
    private val vm: FinTrackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always-dark UI: light status/navigation bar icons regardless of the phone's theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handleShare(intent)
        setContent {
            FinTrackTheme {
                // Each screen draws its own glossy backdrop.
                when {
                    vm.account == null -> LoginScreen(vm)
                    vm.setup != SetupStep.Done -> SetupScreen(vm)
                    else -> MainScreen(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Messages > Share > FinTrack: adds a bank SMS without needing SMS permission. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let(vm::addShared)
        // Don't re-add the same text after a rotation or when the activity is recreated.
        intent.action = null
    }

    override fun onResume() {
        super.onResume()
        // Pick up SMS that arrived while the app was in the background.
        if (vm.account != null) vm.onResume()
    }
}
