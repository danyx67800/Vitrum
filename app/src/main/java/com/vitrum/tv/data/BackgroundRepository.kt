package com.vitrum.tv.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.vitrum.tv.core.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

class BackgroundRepository(private val context: Context) {

    /** Downloads at most once per URL, then decodes with inSampleSize. */
    suspend fun load(url: String, maxWidth: Int, maxHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) return@withContext null
        val file = cachedFile(trimmed)
        if (!file.exists()) {
            val downloaded = runCatching { download(trimmed, file) }.getOrDefault(false)
            if (!downloaded) return@withContext null
        }
        runCatching { decodeSampled(file, maxWidth, maxHeight) }.getOrNull()
    }

    private fun cachedFile(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(20)
        val dir = File(context.filesDir, "backgrounds")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$digest.img")
    }

    private fun download(url: String, destination: File): Boolean {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Http.USER_AGENT)
            .build()
        return try {
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                if (body.contentLength() > MAX_BYTES) return false
                val temp = File(destination.parentFile, destination.name + ".tmp")
                temp.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > MAX_BYTES) {
                                temp.delete()
                                return false
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                temp.renameTo(destination)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun decodeSampled(file: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxWidth || bounds.outHeight / sample > maxHeight) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private companion object {
        const val MAX_BYTES = 20L * 1024 * 1024
    }
}
