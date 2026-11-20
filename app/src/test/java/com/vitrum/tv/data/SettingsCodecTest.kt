package com.vitrum.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun roundTripsAllUserSettings() {
        val settings = Settings(
            theme = ThemeMode.LIGHT,
            accentMode = AccentMode.DYNAMIC,
            accentArgb = 0xFFB79CFF.toInt(),
            backgroundId = "custom",
            customBackgroundUrl = "https://example.com/bg.jpg",
            blurStrength = 30,
            glassOpacity = 18,
            cornerRadius = 32,
            animations = AnimationMode.REDUCED,
            effectsQuality = EffectsQuality.MEDIUM,
            textSize = TextSizeOption.LARGE,
            tileSize = TileSize.LARGE,
            tileStyle = TileStyle.ICON,
            visibleRows = 5,
            rowOrder = listOf(RowKind.RECENT, RowKind.FAVORITES, RowKind.ALL_APPS),
            showAppNames = false,
            uiScale = 1.15f,
            overscanMargin = 4,
            favorites = listOf("com.a", "com.b"),
            customLabels = mapOf("com.a" to "My App"),
            hiddenApps = listOf("com.c"),
            customRows = listOf(CustomRow("id1", "Kids", listOf("com.a"))),
            sortOrder = SortOrder.MANUAL,
            manualOrder = listOf("com.b", "com.a"),
            showSystemApps = true,
            clock24h = false,
            showSeconds = true,
            showDate = false,
            weatherEnabled = true,
            weatherCity = "Milan",
            weatherLat = 45.46,
            weatherLon = 9.19,
            useCelsius = false,
            homeBehavior = HomeBehavior.KEEP,
            soundsEnabled = true,
            confirmUninstall = false,
            autoUpdate = UpdateFrequency.DAILY,
            updateChannel = UpdateChannel.BETA,
            wifiOnlyUpdates = false,
            recentApps = listOf("com.a"),
            onboardingDone = true,
        )
        val decoded = SettingsCodec.decode(SettingsCodec.encode(settings))
        assertEquals(settings, decoded)
    }

    @Test
    fun corruptJsonFallsBackToNull() {
        assertNull(SettingsCodec.decode("{not json"))
        assertNull(SettingsCodec.decode(""))
        assertNull(SettingsCodec.decode(null))
    }

    @Test
    fun unknownKeysAreIgnoredForForwardCompatibility() {
        val json = """{"theme":"DARK","onboardingDone":true,"futureOption":42}"""
        val decoded = SettingsCodec.decode(json)
        assertNotNull(decoded)
        assertEquals(ThemeMode.DARK, decoded?.theme)
        assertTrue(decoded?.onboardingDone == true)
    }

    @Test
    fun valuesAreClampedOnDecode() {
        val json = """
            {"blurStrength":400,"glassOpacity":-5,"uiScale":9.0,"overscanMargin":80,"visibleRows":99}
        """.trimIndent()
        val decoded = SettingsCodec.decode(json)!!
        assertEquals(100, decoded.blurStrength)
        assertEquals(0, decoded.glassOpacity)
        assertEquals(1.3f, decoded.uiScale, 0.001f)
        assertEquals(10, decoded.overscanMargin)
        assertEquals(6, decoded.visibleRows)
    }

    @Test
    fun defaultsAreStable() {
        val decoded = SettingsCodec.decode("{}")!!
        assertEquals(ThemeMode.DARK, decoded.theme)
        assertEquals(28, decoded.cornerRadius)
        assertEquals(12, decoded.glassOpacity)
        assertEquals(UpdateChannel.STABLE, decoded.updateChannel)
        assertTrue(!decoded.onboardingDone)
    }
}
