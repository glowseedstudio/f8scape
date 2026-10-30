package com.f8scape.app

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.f8scape.app.navigation.Screen
import com.f8scape.app.theme.AccentBlue
import com.f8scape.app.theme.F8scapeTheme
import com.f8scape.app.theme.PureBlack
import com.f8scape.app.theme.TextSecondary
import com.f8scape.dashboard.StandbyPreviewScreen
import com.f8scape.data.PreferencesRepository
import com.f8scape.diagnostics.DiagnosticsScreen
import com.f8scape.diagnostics.DiagnosticsViewModel
import com.f8scape.fold.WindowPostureSource
import com.f8scape.onboarding.FirstOpenSetupScreen
import com.f8scape.service.CoverOverlayService
import com.f8scape.settings.SettingsScreen
import com.f8scape.util.F8Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private lateinit var windowPostureSource: WindowPostureSource
    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository = PreferencesRepository(this)

        // Edge-to-edge ambient display + tapjacking protection on configuration UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.filterTouchesWhenObscured = true

        // Keep screen on while testing / previewing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        windowPostureSource = WindowPostureSource(this)

        val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.displayId ?: Display.DEFAULT_DISPLAY
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.displayId
        }

        diagnosticsViewModel.updateCurrentDisplayId(currentDisplayId)
        F8Log.i("MainActivity", "Activity created on display id: $currentDisplayId")

        val initialPrefs = runBlocking { preferencesRepository.userPreferencesFlow.first() }
        val coreReady = CoverOverlayService.isServiceRunning && Settings.canDrawOverlays(this)
        val initialRoute = if (!initialPrefs.hasCompletedOnboarding && !coreReady) {
            Screen.Onboarding.route
        } else {
            Screen.Home.route
        }

        setContent {
            F8scapeTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomRail = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Settings.route,
                    Screen.About.route
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = initialRoute
                        ) {
                            composable(Screen.Onboarding.route) {
                                FirstOpenSetupScreen(
                                    onSetupComplete = {
                                        coroutineScope.launch {
                                            preferencesRepository.setHasCompletedOnboarding(true)
                                        }
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Home.route) {
                                AppHomeScreen(
                                    preferencesRepository = preferencesRepository,
                                    onLaunchPreview = {
                                        navController.navigate(Screen.StandbyPreview.route)
                                    },
                                    onOpenSettings = {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenSetup = {
                                        navController.navigate(Screen.Onboarding.route)
                                    }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    preferencesRepository = preferencesRepository,
                                    onNavigateBack = null,
                                    onOpenDiagnostics = {
                                        navController.navigate(Screen.Diagnostics.route)
                                    }
                                )
                            }

                            composable(Screen.About.route) {
                                AboutScreen()
                            }

                            composable(Screen.Diagnostics.route) {
                                DiagnosticsScreen(
                                    viewModel = diagnosticsViewModel,
                                    onNavigateToPreview = {
                                        navController.navigate(Screen.StandbyPreview.route)
                                    },
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(Screen.StandbyPreview.route) {
                                StandbyPreviewScreen(
                                    onDismiss = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        if (showBottomRail) {
                            F8BottomNavigationRail(
                                currentRoute = currentRoute ?: Screen.Home.route,
                                onSelectRoute = { route ->
                                    if (route != currentRoute) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RailTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun F8BottomNavigationRail(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            RailTab(Screen.Home.route, "Home", Icons.Default.Home),
            RailTab(Screen.Settings.route, "Settings", Icons.Default.Settings),
            RailTab(Screen.About.route, "About", Icons.Default.Info)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF141417).copy(alpha = 0.96f),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route
                Surface(
                    onClick = { onSelectRoute(tab.route) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) AccentBlue else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = tab.label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
