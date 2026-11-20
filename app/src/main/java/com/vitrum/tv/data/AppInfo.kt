package com.vitrum.tv.data

/** Lightweight launchable app record; drawables are loaded lazily and cached. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val activityName: String,
    val isSystem: Boolean,
) {
    val stableKey: String get() = packageName
}
