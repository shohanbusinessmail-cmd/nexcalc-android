package com.shohankhan.nexcalc

import android.app.Application
import com.shohankhan.nexcalc.core.data.HistoryCleanupScheduler
import com.shohankhan.nexcalc.di.AppContainer

class NexCalcApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        HistoryCleanupScheduler.schedule(this)
        container.startMaintenance()
    }
}
