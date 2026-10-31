package com.f8scape.app.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object About : Screen("about")
    object Diagnostics : Screen("diagnostics")
    object StandbyPreview : Screen("standby_preview?initialPage={initialPage}") {
        fun createRoute(initialPage: String? = null): String =
            if (initialPage != null) "standby_preview?initialPage=$initialPage" else "standby_preview"
    }
    object Onboarding : Screen("onboarding")
    object PhotoCollection : Screen("photo_collection")
}
