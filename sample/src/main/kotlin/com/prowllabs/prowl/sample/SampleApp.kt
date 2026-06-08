package com.prowllabs.prowl.sample

import android.app.Application
import com.prowllabs.prowl.Prowl

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prowl.start(this)
    }
}
