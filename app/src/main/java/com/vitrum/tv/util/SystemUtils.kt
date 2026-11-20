package com.vitrum.tv.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NetState { WIFI, ETHERNET, OTHER, OFFLINE }

enum class DeviceKind { FIRE_TV, ANDROID_TV, OTHER }

data class AppShortcut(val labelRes: Int, val action: String, val fallbackAction: String? = null)

object SystemUtils {

    fun networkState(context: Context): NetState {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetState.OFFLINE
            @Suppress("DEPRECATION")
            val network = cm.activeNetwork ?: return NetState.OFFLINE
            val caps = cm.getNetworkCapabilities(network) ?: return NetState.OFFLINE
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetState.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetState.ETHERNET
                else -> NetState.OTHER
            }
        } catch (_: Exception) {
            NetState.OFFLINE
        }
    }

    fun isUnmetered(context: Context): Boolean {
        val state = networkState(context)
        return state == NetState.WIFI || state == NetState.ETHERNET
    }

    fun deviceKind(context: Context): DeviceKind {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.US)
        val model = Build.MODEL.orEmpty().uppercase(Locale.US)
        val isFire = manufacturer.contains("amazon") ||
            model.startsWith("AFT") ||
            runCatching {
                context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")
            }.getOrDefault(false)
        if (isFire) return DeviceKind.FIRE_TV
        val isTv = runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }.getOrDefault(false)
        return if (isTv) DeviceKind.ANDROID_TV else DeviceKind.OTHER
    }

    fun canResolve(context: Context, intent: Intent): Boolean = try {
        intent.resolveActivity(context.packageManager) != null
    } catch (_: Exception) {
        false
    }

    fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }

    fun openUrl(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") return false
        return launch(
            context,
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openApp(context: Context, packageName: String, activityName: String): Boolean {
        return launch(
            context,
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(packageName, activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        ) || launch(
            context,
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setClassName(packageName, activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openAppInfo(context: Context, packageName: String): Boolean = launch(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
    )

    fun uninstall(context: Context, packageName: String): Boolean = launch(
        context,
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")),
    )

    fun openSettings(context: Context, action: String, fallback: String? = null): Boolean {
        val primary = Intent(action)
        if (canResolve(context, primary) && launch(context, primary)) return true
        if (fallback != null) {
            val secondary = Intent(fallback)
            if (canResolve(context, secondary) && launch(context, secondary)) return true
        }
        val generic = Intent(Settings.ACTION_SETTINGS)
        return canResolve(context, generic) && launch(context, generic)
    }

    fun openHomeSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("android.settings.HOME_SETTINGS"),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            if (canResolve(context, intent)) return launch(context, intent)
        }
        return false
    }

    fun openInstallPermission(context: Context): Boolean {
        val pkgUri = Uri.parse("package:${context.packageName}")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val primary = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri)
            if (canResolve(context, primary) && launch(context, primary)) {
                true
            } else {
                launch(context, Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        } else {
            launch(context, Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            @Suppress("DEPRECATION")
            runCatching { Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1 }
                .getOrDefault(true)
        }
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
    }

    fun formatSpeed(bytesPerSecond: Long): String =
        formatBytes(bytesPerSecond.coerceAtLeast(0)) + "/s"

    fun formatTime(timestamp: Long, pattern: String = "d MMM"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
