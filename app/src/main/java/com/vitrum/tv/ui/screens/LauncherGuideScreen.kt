package com.vitrum.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.Settings
import com.vitrum.tv.ui.MainViewModel
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.util.DeviceKind
import com.vitrum.tv.util.SystemUtils
import kotlinx.coroutines.delay

@Composable
fun LauncherGuideScreen(vm: MainViewModel, settings: Settings) {
    val context = LocalContext.current
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val device = remember { SystemUtils.deviceKind(context) }
    val firstFocus = remember { FocusRequester() }

    BackHandler { vm.openSettings(com.vitrum.tv.ui.SettingsSection.SYSTEM) }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassPanel(
            modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth(),
            contentPadding = PaddingValues(26.dp),
            allowRealBlur = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = when (device) {
                        DeviceKind.FIRE_TV -> stringResource(R.string.fire_tv_title)
                        else -> stringResource(R.string.android_tv_title)
                    },
                    style = type.headline,
                    color = colors.textPrimary,
                )
                Text(
                    text = when (device) {
                        DeviceKind.FIRE_TV -> stringResource(R.string.fire_tv_body)
                        else -> stringResource(R.string.android_tv_body)
                    },
                    style = type.body,
                    color = colors.textSecondary,
                )
                if (device != DeviceKind.FIRE_TV) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(
                            text = stringResource(R.string.android_tv_open),
                            onClick = vm::openHomeSettings,
                            primary = true,
                            modifier = Modifier.focusRequester(firstFocus),
                        )
                        GlassButton(
                            text = stringResource(R.string.android_tv_alt),
                            onClick = {
                                if (!SystemUtils.launch(
                                        context,
                                        android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
                                        ),
                                    )
                                ) {
                                    vm.toast(R.string.action_failed)
                                }
                            },
                        )
                    }
                } else {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp),
                        allowRealBlur = false,
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.fire_tv_adb),
                                style = type.caption,
                                color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.fire_tv_adb_command),
                                style = type.body.copy(fontFamily = FontFamily.Monospace),
                                color = colors.accent,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.fire_tv_detect),
                        style = type.caption,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    GlassButton(
                        text = stringResource(R.string.close),
                        onClick = { vm.openSettings(com.vitrum.tv.ui.SettingsSection.SYSTEM) },
                        modifier = if (device == DeviceKind.FIRE_TV) Modifier.focusRequester(firstFocus) else Modifier,
                    )
                }
            }
        }
    }
}
