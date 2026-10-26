package com.fintrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fintrack.app.ui.FinTrackTheme
import com.fintrack.app.ui.FinTrackViewModel
import com.fintrack.app.ui.LoginScreen
import com.fintrack.app.ui.MainScreen

class MainActivity : ComponentActivity() {
    private val vm: FinTrackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinTrackTheme {
                // Each screen draws its own glossy backdrop.
                if (vm.account == null) LoginScreen(vm) else MainScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pick up SMS that arrived while the app was in the background.
        if (vm.account != null) vm.onResume()
    }
}
