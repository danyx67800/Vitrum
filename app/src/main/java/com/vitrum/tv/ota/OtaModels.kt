package com.vitrum.tv.ota

import com.vitrum.tv.core.SemVer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File

data class ReleaseAsset(
    val name: String,
    val url: String,
    val size: Long,
)

data class ReleaseInfo(
    val tag: String,
    val version: SemVer,
    val title: String,
    val notes: String,
    val prerelease: Boolean,
    val publishedAt: String,
    val apk: ReleaseAsset,
    val checksum: ReleaseAsset?,
)

enum class OtaError {
    OFFLINE,
    RATE_LIMIT,
    NO_SPACE,
    INTERRUPTED,
    CHECKSUM,
    SIGNATURE,
    NOT_GITHUB,
    NO_ASSET,
    NO_CHECKSUM,
    HTTP,
    WIFI_REQUIRED,
    PERMISSION,
    INVALID_APK,
    UNKNOWN,
}

class OtaException(val error: OtaError, message: String? = null) : Exception(message)

sealed interface OtaState {
    data object Idle : OtaState
    data object Checking : OtaState
    data class UpToDate(val checkedAt: Long) : OtaState
    data class Available(val release: ReleaseInfo) : OtaState
    data class Downloading(
        val release: ReleaseInfo,
        val downloaded: Long,
        val total: Long,
        val speed: Long,
    ) : OtaState

    data class Verifying(val release: ReleaseInfo) : OtaState
    data class Ready(val release: ReleaseInfo, val file: File) : OtaState
    data class Installing(val release: ReleaseInfo) : OtaState
    data class Failed(val error: OtaError, val release: ReleaseInfo? = null) : OtaState
    data object Cancelled : OtaState
}

object OtaParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses a GitHub `/releases` array. Drafts are ignored; prereleases are
     * included only for the beta channel. The newest version wins.
     */
    fun parseReleases(body: String, includePrerelease: Boolean): List<ReleaseInfo> {
        val array = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            val root = element.jsonObject
            if (root["draft"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
            val prerelease = root["prerelease"]?.jsonPrimitive?.booleanOrNull ?: false
            if (prerelease && !includePrerelease) return@mapNotNull null
            val tag = root["tag_name"]?.jsonPrimitive?.contentOrNullSafe() ?: return@mapNotNull null
            val version = SemVer.parse(tag) ?: return@mapNotNull null
            val assets = root["assets"]?.jsonArray?.mapNotNull { assetElement ->
                val asset = assetElement.jsonObject
                val name = asset["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return@mapNotNull null
                val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNullSafe()
                    ?: return@mapNotNull null
                val size = asset["size"]?.jsonPrimitive?.longOrNull ?: 0L
                ReleaseAsset(name, url, size)
            }.orEmpty()

            val apk = assets
                .filter { it.name.endsWith(".apk", ignoreCase = true) }
                .sortedByDescending { it.name.startsWith("vitrum", ignoreCase = true) }
                .firstOrNull()
                ?: return@mapNotNull null
            val checksum = assets.firstOrNull { it.name == "${apk.name}.sha256" }
                ?: assets.firstOrNull { it.name.endsWith(".sha256", ignoreCase = true) }

            ReleaseInfo(
                tag = tag,
                version = version,
                title = root["name"]?.jsonPrimitive?.contentOrNullSafe() ?: tag,
                notes = root["body"]?.jsonPrimitive?.contentOrNullSafe().orEmpty(),
                prerelease = prerelease,
                publishedAt = root["published_at"]?.jsonPrimitive?.contentOrNullSafe().orEmpty(),
                apk = apk,
                checksum = checksum,
            )
        }.sortedByDescending { it.version }
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this is kotlinx.serialization.json.JsonNull) null else content
}
