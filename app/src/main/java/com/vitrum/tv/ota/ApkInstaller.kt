package com.vitrum.tv.ota

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

class ApkInstaller(private val context: Context) {

    /** Returns true when the system installer session was started. */
    fun install(apk: File): Boolean {
        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            )
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("vitrum.apk", 0, apk.length()).use { output ->
                    apk.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
                    session.fsync(output)
                }
                val callback = Intent(context, InstallResultReceiver::class.java)
                    .putExtra(InstallResultReceiver.EXTRA_SESSION_ID, sessionId)
                val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val pendingIntent = PendingIntent.getBroadcast(context, sessionId, callback, pendingFlags)
                session.commit(pendingIntent.intentSender)
            }
            true
        } catch (_: Exception) {
            fallbackInstall(apk)
        }
    }

    private fun fallbackInstall(apk: File): Boolean = try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
