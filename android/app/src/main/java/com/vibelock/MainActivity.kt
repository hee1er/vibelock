package com.vibelock

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibelock.ads.AdManager
import com.vibelock.billing.BillingManager
import com.vibelock.data.model.MatchState
import com.vibelock.service.VibeLockService
import com.vibelock.ui.screen.*
import com.vibelock.ui.theme.VibeLockTheme
import com.vibelock.ui.theme.VibeSurface
import com.vibelock.ui.viewmodel.DrawingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Matching : Screen("matching")
    data object Drawing : Screen("drawing")
    data object Store : Screen("store")
    data object Premium : Screen("premium")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // AdMob 초기화
        adManager.initialize()

        // Billing 초기화 (미결제 구독 확인)
        billingManager.connect()

        setContent {
            VibeLockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VibeSurface) {
                    VibeLockApp()
                }
            }
        }
    }
}

@Composable
fun VibeLockApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val drawingViewModel: DrawingViewModel = hiltViewModel()
    val uiState by drawingViewModel.uiState.collectAsStateWithLifecycle()

    // 매칭 상태에 따른 자동 네비게이션
    LaunchedEffect(uiState.matchState) {
        when (uiState.matchState) {
            is MatchState.Idle, is MatchState.Disconnected ->
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            is MatchState.Searching ->
                navController.navigate(Screen.Matching.route) {
                    popUpTo(Screen.Home.route)
                }
            is MatchState.Matched ->
                navController.navigate(Screen.Drawing.route) {
                    popUpTo(Screen.Home.route)
                }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                userState = uiState.userState,
                onFindMatch = { drawingViewModel.findMatch() },
                onGoStore = { navController.navigate(Screen.Store.route) },
                onGoPremium = { navController.navigate(Screen.Premium.route) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Screen.Matching.route) {
            MatchingScreen(
                queueSize = (uiState.matchState as? MatchState.Searching)?.queueSize ?: 1,
                onCancel = { drawingViewModel.resetToIdle() },
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
                    selectedBrush = uiState.selectedBrush,
                    guessInput = uiState.guessInput,
                    guessResult = uiState.guessResult,
                    showSkipConfirm = uiState.showSkipConfirm,
                    skipLimitReached = uiState.skipLimitReached,
                    isPremium = uiState.userState?.isPremium ?: false,
                    unlockedItems = uiState.userState?.unlockedItemIds ?: emptySet(),
                    toast = uiState.toast,
                    onDrawStart = drawingViewModel::onDrawStart,
                    onDrawMove = drawingViewModel::onDrawMove,
                    onDrawEnd = drawingViewModel::onDrawEnd,
                    onClear = drawingViewModel::clearCanvas,
                    onColorSelected = drawingViewModel::selectColor,
                    onStrokeSelected = drawingViewModel::selectStroke,
                    onBrushSelected = { brush -> drawingViewModel.selectBrush(brush) },
                    onGuessInput = drawingViewModel::onGuessInput,
                    onGuessSubmit = drawingViewModel::submitGuess,
                    onSkipRequest = { activity -> drawingViewModel.requestSkip(activity) },
                    onSkipConfirm = drawingViewModel::dismissSkipConfirm,
                    onSkipDismiss = drawingViewModel::dismissSkipConfirm,
                    onGuessResultDismiss = drawingViewModel::dismissGuessResult,
                    onSkipLimitWatchAd = { activity -> drawingViewModel.watchAdForSkip(activity) },
                    onSkipLimitGoPremium = { navController.navigate(Screen.Premium.route) },
                    onSkipLimitDismiss = drawingViewModel::dismissSkipLimit,
                    onDismissToast = drawingViewModel::dismissToast,
                    onGoStore = { navController.navigate(Screen.Store.route) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(Screen.Store.route) {
            StoreScreen(
                onBack = { navController.popBackStack() },
                onGoPremium = { navController.navigate(Screen.Premium.route) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onBack = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
