package com.liuyin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.liuyin.app.player.LiuYinPlayer
import com.liuyin.app.ui.components.MiniPlayer
import com.liuyin.app.ui.main.MainScreen
import com.liuyin.app.ui.main.MainViewModel
import com.liuyin.app.ui.navigation.BottomNavItem
import com.liuyin.app.ui.player.PlayerScreen
import com.liuyin.app.ui.playlist.PlaylistScreen
import com.liuyin.app.ui.playlist.PlaylistViewModel
import com.liuyin.app.ui.settings.SettingsScreen
import com.liuyin.app.ui.theme.LiuYinTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bvidFromShare = extractBvidFromShareIntent(intent)

        setContent {
            LiuYinTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val navController = rememberNavController()

                LaunchedEffect(bvidFromShare) {
                    bvidFromShare?.let { mainViewModel.playBvid(it) }
                }

                val currentAudio by mainViewModel.player.currentAudio
                    .collectAsStateWithLifecycle()
                val playbackState by mainViewModel.player.playbackState
                    .collectAsStateWithLifecycle()
                val showFullPlayer by mainViewModel.showFullPlayer
                    .collectAsStateWithLifecycle()

                val isPlaying = playbackState == LiuYinPlayer.PlaybackState.PLAYING

                if (showFullPlayer) {
                    BackHandler {
                        mainViewModel.hidePlayer()
                    }
                }

                Scaffold(
                    bottomBar = {
                        Column {
                            AnimatedVisibility(
                                visible = currentAudio != null && !showFullPlayer,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it }
                            ) {
                                currentAudio?.let { audio ->
                                    MiniPlayer(
                                        audioInfo = audio,
                                        isPlaying = isPlaying,
                                        onPlayPause = { mainViewModel.player.togglePlayPause() },
                                        onExpand = { mainViewModel.showPlayer() }
                                    )
                                }
                            }

                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route

                                BottomNavItem.items.forEach { item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = BottomNavItem.Home.route
                        ) {
                            composable(BottomNavItem.Home.route) {
                                MainScreen(viewModel = mainViewModel)
                            }
                            composable(BottomNavItem.Playlist.route) {
                                val playlistViewModel: PlaylistViewModel = hiltViewModel()
                                PlaylistScreen(viewModel = playlistViewModel)
                            }
                            composable(BottomNavItem.Settings.route) {
                                SettingsScreen()
                            }
                        }

                        AnimatedVisibility(
                            visible = showFullPlayer,
                            enter = slideInVertically { fullHeight -> fullHeight },
                            exit = slideOutVertically { fullHeight -> fullHeight }
                        ) {
                            PlayerScreen(
                                player = mainViewModel.player,
                                onClose = { mainViewModel.hidePlayer() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun extractBvidFromShareIntent(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return MainViewModel.extractBvid(sharedText)
    }
}
