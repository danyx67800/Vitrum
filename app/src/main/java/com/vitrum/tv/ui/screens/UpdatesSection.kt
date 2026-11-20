package com.vitrum.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.UpdateChannel
import com.vitrum.tv.data.UpdateFrequency
import com.vitrum.tv.ota.OtaError
import com.vitrum.tv.ota.OtaState
import com.vitrum.tv.ui.MainViewModel
import com.vitrum.tv.ui.components.ActionRow
import com.vitrum.tv.ui.components.ChoiceRow
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.components.SettingsHint
import com.vitrum.tv.ui.components.SettingsSectionTitle
import com.vitrum.tv.ui.components.ToggleRow
import com.vitrum.tv.ui.components.bringIntoViewOnFocus
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.util.SystemUtils

internal fun LazyListScope.updatesSection(
    vm: MainViewModel,
    settings: Settings,
    state: OtaState,
    permission: Boolean,
) {
    item(key = "updates_header") {
        SettingsSectionTitle(stringResource(R.string.updates))
        ActionRow(
            label = stringResource(R.string.check_updates),
            value = when (state) {
                is OtaState.Checking -> stringResource(R.string.checking_updates)
                is OtaState.UpToDate -> stringResource(R.string.up_to_date)
                is OtaState.Available -> "v${state.release.version.display}"
                is OtaState.Failed -> stringResource(otaErrorRes(state.error))
                else -> null
            },
            onClick = { vm.checkForUpdates() },
        )
    }

    when (state) {
        is OtaState.Checking -> item(key = "updates_checking") {
            SettingsHint(stringResource(R.string.checking_updates))
        }
        is OtaState.UpToDate -> item(key = "updates_current") {
            SettingsHint(stringResource(R.string.up_to_date))
        }
        is OtaState.Available -> {
            item(key = "updates_available") {
                AvailableUpdate(release = state.release, permission = permission, vm = vm)
            }
        }
        is OtaState.Downloading -> item(key = "updates_progress") {
            DownloadProgress(state = state, vm = vm)
        }
        is OtaState.Verifying -> item(key = "updates_verifying") {
            SettingsHint(stringResource(R.string.verifying))
        }
        is OtaState.Ready -> item(key = "updates_ready") {
            SettingsHint(stringResource(R.string.update_ready))
        }
        is OtaState.Installing -> item(key = "updates_installing") {
            SettingsHint(stringResource(R.string.installing))
        }
        is OtaState.Cancelled -> item(key = "updates_cancelled") {
            SettingsHint(stringResource(R.string.update_cancelled))
        }
        is OtaState.Failed -> item(key = "updates_failed") {
            UpdateFailure(state = state, vm = vm)
        }
        OtaState.Idle -> Unit
    }

    item(key = "updates_permission") {
        if (!permission && state is OtaState.Available) {
            InstallPermissionCard(vm = vm)
        }
    }

    item(key = "updates_options") {
        SettingsSectionTitle(stringResource(R.string.updates))
        ChoiceRow(
            label = stringResource(R.string.auto_update),
            options = UpdateFrequency.entries.map { frequency ->
                stringResource(
                    when (frequency) {
                        UpdateFrequency.NEVER -> R.string.auto_update_never
                        UpdateFrequency.ON_START -> R.string.auto_update_start
                        UpdateFrequency.DAILY -> R.string.auto_update_daily
                        UpdateFrequency.WEEKLY -> R.string.auto_update_weekly
                    },
                )
            },
            selectedIndex = UpdateFrequency.entries.indexOf(settings.autoUpdate),
            onSelect = { index -> vm.updateSettings { it.copy(autoUpdate = UpdateFrequency.entries[index]) } },
        )
        ChoiceRow(
            label = stringResource(R.string.update_channel),
            options = listOf(
                stringResource(R.string.channel_stable),
                stringResource(R.string.channel_beta),
            ),
            selectedIndex = if (settings.updateChannel == UpdateChannel.STABLE) 0 else 1,
            onSelect = { index ->
                vm.setUpdateChannel(if (index == 0) UpdateChannel.STABLE else UpdateChannel.BETA)
            },
        )
        ToggleRow(
            label = stringResource(R.string.wifi_only),
            checked = settings.wifiOnlyUpdates,
            onToggle = { vm.updateSettings { it.copy(wifiOnlyUpdates = !it.wifiOnlyUpdates) } },
        )
        SettingsHint(stringResource(R.string.update_check_on_start))
    }
}

