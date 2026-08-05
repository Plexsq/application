package me.plexs.music.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.components.PlexButton
import me.plexs.music.ui.components.PlexTextInput
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexError

@Composable
fun SignUpScreen(vm: AuthViewModel, onSignedUp: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Create account", color = PlexAccent, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        vm.error?.let {
            Text(it, color = PlexError, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (step == 1) {
            PlexTextInput(name, { name = it }, "Name")
            Spacer(Modifier.height(12.dp))
            PlexTextInput(username, { username = it }, "Username")
            Spacer(Modifier.height(12.dp))
            PlexTextInput(email, { email = it }, "Email")
            Spacer(Modifier.height(12.dp))
            PlexTextInput(password, { password = it }, "Password", isPassword = true)
            Spacer(Modifier.height(20.dp))
            PlexButton("Continue", loading = vm.loading, onClick = {
                vm.signUp(name, username, email, password) { step = 2 }
            })
        } else {
            Text(
                "We sent a code to ${vm.pendingEmail ?: email}. Enter it below.",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            PlexTextInput(otp, { otp = it }, "Verification code")
            Spacer(Modifier.height(20.dp))
            PlexButton("Verify", loading = vm.loading, onClick = {
                vm.verifySignup(vm.pendingEmail ?: email, otp) { onSignedUp() }
            })
        }
        TextButton(
            onClick = { step = 1 },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Back") }
    }
}