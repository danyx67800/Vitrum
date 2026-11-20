package com.vitrum.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.vitrum.tv.R
import com.vitrum.tv.data.AnimationMode
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.TextSizeOption
import com.vitrum.tv.data.ThemeMode

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

@Immutable
data class VitrumColors(
    val isLight: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val glassTint: Color,
    val glassTintStrong: Color,
    val panelScrim: Color,
    val borderStart: Color,
    val borderEnd: Color,
)

@Immutable
data class VitrumType(
    val display: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.4).sp,
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.2).sp,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 27.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
)

val LocalVitrumColors = staticCompositionLocalOf {
    VitrumColors(
        isLight = false,
        textPrimary = Color(0xFFF2F7FF),
        textSecondary = Color(0xFFBAC7DB),
        accent = Color(0xFF7FD8FF),
        glassTint = Color.White.copy(alpha = 0.12f),
        glassTintStrong = Color.White.copy(alpha = 0.18f),
        panelScrim = Color(0xFF060A12).copy(alpha = 0.20f),
        borderStart = Color.White.copy(alpha = 0.55f),
        borderEnd = Color.White.copy(alpha = 0.08f),
    )
}

val LocalVitrumType = staticCompositionLocalOf { VitrumType() }
val LocalMotionReduced = staticCompositionLocalOf { false }
val LocalAnimationsOff = staticCompositionLocalOf { false }
val LocalNavSounds = staticCompositionLocalOf { false }

object VitrumThemeDefaults {
    val SmallText = 0.9f
    val LargeText = 1.14f
}

@Composable
fun VitrumTheme(
    settings: Settings,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val tintAlpha = (settings.glassOpacity / 100f).coerceIn(0.06f, 0.3f)
    val textFactor = when (settings.textSize) {
        TextSizeOption.SMALL -> VitrumThemeDefaults.SmallText
        TextSizeOption.NORMAL -> 1f
        TextSizeOption.LARGE -> VitrumThemeDefaults.LargeText
    }
    val type = VitrumType().let { base ->
        base.copy(
            display = base.display.copy(fontSize = 44.sp * textFactor, lineHeight = 50.sp * textFactor),
            headline = base.headline.copy(fontSize = 32.sp * textFactor, lineHeight = 38.sp * textFactor),
            title = base.title.copy(fontSize = 24.sp * textFactor, lineHeight = 30.sp * textFactor),
            body = base.body.copy(fontSize = 19.sp * textFactor, lineHeight = 27.sp * textFactor),
            label = base.label.copy(fontSize = 18.sp * textFactor, lineHeight = 23.sp * textFactor),
            caption = base.caption.copy(fontSize = 16.sp * textFactor, lineHeight = 22.sp * textFactor),
        )
    }
    val colors = if (dark) {
        VitrumColors(
            isLight = false,
            textPrimary = Color(0xFFF4F8FF),
            textSecondary = Color(0xFFBECBDD),
            accent = accent,
            glassTint = Color.White.copy(alpha = tintAlpha),
            glassTintStrong = Color.White.copy(alpha = (tintAlpha + 0.06f).coerceAtMost(0.34f)),
            panelScrim = Color(0xFF050810).copy(alpha = 0.24f),
            borderStart = Color.White.copy(alpha = 0.58f),
            borderEnd = Color.White.copy(alpha = 0.07f),
        )
    } else {
        VitrumColors(
            isLight = true,
            textPrimary = Color(0xFF0B1727),
            textSecondary = Color(0xFF33465E),
            accent = accent,
            glassTint = Color.White.copy(alpha = 0.58f),
            glassTintStrong = Color.White.copy(alpha = 0.74f),
            panelScrim = Color.White.copy(alpha = 0.16f),
            borderStart = Color.White.copy(alpha = 0.92f),
            borderEnd = Color.White.copy(alpha = 0.22f),
        )
    }
    CompositionLocalProvider(
        LocalVitrumColors provides colors,
        LocalVitrumType provides type,
        LocalMotionReduced provides (settings.animations == AnimationMode.REDUCED),
        LocalAnimationsOff provides (settings.animations == AnimationMode.OFF),
        LocalNavSounds provides settings.soundsEnabled,
        content = content,
    )
}

/** Shared ellipsis helper for TV labels. */
val TvLabelOverflow: TextOverflow = TextOverflow.Ellipsis
