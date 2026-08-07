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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
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
            val session = services.session
            val darkMode by services.themeMode.collectAsState()
            val accentHexValue by services.accentHex.collectAsState()
            val dark = darkMode != "light"
            val accent = me.plexs.music.ui.theme.accentColor(accentHexValue)
            PlexTheme(dark = dark, accent = accent) {
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
                            navController.navigate(Destinations.MAIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }, onForgot = {
                            navController.navigate(Destinations.FORGOT)
                        }, onCreateAccount = {
                            navController.navigate(Destinations.SIGN_UP)
                        })
                    }
                    composable(Destinations.SIGN_UP) {
                        SignUpScreen(authVm) {
                            navController.navigate(Destinations.MAIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    composable(Destinations.FORGOT) {
                        ForgotScreen(authVm) {
                            navController.navigate(Destinations.SIGN_IN)
                        }
                    }
                    composable(Destinations.MAIN) {
                        val pagerState = rememberPagerState(pageCount = { CategoryTabs.size })
                        val coroutineScope = rememberCoroutineScope()
                        val selectPage: (Int) -> Unit = { index ->
                            if (index != pagerState.currentPage) {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                        }
                        PlayerShell(
                            selectedIndex = pagerState.currentPage,
                            onSelect = selectPage,
                            onPlayerTap = {
                                navController.navigate(Destinations.NOW_PLAYING) {
                                    launchSingleTop = true
                                }
                            },
                        ) {
                            HorizontalPager(state = pagerState) { page ->
                                when (page) {
                                    0 -> HomeScreen(services, onOpenSection = { dest ->
                                        val idx = CategoryTabs.indexOfFirst { it.first == dest }
                                        if (idx >= 0) selectPage(idx) else navController.navigate(dest)
                                    })
                                    1 -> SearchScreen(services)
                                    2 -> LikedScreen(services)
                                    3 -> RecentsScreen(services)
                                    4 -> SettingsScreen(services, authVm, onSignedOut = {
                                        navController.navigate(Destinations.SIGN_IN) {
                                            popUpTo(Destinations.SPLASH) { inclusive = true }
                                        }
                                    }, onOpenOffline = {
                                        navController.navigate(Destinations.OFFLINE)
                                    })
                                }
                            }
                        }
                    }
                    composable(Destinations.NOW_PLAYING) {
                        Scaffold {
                            NowPlayingScreen(onClose = { navController.popBackStack() })
                        }
                    }
                    composable(Destinations.OFFLINE) {
                        me.plexs.music.ui.screens.offline.OfflineScreen(services)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        me.plexs.music.playback.PlaybackController.refresh(this)
    }
}

class AuthVmFactory(private val services: PlexApp.Services) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(services.auth, services.session) as T
}

@Composable
fun PlayerShell(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onPlayerTap: () -> Unit,
    showBottomBar: Boolean = true,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            CategoryBar(selectedIndex = selectedIndex, onSelect = onSelect)
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
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTabs.forEachIndexed { index, (route, label) ->
            val active = selectedIndex == index
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
                    .clickable(onClick = { onSelect(index) })
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