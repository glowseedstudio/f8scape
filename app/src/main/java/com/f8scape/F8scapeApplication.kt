package com.f8scape

import android.app.Application
import com.f8scape.service.StandbyAutomationCoordinator
import com.f8scape.util.F8Log

class F8scapeApplication : Application() {

    lateinit var automationCoordinator: StandbyAutomationCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        F8Log.i("Application", "F8scape initialized")
        automationCoordinator = StandbyAutomationCoordinator(this)
        automationCoordinator.start()
    }
}
