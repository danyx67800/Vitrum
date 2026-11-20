package com.vitrum.tv.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ThemeMode { DARK, LIGHT, AUTO }

@Serializable
enum class AccentMode { CUSTOM, DYNAMIC }

@Serializable
enum class AnimationMode { NORMAL, REDUCED, OFF }

@Serializable
enum class EffectsQuality { AUTO, HIGH, MEDIUM, LOW }

@Serializable
enum class TextSizeOption { SMALL, NORMAL, LARGE }

@Serializable
enum class TileSize { SMALL, MEDIUM, LARGE }

@Serializable
enum class TileStyle { BANNER, ICON, ICON_ONLY }

@Serializable
enum class SortOrder { AZ, RECENT, MANUAL }

@Serializable
enum class HomeBehavior { TOP, KEEP }

@Serializable
enum class UpdateFrequency { NEVER, ON_START, DAILY, WEEKLY }

@Serializable
enum class UpdateChannel { STABLE, BETA }

@Serializable
enum class RowKind { FAVORITES, RECENT, ALL_APPS, SHORTCUTS }

@Serializable
data class CustomRow(
    val id: String,
    val name: String,
    val packages: List<String> = emptyList(),
)

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.DARK,
    val accentMode: AccentMode = AccentMode.CUSTOM,
    val accentArgb: Int = 0xFF7FD8FF.toInt(),
    val backgroundId: String = "aurora",
    val customBackgroundUrl: String = "",
    val blurStrength: Int = 70,
    val glassOpacity: Int = 12,
    val cornerRadius: Int = 28,
    val animations: AnimationMode = AnimationMode.NORMAL,
    val effectsQuality: EffectsQuality = EffectsQuality.AUTO,
    val textSize: TextSizeOption = TextSizeOption.NORMAL,
    val tileSize: TileSize = TileSize.MEDIUM,
    val tileStyle: TileStyle = TileStyle.BANNER,
    val visibleRows: Int = 3,
    val rowOrder: List<RowKind> = listOf(
        RowKind.FAVORITES,
        RowKind.RECENT,
        RowKind.ALL_APPS,
        RowKind.SHORTCUTS,
    ),
    val showAppNames: Boolean = true,
    val uiScale: Float = 1.0f,
    val overscanMargin: Int = 0,
    val favorites: List<String> = emptyList(),
    val hiddenApps: List<String> = emptyList(),
    val customRows: List<CustomRow> = emptyList(),
    val manualOrder: List<String> = emptyList(),
    val sortOrder: SortOrder = SortOrder.AZ,
    val showSystemApps: Boolean = false,
    val clock24h: Boolean = true,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val weatherEnabled: Boolean = false,
    val weatherCity: String = "",
    val weatherLat: Double? = null,
    val weatherLon: Double? = null,
    val useCelsius: Boolean = true,
    val homeBehavior: HomeBehavior = HomeBehavior.TOP,
    val soundsEnabled: Boolean = false,
    val confirmUninstall: Boolean = true,
    val autoUpdate: UpdateFrequency = UpdateFrequency.WEEKLY,
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
    val wifiOnlyUpdates: Boolean = true,
    val recentApps: List<String> = emptyList(),
    val onboardingDone: Boolean = false,
) {
    fun cleaned(): Settings = copy(
        blurStrength = blurStrength.coerceIn(0, 100),
        glassOpacity = glassOpacity.coerceIn(0, 30),
        cornerRadius = cornerRadius.coerceIn(0, 48),
        visibleRows = visibleRows.coerceIn(1, 6),
        uiScale = uiScale.coerceIn(0.8f, 1.3f),
        overscanMargin = overscanMargin.coerceIn(0, 10),
        recentApps = recentApps.take(20),
        weatherLat = weatherLat?.takeIf { it in -90.0..90.0 },
        weatherLon = weatherLon?.takeIf { it in -180.0..180.0 },
    )
}

object SettingsCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun encode(settings: Settings): String = json.encodeToString(Settings.serializer(), settings)

    fun decode(raw: String?): Settings? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(Settings.serializer(), raw) }
            .getOrNull()
            ?.cleaned()
    }
}

/** Accent palette offered in appearance settings and onboarding. */
data class AccentChoice(val name: String, val argb: Int)

val AccentPalette = listOf(
    AccentChoice("cyan", 0xFF7FD8FF.toInt()),
    AccentChoice("violet", 0xFFB79CFF.toInt()),
    AccentChoice("mint", 0xFF7BF0C4.toInt()),
    AccentChoice("amber", 0xFFFFCF7A.toInt()),
    AccentChoice("rose", 0xFFFF9EB8.toInt()),
    AccentChoice("blue", 0xFF8FB4FF.toInt()),
)
