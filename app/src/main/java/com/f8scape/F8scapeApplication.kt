package com.f8scape

import android.app.Application
import com.f8scape.util.F8Log

class F8scapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        F8Log.i("Application", "F8scape initialized")
    }
}
