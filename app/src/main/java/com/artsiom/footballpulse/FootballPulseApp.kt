package com.artsiom.footballpulse

import android.app.Application

class FootballPulseApp : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
