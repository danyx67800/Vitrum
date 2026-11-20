package com.vitrum.tv.ota

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.Locale

object ApkVerifier {

    private val digestRegex = Regex("(?i)\\b[a-f0-9]{64}\\b")

    fun parseChecksum(text: String): String? = digestRegex.find(text)?.value?.lowercase(Locale.US)

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * The downloaded APK must be the same package and be signed by exactly the
     * same certificate set as the installed app. This blocks tampered updates.
     */
    fun verifySignature(context: Context, apk: File): Boolean {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = try {
            packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
        } catch (_: Exception) {
            null
        } ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = try {
            packageManager.getPackageInfo(context.packageName, flags)
        } catch (_: Exception) {
            null
        } ?: return false

        val archiveSigners = signerDigests(archive)
        val installedSigners = signerDigests(installed)
        return archiveSigners.isNotEmpty() && archiveSigners == installedSigners
    }

    private fun signerDigests(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        } ?: return emptySet()
        return signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}
