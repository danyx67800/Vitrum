package com.vitrum.tv.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

data class Artwork(val bitmap: Bitmap, val isBanner: Boolean)

/**
 * Loads leanback banners when available and app icons otherwise. Bitmaps are
 * cached by package, style and target size so we never keep 4K artwork in RAM.
 */
class AppIconLoader(private val context: Context) {

    private val packageManager = context.packageManager
    private val maxCacheBytes = run {
        val heap = Runtime.getRuntime().maxMemory()
        (heap / 16L).coerceIn(4L * 1024 * 1024, 20L * 1024 * 1024).toInt()
    }

    private val cache = object : LruCache<String, Artwork>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Artwork): Int = value.bitmap.byteCount
    }

    suspend fun load(
        app: AppInfo,
        wantBanner: Boolean,
        widthPx: Int,
        heightPx: Int,
    ): Artwork? = withContext(Dispatchers.IO) {
        val width = widthPx.coerceIn(48, 1600)
        val height = heightPx.coerceIn(48, 900)
        val key = "${app.packageName}/${app.activityName}/$wantBanner/$width/$height"
        cache.get(key)?.let { return@withContext it }
        val artwork = try {
            if (wantBanner) {
                loadBanner(app, width, height) ?: loadIcon(app, min(width, height))
            } else {
                loadIcon(app, min(width, height))
            }
        } catch (_: Throwable) {
            null
        }
        if (artwork != null) cache.put(key, artwork)
        artwork
    }

    private fun loadBanner(app: AppInfo, width: Int, height: Int): Artwork? {
        val activityInfo = try {
            packageManager.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
        } catch (_: Exception) {
            null
        }
        val activityBanner = try {
            activityInfo?.loadBanner(packageManager)
        } catch (_: Exception) {
            null
        }
        val appBanner = if (activityBanner == null) {
            try {
                packageManager.getApplicationInfo(app.packageName, 0).loadBanner(packageManager)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        val banner = activityBanner ?: appBanner ?: return null
        return Artwork(render(banner, width, height, centerCrop = true), isBanner = true)
    }

    private fun loadIcon(app: AppInfo, sizePx: Int): Artwork? {
        val drawable = try {
            packageManager.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
                .loadIcon(packageManager)
        } catch (_: Exception) {
            null
        } ?: try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        } ?: return null
        return Artwork(render(drawable, sizePx, sizePx, centerCrop = false), isBanner = false)
    }

    private fun render(drawable: Drawable, width: Int, height: Int, centerCrop: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(
            max(1, width),
            max(1, height),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val intrinsicWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width
        val intrinsicHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height
        val scale = if (centerCrop) {
            max(width.toFloat() / intrinsicWidth, height.toFloat() / intrinsicHeight)
        } else {
            min(width.toFloat() / intrinsicWidth, height.toFloat() / intrinsicHeight)
        }
        val drawWidth = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
        val drawHeight = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
        val left = (width - drawWidth) / 2
        val top = (height - drawHeight) / 2
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawable.setTintList(null)
        }
        drawable.draw(canvas)
        return bitmap
    }

    fun clear() {
        cache.evictAll()
    }

    fun approximateSize(): Int = cache.size()
}
