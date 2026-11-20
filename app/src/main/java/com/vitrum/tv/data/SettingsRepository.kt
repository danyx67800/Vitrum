package com.vitrum.tv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vitrumDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vitrum_settings",
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.vitrumDataStore

    val settings: Flow<Settings> = dataStore.data
        .map { prefs -> SettingsCodec.decode(prefs[KEY_SETTINGS]) ?: Settings() }
        .distinctUntilChanged()

    suspend fun current(): Settings = settings.first()

    suspend fun save(settings: Settings) {
        val encoded = SettingsCodec.encode(settings.cleaned())
        dataStore.edit { prefs -> prefs[KEY_SETTINGS] = encoded }
    }

    suspend fun update(transform: (Settings) -> Settings) {
        save(transform(current()))
    }

    suspend fun reset() {
        val onboarding = current().onboardingDone
        save(Settings(onboardingDone = onboarding))
    }

    suspend fun setOnboardingDone(done: Boolean = true) {
        update { it.copy(onboardingDone = done) }
    }

    private companion object {
        val KEY_SETTINGS = stringPreferencesKey("settings_json_v1")
    }
}
