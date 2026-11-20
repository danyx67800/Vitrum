package com.vitrum.tv.ota

import android.content.Context
import com.vitrum.tv.data.UpdateChannel
import com.vitrum.tv.util.SystemUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class OtaManager(context: Context) {

    private val appContext = context.applicationContext
    private val repository = OtaRepository(appContext)
    private val downloader = OtaDownloader(appContext)
    private val installer = ApkInstaller(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    val state: StateFlow<OtaState> = _state.asStateFlow()

    fun check(channel: UpdateChannel, silent: Boolean = false) {
        if (checkJob?.isActive == true) return
        checkJob = scope.launch {
            _state.value = OtaState.Checking
            try {
                val release = withContext(Dispatchers.IO) { repository.latestRelease(channel) }
                _state.value = if (release != null) {
                    OtaState.Available(release)
                } else {
                    OtaState.UpToDate(System.currentTimeMillis())
                }
            } catch (e: OtaException) {
                val current = _state.value
                val knownRelease = (current as? OtaState.Available)?.release
                _state.value = if (silent && knownRelease == null) {
                    OtaState.Idle
                } else {
                    OtaState.Failed(e.error, knownRelease)
                }
            } catch (e: Exception) {
                _state.value = OtaState.Failed(OtaError.UNKNOWN)
            }
        }
    }

    fun downloadAndInstall(
        release: ReleaseInfo,
        wifiOnly: Boolean,
        alreadyVerifiedChecksum: String? = null,
    ) {
        if (downloadJob?.isActive == true) return
        if (!SystemUtils.canInstallPackages(appContext)) {
            _state.value = OtaState.Failed(OtaError.PERMISSION, release)
            return
        }
        if (wifiOnly && !SystemUtils.isUnmetered(appContext)) {
            _state.value = OtaState.Failed(OtaError.WIFI_REQUIRED, release)
            return
        }
        downloadJob = scope.launch {
            try {
                _state.value = OtaState.Downloading(release, 0, release.apk.size, 0)
                val expected = alreadyVerifiedChecksum ?: withContext(Dispatchers.IO) {
                    repository.fetchChecksum(release.checksum)
                }
                val file = withContext(Dispatchers.IO) {
                    downloader.download(release) { downloaded, total, speed ->
                        _state.value = OtaState.Downloading(release, downloaded, total, speed)
                    }
                }
                _state.value = OtaState.Verifying(release)
                val actual = withContext(Dispatchers.Default) { ApkVerifier.sha256(file) }
                if (!actual.equals(expected, ignoreCase = true)) {
                    withContext(Dispatchers.IO) { file.delete() }
                    _state.value = OtaState.Failed(OtaError.CHECKSUM, release)
                    return@launch
                }
                val signatureOk = withContext(Dispatchers.Default) {
                    ApkVerifier.verifySignature(appContext, file)
                }
                if (!signatureOk) {
                    withContext(Dispatchers.IO) { file.delete() }
                    _state.value = OtaState.Failed(OtaError.SIGNATURE, release)
                    return@launch
                }
                _state.value = OtaState.Ready(release, file)
                _state.value = OtaState.Installing(release)
                val started = withContext(Dispatchers.Main) { installer.install(file) }
                if (!started) _state.value = OtaState.Failed(OtaError.UNKNOWN, release)
            } catch (e: CancellationException) {
                _state.value = OtaState.Cancelled
                throw e
            } catch (e: OtaException) {
                _state.value = OtaState.Failed(e.error, release)
            } catch (e: Exception) {
                _state.value = OtaState.Failed(OtaError.UNKNOWN, release)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = OtaState.Cancelled
    }

    fun markVerifying(release: ReleaseInfo) {
        _state.value = OtaState.Verifying(release)
    }

    fun lastChecked(channel: UpdateChannel): Long = repository.lastChecked(channel)

    fun reset() {
        if (downloadJob?.isActive != true) _state.value = OtaState.Idle
    }

    fun close() {
        scope.cancel()
    }

    fun deleteDownloaded(release: ReleaseInfo, file: File?) {
        scope.launch(Dispatchers.IO) {
            file?.delete()
            downloader.clear(release)
        }
    }
}
