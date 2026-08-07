package me.plexs.music.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import me.plexs.music.data.api.AuthState
import me.plexs.music.data.auth.AuthRepository
import me.plexs.music.data.session.SessionStore
import me.plexs.music.ui.navigation.Destinations
import me.plexs.music.ui.theme.PlexAccent

@Composable
fun SplashScreen(
    session: SessionStore,
    onDone: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "PLEX",
            color = PlexAccent,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
    }

    LaunchedEffect(Unit) {
        val dest = decide(session)
        onDone(dest)
    }
}

private suspend fun decide(session: SessionStore): String {
    if (!session.isSignedIn) return Destinations.SIGN_IN
    // Network blip with a stored token still lands on Home rather than a flash to auth.
    val state = withTimeoutOrNull(5000) { runCatching { AuthRepository(session).getSession() }.getOrDefault(AuthState(session = null)) }
        ?: AuthState(session = null)
    return if (state.session != null) Destinations.MAIN else Destinations.SIGN_IN
}