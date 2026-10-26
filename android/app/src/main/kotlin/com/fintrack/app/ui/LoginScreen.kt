package com.fintrack.app.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(vm: FinTrackViewModel) {
    val activity = LocalContext.current as Activity
    val c = Fin.colors
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var creating by rememberSaveable { mutableStateOf(false) }
    val ready = !vm.busy && vm.auth.configured
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
    }

    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppMark()
            Spacer(Modifier.height(20.dp))
            Text("FinTrack", style = MaterialTheme.typography.displaySmall, color = c.ink)
            Text(
                "Every rupee, accounted for.",
                style = MaterialTheme.typography.bodyLarge, color = c.inkSoft, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp),
            )

            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!vm.auth.configured) {
                    Tile(Modifier.fillMaxWidth()) {
                        Text("Try it on this phone", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Spacer(Modifier.height(4.dp))
                        Faint("Google and email sign-in switch on once Firebase is connected (see android/README.md).")
                        Spacer(Modifier.height(14.dp))
                        PopButton("Continue without account", vm::continueWithoutAccount, Modifier.fillMaxWidth())
                    }
                }

                Tile(Modifier.fillMaxWidth(), padding = 20.dp) {
                    PopOutlineButton(
                        "Continue with Google", onClick = { vm.signInWithGoogle(activity) },
                        modifier = Modifier.fillMaxWidth(), enabled = ready,
                        leading = { Text("G", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4285F4)) },
                    )
                    Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = c.track)
                        Text("  or with email  ", style = MaterialTheme.typography.labelMedium, color = c.inkFaint)
                        HorizontalDivider(Modifier.weight(1f), color = c.track)
                    }
                    PopTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email, enabled = ready)
                    Spacer(Modifier.height(12.dp))
                    PopTextField(password, { password = it }, "Password", password = true,
                        keyboardType = KeyboardType.Password, enabled = ready)
                    Spacer(Modifier.height(18.dp))
                    PopButton(
                        when {
                            vm.busy -> "Please wait…"
                            creating -> "Create account"
                            else -> "Sign in"
                        },
                        onClick = {
                            if (creating) vm.signIn { createAccount(email, password) }
                            else vm.signIn { signInWithEmail(email, password) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = ready && email.isNotBlank() && password.length >= 6,
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { creating = !creating }, enabled = vm.auth.configured) {
                            Text(if (creating) "I have an account" else "Create account")
                        }
                        if (!creating) {
                            TextButton(onClick = { vm.resetPassword(email) }, enabled = vm.auth.configured && email.isNotBlank()) {
                                Text("Forgot password?")
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp))
    }
}

/** App mark: three rising bars on an accent plunk tile. */
@Composable
private fun AppMark() {
    val c = Fin.colors
    Plunk(c.accent, c.accentEdgeRight, c.accentEdgeBottom, faceModifier = Modifier.size(84.dp), depth = 6.dp) {
        Canvas(Modifier.size(40.dp).align(Alignment.Center)) {
            val bw = size.width / 4.6f
            val gap = (size.width - bw * 3) / 2
            listOf(0.45f, 0.7f, 1f).forEachIndexed { i, h ->
                val barH = size.height * h
                drawRect(Color.White, Offset(i * (bw + gap), size.height - barH), Size(bw, barH))
            }
        }
    }
}
