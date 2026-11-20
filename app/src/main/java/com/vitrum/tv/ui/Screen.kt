package com.vitrum.tv.ui

import com.vitrum.tv.R

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data class AllApps(val focusSearch: Boolean = false) : Screen
    data class Settings(val section: SettingsSection = SettingsSection.APPEARANCE) : Screen
    data object LauncherGuide : Screen
}

enum class SettingsSection(val titleRes: Int) {
    APPEARANCE(R.string.cat_appearance),
    LAYOUT(R.string.cat_layout),
    APPS(R.string.cat_apps),
    CLOCK_WEATHER(R.string.cat_clock_weather),
    BEHAVIOR(R.string.cat_behavior),
    SYSTEM(R.string.cat_system),
    BACKUP(R.string.cat_backup),
    ABOUT(R.string.cat_about),
}
