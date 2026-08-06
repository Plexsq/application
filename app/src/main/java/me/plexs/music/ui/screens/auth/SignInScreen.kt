package me.plexs.music.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.plexs.music.data.api.ConfigRepository
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.auth.GoogleAuth
import me.plexs.music.ui.components.PlexButton
import me.plexs.music.ui.components.PlexTextInput
import me.plexs.music.ui.components.QrScanner
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexError

@Composable
fun SignInScreen(
    vm: AuthViewModel,
    config: ConfigRepository,
    onSignedIn: () -> Unit,
    onForgot: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleClientId by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scanError = null
            scanning = true
        } else {
            scanError = "Camera permission is required to scan the QR code"
        }
    }

    LaunchedEffect(Unit) {
        googleClientId = runCatching { config.config().googleClientId }.getOrDefault("")
    }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (googleClientId.isNotBlank()) {
            GoogleAuth(context, googleClientId).idToken(result)?.let { token -> vm.google(token) { onSignedIn() } }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (scanning) {
            QrScanner(
                onToken = { token ->
                    scanning = false
                    vm.qrScan(token) { onSignedIn() }
                },
                onError = { msg ->
                    scanning = false
                    scanError = msg
                },
                modifier = Modifier.fillMaxSize(),
            )
            TextButton(
                onClick = { scanning = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) { Text("Close", color = Color.White) }
            Text(
                text = scanError ?: "Point the camera at the QR code shown in the website's Settings → Mobile App.",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("PLEX", color = PlexAccent, fontSize = 40.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(32.dp))
                PlexTextInput(email, { email = it }, "Email")
                Spacer(Modifier.height(12.dp))
                PlexTextInput(password, { password = it }, "Password", isPassword = true)
                Spacer(Modifier.height(20.dp))
                vm.error?.let {
                    Text(it, color = PlexError, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                scanError?.let {
                    Text(it, color = PlexError, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                PlexButton("Sign in", loading = vm.loading, onClick = {
                    vm.signIn(email, password) { onSignedIn() }
                })
                Spacer(Modifier.height(12.dp))
                if (googleClientId.isNotBlank()) {
                    OutlinedButton(
                        onClick = { googleLauncher.launch(GoogleAuth(context, googleClientId).signInIntent()) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Continue with Google") }
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(onClick = { camPerm.launch(android.Manifest.permission.CAMERA) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Scan QR code to link this device")
                }
                TextButton(onClick = onForgot, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Forgot password?")
                }
                TextButton(onClick = onCreateAccount, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Create an account")
                }
            }
        }
    }
}