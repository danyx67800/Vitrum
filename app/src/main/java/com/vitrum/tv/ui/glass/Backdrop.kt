package com.vitrum.tv.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.vitrum.tv.data.AnimationMode
import com.vitrum.tv.data.BackgroundRepository
import com.vitrum.tv.data.EffectsQuality
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class BackdropMode { REAL, BITMAP, NONE }

data class GlassBackdrop(
    val mode: BackdropMode,
    val preset: BackgroundPreset,
    val image: ImageBitmap?,
    val blurred: ImageBitmap?,
    val phase: androidx.compose.runtime.State<Float>,
    val screenSize: IntSize,
    val blurRadiusDp: Float,
    val accent: Color,
)

data class GlassStyle(
    val cornerRadiusDp: Int = 28,
    val blurRadiusDp: Float = 24f,
)

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop> {
    GlassBackdrop(
        mode = BackdropMode.NONE,
        preset = BackgroundPresets.first(),
        image = null,
        blurred = null,
        phase = androidx.compose.runtime.mutableFloatStateOf(0.2f),
        screenSize = IntSize(1920, 1080),
        blurRadiusDp = 24f,
        accent = Color(0xFF7FD8FF),
    )
}

val LocalGlassStyle = staticCompositionLocalOf { GlassStyle() }

fun resolvedQuality(context: Context, quality: EffectsQuality): EffectsQuality {
    if (quality != EffectsQuality.AUTO) return quality
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val lowRam = activityManager?.isLowRamDevice == true ||
        (activityManager?.memoryClass ?: 256) < 160
    return when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> EffectsQuality.MEDIUM
        lowRam -> EffectsQuality.MEDIUM
        else -> EffectsQuality.HIGH
    }
}

@Composable
fun rememberGlassBackdrop(settings: Settings): GlassBackdrop {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val screenSize = remember(metrics.widthPixels, metrics.heightPixels) {
        IntSize(metrics.widthPixels, metrics.heightPixels)
    }
    val useCustom = settings.backgroundId == "custom" &&
        settings.customBackgroundUrl.startsWith("https://", ignoreCase = true)
    val customUrl = if (useCustom) settings.customBackgroundUrl else null
    val preset = remember(settings.backgroundId) { presetById(settings.backgroundId) }

    val customImage by produceState<ImageBitmap?>(initialValue = null, customUrl, screenSize) {
        value = if (customUrl == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                BackgroundRepository(context.applicationContext)
                    .load(
                        customUrl,
                        maxWidth = max(1280, screenSize.width.coerceAtMost(1920)),
                        maxHeight = screenSize.height,
                    )
                    ?.asImageBitmap()
            }
        }
    }

    val quality = remember(settings.effectsQuality) { resolvedQuality(context, settings.effectsQuality) }
    val blurred by produceState<ImageBitmap?>(
        initialValue = null,
        preset.id,
        customImage,
        screenSize,
        settings.blurStrength,
        quality,
    ) {
        value = withContext(Dispatchers.Default) {
            val source = customImage?.asAndroidBitmap()
                ?: BackgroundRenderer.renderPreset(preset, 480, 270)
            BlurEngine.blurredBackground(
                source = source,
                screenWidth = screenSize.width,
                screenHeight = screenSize.height,
                strength = settings.blurStrength,
            ).asImageBitmap()
        }
    }

    val accentIsLight = settings.theme == ThemeMode.LIGHT
    var dynamicAccent by remember { mutableStateOf(Color(settings.accentArgb)) }
    LaunchedEffect(settings.accentMode, blurred, accentIsLight) {
        if (settings.accentMode != com.vitrum.tv.data.AccentMode.DYNAMIC) return@LaunchedEffect
        val image = blurred ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) {
            Palette.from(image.asAndroidBitmap()).clearFilters().generate()
        }
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        if (swatch != null) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            hsl[1] = max(hsl[1], 0.45f)
            hsl[2] = if (accentIsLight) 0.36f else 0.72f
            dynamicAccent = Color(ColorUtils.HSLToColor(hsl))
        }
    }
    val accent = if (settings.accentMode == com.vitrum.tv.data.AccentMode.DYNAMIC) {
        dynamicAccent
    } else {
        Color(settings.accentArgb)
    }

    val realBlur = quality == EffectsQuality.HIGH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val mode = when {
        quality == EffectsQuality.LOW || settings.blurStrength == 0 -> BackdropMode.NONE
        realBlur -> BackdropMode.REAL
        else -> BackdropMode.BITMAP
    }

    val phase = remember { mutableFloatStateOf(0.18f) }
    LaunchedEffect(settings.animations, quality) {
        if (settings.animations == AnimationMode.OFF) {
            phase.floatValue = 0.24f
            return@LaunchedEffect
        }
        var lastUpdate = 0L
        while (true) {
            withFrameMillis { now ->
                val throttled = quality == EffectsQuality.LOW && now - lastUpdate < 90
                if (!throttled) {
                    lastUpdate = now
                    phase.floatValue = ((now % 60_000L) / 60_000f)
                }
            }
        }
    }

    return remember(
        mode,
        preset.id,
        customImage,
        blurred,
        phase,
        screenSize,
        settings.blurStrength,
        accent,
    ) {
        GlassBackdrop(
            mode = mode,
            preset = preset,
            image = customImage,
            blurred = blurred,
            phase = phase,
            screenSize = screenSize,
            blurRadiusDp = (settings.blurStrength * 0.34f).coerceIn(0f, 34f),
            accent = accent,
        )
    }
}

@Composable
fun VitrumBackdropHost(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        androidx.compose.foundation.layout.Box(modifier) {
            LiquidBackground(
                preset = backdrop.preset,
                phase = backdrop.phase,
                image = backdrop.image,
                modifier = Modifier.matchParentSize(),
            )
            content()
        }
    }
}
