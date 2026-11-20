package com.vitrum.tv.ui.glass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vitrum.tv.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

data class BlobSpec(
    val argb: Long,
    val x: Float,
    val y: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phase: Float,
    val alpha: Float = 0.55f,
)

data class BackgroundPreset(
    val id: String,
    val labelRes: Int,
    val topArgb: Long,
    val bottomArgb: Long,
    val blobs: List<BlobSpec>,
)

val BackgroundPresets = listOf(
    BackgroundPreset(
        id = "aurora",
        labelRes = R.string.background_aurora,
        topArgb = 0xFF050914,
        bottomArgb = 0xFF0A1B2C,
        blobs = listOf(
            BlobSpec(0xFF2A9CF0, 0.22f, 0.16f, 0.62f, 0.10f, 0.07f, 0.0f),
            BlobSpec(0xFF8A5CFF, 0.82f, 0.22f, 0.50f, 0.08f, 0.10f, 0.35f),
            BlobSpec(0xFF1FE0C0, 0.60f, 0.92f, 0.58f, 0.12f, 0.06f, 0.68f),
        ),
    ),
    BackgroundPreset(
        id = "dusk",
        labelRes = R.string.background_dusk,
        topArgb = 0xFF120711,
        bottomArgb = 0xFF1D0B1A,
        blobs = listOf(
            BlobSpec(0xFFFF6B9D, 0.78f, 0.20f, 0.55f, 0.09f, 0.08f, 0.10f),
            BlobSpec(0xFFFFB86B, 0.18f, 0.78f, 0.52f, 0.10f, 0.07f, 0.48f),
            BlobSpec(0xFF7A4DFF, 0.42f, 0.35f, 0.62f, 0.12f, 0.09f, 0.72f),
        ),
    ),
    BackgroundPreset(
        id = "mint",
        labelRes = R.string.background_mint,
        topArgb = 0xFF04100F,
        bottomArgb = 0xFF071F1C,
        blobs = listOf(
            BlobSpec(0xFF4CFFC2, 0.30f, 0.30f, 0.55f, 0.10f, 0.09f, 0.20f),
            BlobSpec(0xFF2DA0FF, 0.85f, 0.70f, 0.52f, 0.08f, 0.08f, 0.55f),
            BlobSpec(0xFFB6FF7A, 0.55f, 0.95f, 0.42f, 0.11f, 0.05f, 0.80f, 0.38f),
        ),
    ),
    BackgroundPreset(
        id = "ember",
        labelRes = R.string.background_ember,
        topArgb = 0xFF120705,
        bottomArgb = 0xFF1E0C07,
        blobs = listOf(
            BlobSpec(0xFFFF7A3D, 0.25f, 0.78f, 0.60f, 0.09f, 0.08f, 0.0f),
            BlobSpec(0xFFFFB03A, 0.80f, 0.35f, 0.46f, 0.10f, 0.06f, 0.40f),
            BlobSpec(0xFFC1264E, 0.55f, 0.15f, 0.50f, 0.08f, 0.10f, 0.75f),
        ),
    ),
    BackgroundPreset(
        id = "ocean",
        labelRes = R.string.background_ocean,
        topArgb = 0xFF020A18,
        bottomArgb = 0xFF051A33,
        blobs = listOf(
            BlobSpec(0xFF1E6BFF, 0.15f, 0.25f, 0.65f, 0.10f, 0.08f, 0.15f),
            BlobSpec(0xFF00C2FF, 0.75f, 0.65f, 0.55f, 0.09f, 0.09f, 0.52f),
            BlobSpec(0xFF4B3FFF, 0.50f, 0.95f, 0.48f, 0.12f, 0.05f, 0.85f),
        ),
    ),
    BackgroundPreset(
        id = "mono",
        labelRes = R.string.background_mono,
        topArgb = 0xFF0A0B0D,
        bottomArgb = 0xFF16181C,
        blobs = listOf(
            BlobSpec(0xFF9AA6BC, 0.25f, 0.20f, 0.55f, 0.08f, 0.07f, 0.10f, 0.22f),
            BlobSpec(0xFFD6DFEE, 0.80f, 0.55f, 0.45f, 0.10f, 0.08f, 0.45f, 0.16f),
            BlobSpec(0xFF566175, 0.55f, 0.92f, 0.60f, 0.10f, 0.05f, 0.75f, 0.28f),
        ),
    ),
)

fun presetById(id: String?): BackgroundPreset =
    BackgroundPresets.firstOrNull { it.id == id } ?: BackgroundPresets.first()

