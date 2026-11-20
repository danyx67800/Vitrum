package com.vitrum.tv

import android.app.Application
import com.vitrum.tv.data.AppIconLoader
import com.vitrum.tv.data.AppRepository
import com.vitrum.tv.data.SettingsRepository
import com.vitrum.tv.data.WeatherRepository

class VitrumApp : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val appRepository: AppRepository by lazy { AppRepository(this) }
    val weatherRepository: WeatherRepository by lazy { WeatherRepository() }
    val iconLoader: AppIconLoader by lazy { AppIconLoader(this) }

    override fun onCreate() {
        super.onCreate()
        appRepository.start()
    }
}
