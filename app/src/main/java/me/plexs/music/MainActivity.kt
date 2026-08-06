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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import me.plexs.music.ui.screens.liked.LikedScreen
import me.plexs.music.ui.screens.recents.RecentsScreen
import me.plexs.music.ui.screens.settings.SettingsScreen
import me.plexs.music.ui.theme.PlexAccent
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
                val goCategory: (String) -> Unit = { dest ->
                    navController.navigate(dest) { launchSingleTop = true }
                }
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
                            navController.navigate(Destinations.HOME) {
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
                            navController.navigate(Destinations.HOME) {
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
                            currentRoute = Destinations.HOME,
                            onSelectCategory = goCategory,
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            HomeScreen(services, onOpenSection = goCategory)
                        }
                    }
                    composable(Destinations.SEARCH) {
                        PlayerShell(
                            currentRoute = Destinations.SEARCH,
                            onSelectCategory = goCategory,
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            SearchScreen(services)
                        }
                    }
                    composable(Destinations.LIKED) {
                        PlayerShell(
                            currentRoute = Destinations.LIKED,
                            onSelectCategory = goCategory,
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            LikedScreen(services)
                        }
                    }
                    composable(Destinations.RECENTS) {
                        PlayerShell(
                            currentRoute = Destinations.RECENTS,
                            onSelectCategory = goCategory,
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            RecentsScreen(services)
                        }
                    }
                    composable(Destinations.SETTINGS) {
                        PlayerShell(
                            currentRoute = Destinations.SETTINGS,
                            onSelectCategory = goCategory,
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            SettingsScreen(services, authVm) {
                                navController.navigate(Destinations.SIGN_IN) {
                                    popUpTo(Destinations.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(Destinations.NOW_PLAYING) {
                        PlayerShell(
                            currentRoute = Destinations.SEARCH,
                            onSelectCategory = goCategory,
                            onPlayerTap = { navController.popBackStack() },
                            showBottomBar = false,
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
    currentRoute: String,
    onSelectCategory: (String) -> Unit,
    onPlayerTap: () -> Unit,
    showBottomBar: Boolean = true,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            CategoryBar(currentRoute = currentRoute, onSelect = onSelectCategory)
        },
        bottomBar = {
            if (showBottomBar) {
                PlayerBar(onTap = onPlayerTap)
            }
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

private val CategoryTabs = listOf(
    Destinations.HOME to "Home",
    Destinations.SEARCH to "Search",
    Destinations.LIKED to "Liked",
    Destinations.RECENTS to "Recent",
    Destinations.SETTINGS to "Settings",
)

@Composable
fun CategoryBar(
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTabs.forEach { (route, label) ->
            val active = currentRoute == route
            val icon = when (route) {
                Destinations.HOME -> Icons.Filled.Home
                Destinations.SEARCH -> Icons.Filled.Search
                Destinations.LIKED -> Icons.Filled.Favorite
                Destinations.RECENTS -> Icons.Filled.History
                Destinations.SETTINGS -> Icons.Filled.Settings
                else -> Icons.Filled.Home
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = { onSelect(route) })
                    .padding(vertical = 4.dp),
            ) {
                Icon(icon, contentDescription = label, tint = if (active) PlexAccent else PlexMuted)
                Text(
                    label,
                    color = if (active) PlexAccent else PlexMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}