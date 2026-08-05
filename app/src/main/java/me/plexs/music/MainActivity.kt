package me.plexs.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.components.PlayerBar
import me.plexs.music.ui.navigation.Destinations
import me.plexs.music.ui.screens.auth.ForgotScreen
import me.plexs.music.ui.screens.auth.SignInScreen
import me.plexs.music.ui.screens.auth.SignUpScreen
import me.plexs.music.ui.screens.home.HomeScreen
import me.plexs.music.ui.screens.player.NowPlayingScreen
import me.plexs.music.ui.screens.search.SearchScreen
import me.plexs.music.ui.screens.splash.SplashScreen
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexTheme

class MainActivity : ComponentActivity() {

    private val services get() = (application as PlexApp).services

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001,
            )
        }
        me.plexs.music.playback.PlaybackController.restore(this)
        setContent {
            PlexTheme {
                val navController = rememberNavController()
                val authVm: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = AuthVmFactory(services),
                )
                NavHost(navController, startDestination = Destinations.SPLASH) {
                    composable(Destinations.SPLASH) {
                        SplashScreen(services.session) { dest ->
                            navController.navigate(dest) {
                                popUpTo(Destinations.SPLASH) { inclusive = true }
                            }
                        }
                    }
                    composable(Destinations.SIGN_IN) {
                        SignInScreen(authVm, services.config, onSignedIn = {
                            navController.navigate(Destinations.SEARCH) {
                                popUpTo(Destinations.SPLASH) { inclusive = true }
                            }
                        }, onForgot = {
                            navController.navigate(Destinations.FORGOT)
                        }, onCreateAccount = {
                            navController.navigate(Destinations.SIGN_UP)
                        })
                    }
                    composable(Destinations.SIGN_UP) {
                        SignUpScreen(authVm) {
                            navController.navigate(Destinations.SEARCH) {
                                popUpTo(Destinations.SPLASH) { inclusive = true }
                            }
                        }
                    }
                    composable(Destinations.FORGOT) {
                        ForgotScreen(authVm) {
                            navController.navigate(Destinations.SIGN_IN)
                        }
                    }
                    composable(Destinations.HOME) {
                        PlayerShell(
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING)
                            },
                            onHome = { /* already home */ },
                            onSearch = {
                                navController.navigate(Destinations.SEARCH) {
                                    popUpTo(Destinations.HOME) { inclusive = true }
                                }
                            },
                            currentRoute = Destinations.HOME,
                        ) {
                            HomeScreen(services, authVm) {
                                navController.navigate(Destinations.SIGN_IN) {
                                    popUpTo(Destinations.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(Destinations.SEARCH) {
                        PlayerShell(
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING)
                            },
                            onHome = {
                                navController.navigate(Destinations.HOME) {
                                    popUpTo(Destinations.SEARCH) { inclusive = true }
                                }
                            },
                            onSearch = { /* already search */ },
                            currentRoute = Destinations.SEARCH,
                        ) {
                            SearchScreen(services)
                        }
                    }
                    composable(Destinations.NOW_PLAYING) {
                        PlayerShell(
                            onPlayerTap = { navController.popBackStack() },
                            onHome = {
                                navController.popBackStack(Destinations.SEARCH, inclusive = false)
                            },
                            onSearch = {
                                navController.popBackStack(Destinations.SEARCH, inclusive = false)
                            },
                            currentRoute = Destinations.SEARCH,
                        ) {
                            NowPlayingScreen(onClose = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

class AuthVmFactory(private val services: PlexApp.Services) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(services.auth, services.session) as T
}

@Composable
fun PlayerShell(
    onPlayerTap: () -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    currentRoute: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(currentRoute = currentRoute, onHome = onHome, onSearch = onSearch)
        },
        bottomBar = {
            PlayerBar(onTap = onPlayerTap)
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}

@Composable
fun TopBar(
    currentRoute: String,
    onHome: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "PLEX",
            color = PlexAccent,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            fontSize = 24.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onHome),
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSearch) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = if (currentRoute == Destinations.SEARCH) PlexAccent else PlexMuted,
            )
        }
    }
}