/** Draws the animated mesh or the user image. Phase is read during draw only. */
@Composable
fun LiquidBackground(
    preset: BackgroundPreset,
    phase: State<Float>,
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(preset.topArgb), Color(preset.bottomArgb)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        if (image != null) {
            drawBackgroundImage(image)
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.50f)),
                ),
            )
            return@Canvas
        }
        val minSide = minOf(size.width, size.height)
        val currentPhase = phase.value
        for (blob in preset.blobs) {
            val angle = (currentPhase + blob.phase) * 2f * PI.toFloat()
            val center = Offset(
                x = (blob.x + cos(angle) * blob.driftX) * size.width,
                y = (blob.y + sin(angle) * blob.driftY) * size.height,
            )
            val radius = (blob.radius * minSide).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(blob.argb).copy(alpha = blob.alpha),
                        Color(blob.argb).copy(alpha = blob.alpha * 0.30f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        // Soft vignette keeps glass text readable at the edges.
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = max(size.width, size.height) * 0.72f,
            ),
        )
    }
}

private fun DrawScope.drawBackgroundImage(image: ImageBitmap) {
    val scale = max(size.width / image.width, size.height / image.height)
    val srcWidth = (size.width / scale).roundToInt().coerceIn(1, image.width)
    val srcHeight = (size.height / scale).roundToInt().coerceIn(1, image.height)
    drawImage(
        image = image,
        srcOffset = IntOffset((image.width - srcWidth) / 2, (image.height - srcHeight) / 2),
        srcSize = IntSize(srcWidth, srcHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.Medium,
    )
}

object BackgroundRenderer {

    /** Renders a still frame used for palette extraction and blur fallback. */
    fun renderPreset(preset: BackgroundPreset, width: Int, height: Int): Bitmap {
        val safeWidth = width.coerceAtLeast(2)
        val safeHeight = height.coerceAtLeast(2)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                safeWidth.toFloat(),
                safeHeight.toFloat(),
                preset.topArgb.toInt(),
                preset.bottomArgb.toInt(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat(), backgroundPaint)
        val minSide = minOf(safeWidth, safeHeight).toFloat()
        for (blob in preset.blobs) {
            val centerX = blob.x * safeWidth
            val centerY = blob.y * safeHeight
            val radius = (blob.radius * minSide).coerceAtLeast(1f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centerX,
                    centerY,
                    radius,
                    intArrayOf(
                        withAlpha(blob.argb, blob.alpha),
                        withAlpha(blob.argb, blob.alpha * 0.30f),
                        withAlpha(blob.argb, 0f),
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
        return bitmap
    }

    private fun withAlpha(argb: Long, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (a shl 24) or (argb.toInt() and 0x00FFFFFF)
    }
}

object BlurEngine {

    /**
     * Downscales to ~1/8 of the screen, blurs three times and returns a small
     * bitmap cached by the caller. Cost is O(screen pixels / 64), not per frame.
     */
    fun blurredBackground(source: Bitmap, screenWidth: Int, screenHeight: Int, strength: Int): Bitmap {
        val targetWidth = (screenWidth / 8).coerceIn(48, 640)
        val targetHeight = (screenHeight / 8).coerceIn(27, 360)
        val small = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(small)
        canvas.drawBitmap(
            source,
            null,
            android.graphics.Rect(0, 0, targetWidth, targetHeight),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        val radius = (strength.coerceIn(0, 100) / 12 + 1).coerceIn(1, 9)
        return boxBlur(small, radius)
    }

    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        val width = source.width
        val height = source.height
        var pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(3) {
            pixels = blurPass(pixels, width, height, radius)
        }
        source.recycle()
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun blurPass(input: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val horizontal = IntArray(input.size)
        val output = IntArray(input.size)
        val window = radius * 2 + 1
        for (y in 0 until height) {
            val row = y * width
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (i in -radius..radius) {
                val color = input[row + i.coerceIn(0, width - 1)]
                sumR += (color shr 16) and 0xFF
                sumG += (color shr 8) and 0xFF
                sumB += color and 0xFF
            }
            for (x in 0 until width) {
                horizontal[row + x] = (0xFF shl 24) or
                    ((sumR / window) shl 16) or
                    ((sumG / window) shl 8) or
                    (sumB / window)
                val add = input[row + (x + radius + 1).coerceIn(0, width - 1)]
                val remove = input[row + (x - radius).coerceIn(0, width - 1)]
                sumR += ((add shr 16) and 0xFF) - ((remove shr 16) and 0xFF)
                sumG += ((add shr 8) and 0xFF) - ((remove shr 8) and 0xFF)
                sumB += (add and 0xFF) - (remove and 0xFF)
            }
        }
        for (x in 0 until width) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (i in -radius..radius) {
                val color = horizontal[i.coerceIn(0, height - 1) * width + x]
                sumR += (color shr 16) and 0xFF
                sumG += (color shr 8) and 0xFF
                sumB += color and 0xFF
            }
            for (y in 0 until height) {
                output[y * width + x] = (0xFF shl 24) or
                    ((sumR / window) shl 16) or
                    ((sumG / window) shl 8) or
                    (sumB / window)
                val add = horizontal[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                val remove = horizontal[(y - radius).coerceIn(0, height - 1) * width + x]
                sumR += ((add shr 16) and 0xFF) - ((remove shr 16) and 0xFF)
                sumG += ((add shr 8) and 0xFF) - ((remove shr 8) and 0xFF)
                sumB += (add and 0xFF) - (remove and 0xFF)
            }
        }
        return output
    }
}
