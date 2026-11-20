package com.vitrum.tv.ota

import android.content.Context
import com.vitrum.tv.core.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

class OtaDownloader(private val context: Context) {

    suspend fun download(
        release: ReleaseInfo,
        onProgress: (downloaded: Long, total: Long, speed: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (release.apk.size > 0) {
            val usable = context.cacheDir.usableSpace
            val existing = downloadFile(release).takeIf { it.exists() }?.length() ?: 0L
            if (usable + existing < release.apk.size + 2 * 1024 * 1024) {
                throw OtaException(OtaError.NO_SPACE)
            }
        }
        if (!isAllowedGitHubUrl(release.apk.url)) throw OtaException(OtaError.NOT_GITHUB)

        val target = downloadFile(release)
        target.parentFile?.mkdirs()
        var existing = if (target.exists()) target.length() else 0L
        val builder = Request.Builder()
            .url(release.apk.url)
            .header("User-Agent", Http.USER_AGENT)
            .header("Accept", "application/octet-stream")
        if (existing > 0) builder.header("Range", "bytes=$existing-")

        try {
            Http.client.newCall(builder.build()).execute().use { response ->
                if (response.code == 416 && existing > 0) {
                    target.delete()
                    throw OtaException(OtaError.INTERRUPTED, "range not satisfiable")
                }
                if (!response.isSuccessful && response.code != 206) {
                    throw OtaException(OtaError.HTTP, "http ${response.code}")
                }
                val finalUrl = response.request.url.toString()
                if (!isAllowedGitHubUrl(finalUrl)) throw OtaException(OtaError.NOT_GITHUB)
                val body = response.body ?: throw OtaException(OtaError.INTERRUPTED)
                val append = response.code == 206 && existing > 0
                val total = if (append) {
                    existing + (body.contentLength().takeIf { it > 0 } ?: 0L)
                } else {
                    existing = 0
                    body.contentLength().takeIf { it > 0 } ?: release.apk.size
                }

                var downloaded = existing
                var lastEmit = 0L
                var lastBytes = existing
                var lastTime = System.currentTimeMillis()
                var speed = 0L
                body.byteStream().use { input ->
                    java.io.FileOutputStream(target, append).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 150) {
                                val elapsed = (now - lastTime).coerceAtLeast(1)
                                speed = ((downloaded - lastBytes) * 1000 / elapsed)
                                lastEmit = now
                                lastBytes = downloaded
                                lastTime = now
                                onProgress(downloaded, total, speed)
                            }
                        }
                        output.flush()
                    }
                }
                onProgress(downloaded, total, speed)
                if (total > 0 && downloaded < total) throw OtaException(OtaError.INTERRUPTED)
            }
            target
        } catch (e: OtaException) {
            throw e
        } catch (e: IOException) {
            throw OtaException(OtaError.INTERRUPTED, e.message)
        }
    }

    fun downloadFile(release: ReleaseInfo): File =
        File(context.cacheDir, "updates/v${release.version.display}/Vitrum-v${release.version.display}.apk")

    fun clear(release: ReleaseInfo) {
        runCatching { downloadFile(release).delete() }
    }
}
