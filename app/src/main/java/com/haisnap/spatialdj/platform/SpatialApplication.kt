package com.haisnap.spatialdj.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.haisnap.spatialdj.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
