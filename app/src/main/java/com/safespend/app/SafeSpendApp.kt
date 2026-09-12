package com.safespend.app

import android.app.Application
import com.safespend.app.di.AppContainer

class SafeSpendApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
