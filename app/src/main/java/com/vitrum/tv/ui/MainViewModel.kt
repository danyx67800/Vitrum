package com.vitrum.tv.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitrum.tv.VitrumApp
import com.vitrum.tv.core.AppLogic
import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.CustomRow
import com.vitrum.tv.data.GeoResult
import com.vitrum.tv.data.HomeBehavior
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.SettingsCodec
import com.vitrum.tv.data.SortOrder
import com.vitrum.tv.data.UpdateChannel
import com.vitrum.tv.data.UpdateFrequency
import com.vitrum.tv.data.WeatherNow
import com.vitrum.tv.ota.OtaManager
import com.vitrum.tv.ota.OtaState
import com.vitrum.tv.ota.ReleaseInfo
import com.vitrum.tv.util.NetState
import com.vitrum.tv.util.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VitrumApp
    private val settingsRepo = app.settingsRepository
    private val appRepo = app.appRepository
    private val weatherRepo = app.weatherRepository
    val iconLoader = app.iconLoader
    val otaManager = OtaManager(application)
    private var autoCheckDone = false

    val settings: StateFlow<Settings?> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val apps: StateFlow<List<AppInfo>> = combine(appRepo.apps, settingsRepo.settings) { list, current ->
        if (current.customLabels.isEmpty()) {
            list
        } else {
            list.map { appInfo ->
                val custom = current.customLabels[appInfo.packageName]
                if (custom.isNullOrBlank()) appInfo else appInfo.copy(label = custom)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appsLoading: StateFlow<Boolean> = appRepo.loading
    private val _network = MutableStateFlow(SystemUtils.networkState(application))
    val network: StateFlow<NetState> = _network.asStateFlow()

    private val _weather = MutableStateFlow<WeatherNow?>(null)
    val weather: StateFlow<WeatherNow?> = _weather.asStateFlow()
    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _homeResetTick = MutableStateFlow(0)
    val homeResetTick: StateFlow<Int> = _homeResetTick.asStateFlow()
    val lastHomeFocusKey = MutableStateFlow<String?>(null)
    val lastAllAppsPackage = MutableStateFlow<String?>(null)

    private val _installPermission = MutableStateFlow(SystemUtils.canInstallPackages(application))
    val installPermission: StateFlow<Boolean> = _installPermission.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    val otaState: StateFlow<OtaState> = otaManager.state

    init {
        viewModelScope.launch {
            val current = settingsRepo.settings.first()
            _screen.value = if (current.onboardingDone) Screen.Home else Screen.Onboarding
            maybeAutoCheck(current)
        }
        viewModelScope.launch {
            while (true) {
                refreshNetwork()
                delay(30_000)
            }
        }
        viewModelScope.launch {
            settingsRepo.settings
                .map { WeatherKey(it.weatherEnabled, it.weatherLat, it.weatherLon, it.useCelsius) }
                .distinctUntilChanged()
                .collect { key -> loadWeather(key, force = false) }
        }
    }

    private data class WeatherKey(
        val enabled: Boolean,
        val latitude: Double?,
        val longitude: Double?,
        val celsius: Boolean,
    )

    // --- Navigation ---------------------------------------------------------

    fun openAllApps(focusSearch: Boolean = false) {
        _screen.value = Screen.AllApps(focusSearch)
    }

    fun openSettings(section: SettingsSection? = null) {
        val target = section
            ?: (screen.value as? Screen.Settings)?.section
            ?: SettingsSection.APPEARANCE
        _screen.value = Screen.Settings(target)
    }

    fun openSection(section: SettingsSection) {
        _screen.value = Screen.Settings(section)
    }

    fun openLauncherGuide() {
        _screen.value = Screen.LauncherGuide
    }

    fun goHome(forceTop: Boolean = false) {
        val currentScreen = _screen.value
        _screen.value = Screen.Home
        if (forceTop || currentScreen != Screen.Home ||
            settings.value?.homeBehavior != HomeBehavior.KEEP
        ) {
            _homeResetTick.value += 1
            lastHomeFocusKey.value = null
        }
    }

    fun onHomePressed() {
        val wasHome = _screen.value == Screen.Home
        _screen.value = Screen.Home
        if (!wasHome || settings.value?.homeBehavior != HomeBehavior.KEEP) {
            _homeResetTick.value += 1
            lastHomeFocusKey.value = null
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            settingsRepo.setOnboardingDone(true)
            _homeResetTick.value += 1
            lastHomeFocusKey.value = null
            _screen.value = Screen.Home
        }
    }

    fun onActivityResumed() {
        refreshNetwork()
        _installPermission.value = SystemUtils.canInstallPackages(getApplication())
        val current = settings.value
        if (current != null) {
            loadWeather(
                WeatherKey(
                    current.weatherEnabled,
                    current.weatherLat,
                    current.weatherLon,
                    current.useCelsius,
                ),
                force = true,
            )
        }
    }

    private fun refreshNetwork() {
        _network.value = SystemUtils.networkState(getApplication())
    }

    private fun loadWeather(key: WeatherKey, force: Boolean) {
        if (!key.enabled || key.latitude == null || key.longitude == null) {
            _weather.value = null
            return
        }
        viewModelScope.launch {
            _weatherLoading.value = true
            val result = weatherRepo.current(key.latitude, key.longitude, key.celsius)
            if (result != null || force) _weather.value = result
            _weatherLoading.value = false
        }
    }

    // --- Settings -----------------------------------------------------------

    fun updateSettings(block: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepo.update(block) }
    }

    fun setAccent(argb: Int) = updateSettings {
        it.copy(accentMode = com.vitrum.tv.data.AccentMode.CUSTOM, accentArgb = argb)
    }

    fun setAccentDynamic() = updateSettings {
        it.copy(accentMode = com.vitrum.tv.data.AccentMode.DYNAMIC)
    }

    fun setBackground(id: String) = updateSettings { it.copy(backgroundId = id) }

    fun setCustomBackground(url: String) = updateSettings {
        it.copy(backgroundId = "custom", customBackgroundUrl = url.trim())
    }

    fun setSort(order: SortOrder) = updateSettings { it.copy(sortOrder = order) }

    fun setUpdateChannel(channel: UpdateChannel) {
        updateSettings { it.copy(updateChannel = channel) }
        otaManager.reset()
    }

    suspend fun resolveCity(query: String): GeoResult? = withContext(Dispatchers.IO) {
        val language = java.util.Locale.getDefault().language
        weatherRepo.geocode(query, if (language == "it") "it" else "en")
    }

    fun setWeatherCity(city: GeoResult) {
        updateSettings {
            it.copy(
                weatherEnabled = true,
                weatherCity = city.name,
                weatherLat = city.latitude,
                weatherLon = city.longitude,
            )
        }
    }

    fun clearWeatherCity() = updateSettings {
        it.copy(weatherCity = "", weatherLat = null, weatherLon = null)
    }

    fun toggleFavorite(packageName: String) {
        updateSettings { current ->
            val favorites = if (packageName in current.favorites) {
                current.favorites - packageName
            } else {
                current.favorites + packageName
            }
            current.copy(favorites = favorites)
        }
    }

    fun renameApp(packageName: String, newName: String) {
        updateSettings { current ->
            val labels = current.customLabels.toMutableMap()
            if (newName.isBlank()) labels.remove(packageName) else labels[packageName] = newName.trim()
            current.copy(customLabels = labels)
        }
    }

    fun hideApp(packageName: String) = updateSettings { current ->
        current.copy(hiddenApps = (current.hiddenApps + packageName).distinct())
    }

    fun restoreHidden(packageName: String) = updateSettings { current ->
        current.copy(hiddenApps = current.hiddenApps - packageName)
    }

    fun addCustomRow(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateSettings { current ->
            current.copy(
                customRows = current.customRows + CustomRow(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                ),
            )
        }
    }

    fun deleteCustomRow(id: String) = updateSettings { current ->
        current.copy(customRows = current.customRows.filterNot { it.id == id })
    }

    fun toggleRowApp(rowId: String, packageName: String) {
        updateSettings { current ->
            current.copy(
                customRows = current.customRows.map { row ->
                    if (row.id != rowId) row else {
                        val packages = if (packageName in row.packages) {
                            row.packages - packageName
                        } else {
                            row.packages + packageName
                        }
                        row.copy(packages = packages)
                    }
                },
            )
        }
    }

    fun addToCustomRow(rowId: String, packageName: String) {
        updateSettings { current ->
            current.copy(
                customRows = current.customRows.map { row ->
                    if (row.id == rowId) row.copy(packages = (row.packages + packageName).distinct())
                    else row
                },
            )
        }
    }

    fun moveFavorite(packageName: String, delta: Int) {
        updateSettings { current ->
            val list = current.favorites.toMutableList()
            val index = list.indexOf(packageName)
            if (index < 0) return@updateSettings current
            val target = (index + delta).coerceIn(0, list.lastIndex)
            if (target == index) return@updateSettings current
            list.removeAt(index)
            list.add(target, packageName)
            current.copy(favorites = list)
        }
    }

    fun moveManual(packageName: String, delta: Int) {
        updateSettings { current ->
            val all = apps.value.map { it.packageName }
            val base = (current.manualOrder + all).distinct()
            val list = base.toMutableList()
            val index = list.indexOf(packageName)
            if (index < 0) return@updateSettings current
            val target = (index + delta).coerceIn(0, list.lastIndex)
            if (target == index) return@updateSettings current
            list.removeAt(index)
            list.add(target, packageName)
            current.copy(manualOrder = list)
        }
    }

    fun resetRows() = updateSettings {
        it.copy(
            rowOrder = listOf(
                com.vitrum.tv.data.RowKind.FAVORITES,
                com.vitrum.tv.data.RowKind.RECENT,
                com.vitrum.tv.data.RowKind.ALL_APPS,
                com.vitrum.tv.data.RowKind.SHORTCUTS,
            ),
        )
    }

    fun resetSettings() {
        viewModelScope.launch {
            val onboarding = settingsRepo.current().onboardingDone
            settingsRepo.save(Settings(onboardingDone = onboarding))
            _messages.emit(getApplication<Application>().getString(com.vitrum.tv.R.string.reset_done))
        }
    }

    fun exportSettings(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val payload = SettingsCodec.encode(settings.value ?: Settings())
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("no stream")
                }
            }
            _messages.emit(
                getApplication<Application>().getString(
                    if (result.isSuccess) com.vitrum.tv.R.string.export_done
                    else com.vitrum.tv.R.string.export_failed,
                ),
            )
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            val imported = runCatching {
                withContext(Dispatchers.IO) {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                    SettingsCodec.decode(text)
                }
            }.getOrNull()
            if (imported == null) {
                _messages.emit(getApplication<Application>().getString(com.vitrum.tv.R.string.import_failed))
                return@launch
            }
            settingsRepo.save(imported.copy(onboardingDone = true))
            _messages.emit(getApplication<Application>().getString(com.vitrum.tv.R.string.import_done))
        }
    }

    // --- Apps ---------------------------------------------------------------

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val started = SystemUtils.openApp(
                getApplication(),
                appInfo.packageName,
                appInfo.activityName,
            )
            if (!started) {
                _messages.emit(getApplication<Application>().getString(com.vitrum.tv.R.string.app_unavailable))
                appRepo.refresh()
                return@launch
            }
            settingsRepo.update { current ->
                current.copy(recentApps = AppLogic.pushRecent(current.recentApps, appInfo.packageName))
            }
        }
    }

    fun openAppInfo(appInfo: AppInfo) {
        if (!SystemUtils.openAppInfo(getApplication(), appInfo.packageName)) {
            emitActionFailed()
        }
    }

    fun uninstallApp(appInfo: AppInfo) {
        if (!SystemUtils.uninstall(getApplication(), appInfo.packageName)) {
            emitActionFailed()
        }
    }

    fun openSystemAction(action: String, fallback: String? = null) {
        if (!SystemUtils.openSettings(getApplication(), action, fallback)) {
            emitActionFailed()
        }
    }

    fun openHomeSettings() {
        if (!SystemUtils.openHomeSettings(getApplication())) emitActionFailed()
    }

    fun openInstallPermission() {
        if (!SystemUtils.openInstallPermission(getApplication())) emitActionFailed()
    }

    fun openUrl(url: String) {
        if (!SystemUtils.openUrl(getApplication(), url)) emitActionFailed()
    }

    fun toast(text: String) {
        viewModelScope.launch { _messages.emit(text) }
    }

    fun toast(resId: Int) {
        viewModelScope.launch { _messages.emit(getApplication<Application>().getString(resId)) }
    }

    private fun emitActionFailed() {
        toast(com.vitrum.tv.R.string.action_failed)
    }

    // --- Updates ------------------------------------------------------------

    fun checkForUpdates(silent: Boolean = false) {
        val current = settings.value ?: return
        otaManager.check(current.updateChannel, silent)
    }

    fun downloadUpdate(release: ReleaseInfo) {
        val current = settings.value ?: Settings()
        _installPermission.value = SystemUtils.canInstallPackages(getApplication())
        otaManager.downloadAndInstall(release, current.wifiOnlyUpdates)
    }

    fun cancelUpdate() = otaManager.cancelDownload()

    fun refreshInstallPermission() {
        _installPermission.value = SystemUtils.canInstallPackages(getApplication())
    }

    private fun maybeAutoCheck(current: Settings) {
        if (autoCheckDone) return
        autoCheckDone = true
        if (current.autoUpdate == UpdateFrequency.NEVER) return
        val last = otaManager.lastChecked(current.updateChannel)
        val now = System.currentTimeMillis()
        val due = when (current.autoUpdate) {
            UpdateFrequency.NEVER -> false
            UpdateFrequency.ON_START -> true
            UpdateFrequency.DAILY -> now - last > 24 * 60 * 60 * 1000L
            UpdateFrequency.WEEKLY -> now - last > 7 * 24 * 60 * 60 * 1000L
        }
        if (due) otaManager.check(current.updateChannel, silent = true)
    }

    override fun onCleared() {
        otaManager.close()
        super.onCleared()
    }
}
