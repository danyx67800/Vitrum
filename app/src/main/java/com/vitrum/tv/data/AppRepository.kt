package com.vitrum.tv.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.vitrum.tv.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lists launchable apps. Many Fire OS apps only declare CATEGORY_LAUNCHER,
 * so both categories are queried and deduplicated by package.
 */
class AppRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packageManager = context.packageManager
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private var refreshJob: Job? = null

    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED,
                -> scheduleRefresh()
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _loading.value = true
            val discovered = runCatching { queryApps() }.getOrElse { emptyList() }
            _apps.value = discovered
            _loading.value = false
        }
    }

    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(600)
            val discovered = runCatching { queryApps() }.getOrElse { emptyList() }
            _apps.value = discovered
            _loading.value = false
        }
    }

    fun appFor(packageName: String): AppInfo? = _apps.value.firstOrNull { it.packageName == packageName }

    private fun queryApps(): List<AppInfo> {
        val found = LinkedHashMap<String, AppInfo>()
        addCategory(found, Intent.CATEGORY_LEANBACK_LAUNCHER)
        addCategory(found, Intent.CATEGORY_LAUNCHER)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Launcher apps: ${found.size} ${found.keys.joinToString()}")
        }
        return found.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun addCategory(target: MutableMap<String, AppInfo>, category: String) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        val resolved = queryIntentActivities(intent)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Query $category: ${resolved.size} activities")
        }
        for (info in resolved) {
            val activity = info.activityInfo ?: continue
            val pkg = activity.packageName ?: continue
            if (pkg == context.packageName) continue
            if (target.containsKey(pkg)) continue
            val appInfo = runCatching { activity.applicationInfo }.getOrNull()
            val flags = appInfo?.flags ?: 0
            val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val label = runCatching { info.loadLabel(packageManager)?.toString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: pkg
            target[pkg] = AppInfo(
                packageName = pkg,
                label = label,
                activityName = activity.name,
                isSystem = isSystem,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L),
                )
            } else {
                packageManager.queryIntentActivities(intent, 0)
            }
        } catch (error: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "App query failed", error)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "VitrumApps"
    }
}
