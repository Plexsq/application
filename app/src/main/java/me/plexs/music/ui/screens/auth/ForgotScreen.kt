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
fun ForgotScreen(vm: AuthViewModel, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reset password", color = PlexAccent, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        vm.error?.let {
            Text(it, color = PlexError, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (step == 1) {
            Text("Enter your email and we'll send a reset code.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            PlexTextInput(email, { email = it }, "Email")
            Spacer(Modifier.height(20.dp))
            PlexButton("Send code", loading = vm.loading, onClick = {
                vm.forgot(email) { step = 2 }
            })
        } else {
            Text("Enter the code we emailed you, then set a new password.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            PlexTextInput(otp, { otp = it }, "Verification code")
            Spacer(Modifier.height(12.dp))
            PlexTextInput(newPassword, { newPassword = it }, "New password", isPassword = true)
            Spacer(Modifier.height(20.dp))
            PlexButton("Reset password", loading = vm.loading, onClick = {
                vm.verifyOtp(vm.pendingEmail ?: email, otp, newPassword) { onDone() }
            })
        }
        TextButton(onClick = { step = 1 }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}