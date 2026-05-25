package com.edu.gymledger

import android.app.Application
import com.edu.gymledger.app.AppContainer

class GymLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}