@Composable
private fun AvailableUpdate(
    release: com.vitrum.tv.ota.ReleaseInfo,
    permission: Boolean,
    vm: MainViewModel,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.update_available, release.version.display),
            style = type.title,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.release_size, SystemUtils.formatBytes(release.apk.size)),
            style = type.caption,
            color = colors.textSecondary,
        )
        if (release.notes.isNotBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(14.dp),
            ) {
                Text(
                    text = release.notes,
                    style = type.body,
                    color = colors.textSecondary,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!permission) {
            InstallPermissionCard(vm = vm)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = stringResource(R.string.download_and_install),
                    onClick = { vm.downloadUpdate(release) },
                    primary = true,
                    modifier = Modifier.bringIntoViewOnFocus(),
                )
            }
        }
    }
}

@Composable
private fun DownloadProgress(
    state: OtaState.Downloading,
    vm: MainViewModel,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val total = state.total.coerceAtLeast(1L)
    val fraction = (state.downloaded.toFloat() / total).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                R.string.download_progress,
                percent,
                SystemUtils.formatSpeed(state.speed),
            ),
            style = type.body,
            color = colors.textPrimary,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
        Text(
            text = "${SystemUtils.formatBytes(state.downloaded)} / ${SystemUtils.formatBytes(state.total)}",
            style = type.caption,
            color = colors.textSecondary,
        )
        GlassButton(
            text = stringResource(R.string.cancel),
            onClick = vm::cancelUpdate,
            modifier = Modifier.bringIntoViewOnFocus(),
        )
    }
}

@Composable
private fun UpdateFailure(state: OtaState.Failed, vm: MainViewModel) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(otaErrorRes(state.error)),
            style = type.body,
            color = colors.textPrimary,
        )
        if (state.error == OtaError.PERMISSION) {
            InstallPermissionCard(vm = vm)
        } else {
            val retry = {
                val release = state.release
                val restoreCheck = state.error == OtaError.CHECKSUM ||
                    state.error == OtaError.SIGNATURE ||
                    state.error == OtaError.NO_CHECKSUM ||
                    state.error == OtaError.NO_ASSET ||
                    state.error == OtaError.HTTP ||
                    state.error == OtaError.RATE_LIMIT
                if (release != null && !restoreCheck) {
                    vm.downloadUpdate(release)
                } else {
                    vm.checkForUpdates()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = stringResource(R.string.retry),
                    onClick = retry,
                    primary = true,
                    modifier = Modifier.bringIntoViewOnFocus(),
                )
                if (state.error == OtaError.WIFI_REQUIRED) {
                    Text(
                        text = stringResource(R.string.err_wifi_only),
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallPermissionCard(vm: MainViewModel) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accent.copy(alpha = 0.10f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.install_permission_title),
            style = type.title,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.install_permission_body),
            style = type.body,
            color = colors.textSecondary,
        )
        GlassButton(
            text = stringResource(R.string.install_permission_open),
            onClick = vm::openInstallPermission,
            primary = true,
            modifier = Modifier.bringIntoViewOnFocus(),
        )
    }
}

private fun otaErrorRes(error: OtaError): Int = when (error) {
    OtaError.OFFLINE -> R.string.err_offline
    OtaError.RATE_LIMIT -> R.string.err_rate_limit
    OtaError.NO_SPACE -> R.string.err_space
    OtaError.INTERRUPTED -> R.string.err_interrupted
    OtaError.CHECKSUM -> R.string.err_checksum
    OtaError.SIGNATURE -> R.string.err_signature
    OtaError.NOT_GITHUB -> R.string.err_not_github
    OtaError.NO_ASSET -> R.string.err_no_asset
    OtaError.NO_CHECKSUM -> R.string.err_no_checksum
    OtaError.WIFI_REQUIRED -> R.string.err_wifi_only
    OtaError.PERMISSION -> R.string.install_permission_missing
    OtaError.INVALID_APK -> R.string.err_signature
    OtaError.HTTP -> R.string.err_http
    OtaError.UNKNOWN -> R.string.err_unknown
}
