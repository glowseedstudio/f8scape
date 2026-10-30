package com.f8scape.app

import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.f8scape.app.navigation.Screen
import com.f8scape.app.theme.F8scapeTheme
import com.f8scape.app.theme.PureBlack
import com.f8scape.dashboard.StandbyPreviewScreen
import com.f8scape.data.PreferencesRepository
import com.f8scape.diagnostics.DiagnosticsScreen
import com.f8scape.diagnostics.DiagnosticsViewModel
import com.f8scape.fold.WindowPostureSource
import com.f8scape.settings.SettingsScreen
import com.f8scape.util.F8Log

class MainActivity : ComponentActivity() {

    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private lateinit var windowPostureSource: WindowPostureSource
    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository = PreferencesRepository(this)

        // Edge-to-edge ambient display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Keep screen on while testing diagnostics / preview
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

        setContent {
            F8scapeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Diagnostics.route
                    ) {
                        composable(Screen.Diagnostics.route) {
                            DiagnosticsScreen(
                                viewModel = diagnosticsViewModel,
                                onNavigateToPreview = {
                                    navController.navigate(Screen.StandbyPreview.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
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

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                preferencesRepository = preferencesRepository,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
