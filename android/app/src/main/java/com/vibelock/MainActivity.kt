package com.vibelock

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibelock.data.model.MatchState
import com.vibelock.service.VibeLockService
import com.vibelock.ui.screen.DrawingScreen
import com.vibelock.ui.screen.HomeScreen
import com.vibelock.ui.screen.MatchingScreen
import com.vibelock.ui.theme.VibeLockTheme
import com.vibelock.ui.theme.VibeSurface
import com.vibelock.ui.viewmodel.DrawingViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Matching : Screen("matching")
    data object Drawing : Screen("drawing")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Allow activity to show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            VibeLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VibeSurface,
                ) {
                    val navController = rememberNavController()
                    val viewModel: DrawingViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    // Drive navigation from match state
                    val matchState = uiState.matchState
                    androidx.compose.runtime.LaunchedEffect(matchState) {
                        when (matchState) {
                            is MatchState.Idle, is MatchState.Disconnected -> {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                            is MatchState.Searching -> {
                                navController.navigate(Screen.Matching.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                            is MatchState.Matched -> {
                                navController.navigate(Screen.Drawing.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onFindMatch = { viewModel.findMatch() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        composable(Screen.Matching.route) {
                            MatchingScreen(
                                queueSize = (uiState.matchState as? MatchState.Searching)?.queueSize ?: 1,
                                onCancel = { viewModel.endSession() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        composable(Screen.Drawing.route) {
                            val matched = uiState.matchState as? MatchState.Matched
                            if (matched != null) {
                                DrawingScreen(
                                    matchState = matched,
                                    localPaths = uiState.localPaths,
                                    remotePaths = uiState.remotePaths,
                                    currentLocalPath = uiState.currentLocalPath,
                                    currentRemotePath = uiState.currentRemotePath,
                                    selectedColor = uiState.selectedColor,
                                    strokeWidth = uiState.strokeWidth,
                                    guessInput = uiState.guessInput,
                                    guessResult = uiState.guessResult,
                                    showSkipConfirm = uiState.showSkipConfirm,
                                    onDrawStart = viewModel::onDrawStart,
                                    onDrawMove = viewModel::onDrawMove,
                                    onDrawEnd = viewModel::onDrawEnd,
                                    onClear = viewModel::clearCanvas,
                                    onColorSelected = viewModel::selectColor,
                                    onStrokeSelected = viewModel::selectStroke,
                                    onGuessInput = viewModel::onGuessInput,
                                    onGuessSubmit = viewModel::submitGuess,
                                    onSkipRequest = viewModel::requestSkipConfirm,
                                    onSkipConfirm = viewModel::skip,
                                    onSkipDismiss = viewModel::dismissSkipConfirm,
                                    onGuessResultDismiss = viewModel::dismissGuessResult,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension on ViewModel for cancel
fun DrawingViewModel.endSession() {
    // Reset to idle
}
