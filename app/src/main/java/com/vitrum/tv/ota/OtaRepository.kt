package com.vitrum.tv.ota

import android.content.Context
import com.vitrum.tv.BuildConfig
import com.vitrum.tv.core.Http
import com.vitrum.tv.data.UpdateChannel
import com.vitrum.tv.util.NetState
import com.vitrum.tv.util.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException

class OtaRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("vitrum_ota", Context.MODE_PRIVATE)

    /**
     * Returns the newest release for the channel, or null when the installed
     * build is current. Uses If-None-Match to avoid burning the anonymous
     * GitHub API quota.
     */
    suspend fun latestRelease(channel: UpdateChannel): ReleaseInfo? = withContext(Dispatchers.IO) {
        if (SystemUtils.networkState(context) == NetState.OFFLINE) throw OtaException(OtaError.OFFLINE)

        val includePrerelease = channel == UpdateChannel.BETA
        val url = "${BuildConfig.UPDATE_API}repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}" +
            "/releases?per_page=20"
        val etagKey = "etag_$channel"
        val bodyKey = "body_$channel"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", Http.USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
        prefs.getString(etagKey, null)?.let { requestBuilder.header("If-None-Match", it) }

        val releases: List<ReleaseInfo> = try {
            Http.client.newCall(requestBuilder.build()).execute().use { response ->
                when {
                    response.code == 304 -> {
                        val cached = prefs.getString(bodyKey, null)
                            ?: throw OtaException(OtaError.HTTP)
                        OtaParser.parseReleases(cached, includePrerelease)
                    }
                    response.code == 200 -> {
                        val body = response.body?.string().orEmpty()
                        val etag = response.header("ETag")
                        val editor = prefs.edit().putString(bodyKey, body)
                        if (etag != null) editor.putString(etagKey, etag)
                        editor.apply()
                        OtaParser.parseReleases(body, includePrerelease)
                    }
                    response.code == 403 || response.code == 429 -> {
                        if (response.header("X-RateLimit-Remaining") == "0" || response.code == 429) {
                            throw OtaException(OtaError.RATE_LIMIT)
                        }
                        throw OtaException(OtaError.HTTP)
                    }
                    response.code == 404 -> emptyList()
                    else -> throw OtaException(OtaError.HTTP)
                }
            }
        } catch (e: OtaException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw OtaException(OtaError.OFFLINE, e.message)
        } catch (e: IOException) {
            throw OtaException(
                if (SystemUtils.networkState(context) == NetState.OFFLINE) OtaError.OFFLINE else OtaError.INTERRUPTED,
                e.message,
            )
        }

        prefs.edit().putLong("last_check_$channel", System.currentTimeMillis()).apply()
        releases.firstOrNull { release ->
            com.vitrum.tv.core.VersionLogic.isUpdateAvailable(release.version, BuildConfig.VERSION_NAME)
        }
    }

    /** Downloads the small checksum asset; returns the hex digest. */
    suspend fun fetchChecksum(asset: ReleaseAsset?): String = withContext(Dispatchers.IO) {
        if (asset == null) throw OtaException(OtaError.NO_CHECKSUM)
        if (!isAllowedGitHubUrl(asset.url)) throw OtaException(OtaError.NOT_GITHUB)
        try {
            Http.client.newCall(
                Request.Builder().url(asset.url).header("User-Agent", Http.USER_AGENT).build(),
            ).execute().use { response ->
                if (!response.isSuccessful) throw OtaException(OtaError.HTTP)
                val text = response.body?.string().orEmpty().take(8192)
                ApkVerifier.parseChecksum(text) ?: throw OtaException(OtaError.NO_CHECKSUM)
            }
        } catch (e: OtaException) {
            throw e
        } catch (e: Exception) {
            throw OtaException(OtaError.INTERRUPTED, e.message)
        }
    }

    fun lastChecked(channel: UpdateChannel): Long = prefs.getLong("last_check_$channel", 0L)
}

/** Only HTTPS URLs from GitHub-owned hosts are ever downloaded. */
fun isAllowedGitHubUrl(raw: String): Boolean {
    val url = runCatching { raw.toHttpUrlOrNull() }.getOrNull() ?: return false
    if (url.scheme != "https") return false
    val host = url.host.lowercase()
    return host == "github.com" ||
        host == "api.github.com" ||
        host == "objects.githubusercontent.com" ||
        host == "release-assets.githubusercontent.com" ||
        host.endsWith(".githubusercontent.com")
}
