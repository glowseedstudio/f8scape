package com.f8scape.app.navigation

sealed class Screen(val route: String) {
    object Diagnostics : Screen("diagnostics")
    object StandbyPreview : Screen("standby_preview")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
}
