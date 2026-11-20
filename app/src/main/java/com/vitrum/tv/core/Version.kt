package com.vitrum.tv.core

import java.io.Serializable

/**
 * Semantic version used for OTA comparisons. Release tags use `vX.Y.Z`;
 * versionCode is derived as major * 10000 + minor * 100 + patch.
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer>, Serializable {

    val versionCode: Long
        get() = major.toLong() * 10_000 + minor.toLong() * 100 + patch

    val display: String
        get() = "$major.$minor.$patch"

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = display

    companion object {
        private val TAG_REGEX =
            Regex("""^[vV]?(\d{1,6})\.(\d{1,6})\.(\d{1,6})(?:[-+].*)?$""")

        fun parse(raw: String?): SemVer? {
            val match = TAG_REGEX.matchEntire(raw?.trim().orEmpty()) ?: return null
            val (major, minor, patch) = match.destructured
            return SemVer(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: return null,
            )
        }
    }
}

object VersionLogic {
    /** True when [remote] is strictly newer than the installed [localVersionName]. */
    fun isUpdateAvailable(remote: SemVer, localVersionName: String?): Boolean {
        val local = SemVer.parse(localVersionName) ?: return false
        return remote > local
    }

    fun versionCodeFor(name: String?): Int {
        val semver = SemVer.parse(name) ?: return 0
        return semver.versionCode.toInt()
    }
}
