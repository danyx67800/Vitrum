package com.vitrum.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.AccentPalette
import com.vitrum.tv.data.Settings
import com.vitrum.tv.ui.MainViewModel
import com.vitrum.tv.ui.components.BackgroundChooser
import com.vitrum.tv.ui.components.ColorSwatch
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.components.SettingsHint
import com.vitrum.tv.ui.components.StepperRow
import com.vitrum.tv.ui.components.ToggleRow
import com.vitrum.tv.ui.glass.BackgroundPresets
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.util.DeviceKind
import com.vitrum.tv.util.SystemUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: MainViewModel, settings: Settings) {
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val primaryFocus = remember { FocusRequester() }
    val totalSteps = 4
    val device = remember { SystemUtils.deviceKind(context) }

    BackHandler {
        if (step > 0) step -= 1
    }
    LaunchedEffect(step) {
        delay(120)
        runCatching { primaryFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        if (step == 3) {
            CalibrationPattern()
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(0.86f),
                contentPadding = PaddingValues(28.dp),
                allowRealBlur = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.ob_step, step + 1, totalSteps),
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                    when (step) {
                        0 -> WelcomeStep(type = type)
                        1 -> StyleStep(settings = settings, vm = vm)
                        2 -> LauncherStep(device = device, settings = settings, vm = vm)
                        else -> CalibrationStep(settings = settings, vm = vm)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(totalSteps) { index ->
                                Box(
                                    Modifier
                                        .height(4.dp)
                                        .width(if (index == step) 24.dp else 10.dp)
                                        .then(
                                            Modifier.padding(0.dp),
                                        ),
                                ) {
                                    Canvas(Modifier.fillMaxSize()) {
                                        drawRoundRect(
                                            color = if (index == step) colors.accent else Color.White.copy(alpha = 0.22f),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (step > 0) {
                            GlassButton(
                                text = stringResource(R.string.back),
                                onClick = { step -= 1 },
                            )
                        }
                        GlassButton(
                            text = if (step == totalSteps - 1) stringResource(R.string.ob_start)
                            else stringResource(R.string.continue_label),
                            onClick = {
                                if (step == totalSteps - 1) {
                                    vm.finishOnboarding()
                                } else {
                                    step += 1
                                }
                            },
                            primary = true,
                            modifier = Modifier.focusRequester(primaryFocus),
                        )
                        if (step == 0) {
                            GlassButton(
                                text = stringResource(R.string.skip),
                                onClick = { vm.finishOnboarding() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(type: com.vitrum.tv.ui.theme.VitrumType) {
    val colors = LocalVitrumColors.current
    Text(stringResource(R.string.ob_welcome_title), style = type.display, color = colors.textPrimary)
    Text(stringResource(R.string.ob_welcome_body), style = type.body, color = colors.textSecondary)
    SettingsHint(stringResource(R.string.ob_privacy))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleStep(settings: Settings, vm: MainViewModel) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Text(stringResource(R.string.ob_style_title), style = type.headline, color = colors.textPrimary)
    Text(
        stringResource(R.string.ob_style_body),
        style = type.body,
        color = colors.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    BackgroundChooser(
        presets = BackgroundPresets,
        selectedId = settings.backgroundId,
        onSelectPreset = vm::setBackground,
        onSelectCustom = { vm.setBackground("custom") },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AccentPalette.forEach { choice ->
            ColorSwatch(
                label = null,
                argb = choice.argb,
                selected = settings.accentMode == com.vitrum.tv.data.AccentMode.CUSTOM &&
                    settings.accentArgb == choice.argb,
                onClick = { vm.setAccent(choice.argb) },
            )
        }
        ColorSwatch(
            label = null,
            argb = 0xFFB0BAC9.toInt(),
            selected = settings.accentMode == com.vitrum.tv.data.AccentMode.DYNAMIC,
            onClick = vm::setAccentDynamic,
        )
    }
    SettingsHint(stringResource(R.string.ob_accent) + " · " + stringResource(R.string.accent_dynamic))
}

@Composable
private fun LauncherStep(device: DeviceKind, settings: Settings, vm: MainViewModel) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Text(stringResource(R.string.ob_launcher_title), style = type.headline, color = colors.textPrimary)
    Text(stringResource(R.string.ob_launcher_body), style = type.body, color = colors.textSecondary)
    SettingsHint(
        stringResource(
            when (device) {
                DeviceKind.FIRE_TV -> R.string.fire_tv_detect
                DeviceKind.ANDROID_TV -> R.string.android_tv_detect
                else -> R.string.other_tv_detect
            },
        ),
    )
    when (device) {
        DeviceKind.ANDROID_TV -> {
            GlassButton(
                text = stringResource(R.string.android_tv_open),
                onClick = { vm.openHomeSettings() },
                primary = true,
            )
            Text(
                stringResource(R.string.android_tv_body),
                style = type.caption,
                color = colors.textSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DeviceKind.FIRE_TV -> {
            Text(
                stringResource(R.string.fire_tv_body),
                style = type.caption,
                color = colors.textSecondary,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DeviceKind.OTHER -> {
            GlassButton(
                text = stringResource(R.string.android_tv_open),
                onClick = { vm.openHomeSettings() },
            )
        }
    }
}

@Composable
private fun CalibrationStep(settings: Settings, vm: MainViewModel) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Text(stringResource(R.string.ob_calibration_title), style = type.headline, color = colors.textPrimary)
    Text(stringResource(R.string.ob_calibration_body), style = type.body, color = colors.textSecondary)
    StepperRow(
        label = stringResource(R.string.overscan),
        value = "${settings.overscanMargin}%",
        onMinus = { vm.updateSettings { it.copy(overscanMargin = (it.overscanMargin - 1).coerceAtLeast(0)) } },
        onPlus = { vm.updateSettings { it.copy(overscanMargin = (it.overscanMargin + 1).coerceAtMost(10)) } },
        minusEnabled = settings.overscanMargin > 0,
        plusEnabled = settings.overscanMargin < 10,
        modifier = Modifier.width(520.dp),
    )
    SettingsHint(stringResource(R.string.overscan_preview_hint))
}

@Composable
private fun CalibrationPattern() {
    val colors = LocalVitrumColors.current
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        drawRoundRect(
            color = colors.accent.copy(alpha = 0.7f),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
            ),
        )
        val tick = 22.dp.toPx()
        val cornerColor = colors.textPrimary.copy(alpha = 0.8f)
        drawLine(cornerColor, Offset(size.width / 2 - tick, 0f), Offset(size.width / 2 + tick, 0f), 3.dp.toPx())
        drawLine(cornerColor, Offset(size.width / 2 - tick, size.height), Offset(size.width / 2 + tick, size.height), 3.dp.toPx())
        drawLine(cornerColor, Offset(0f, size.height / 2 - tick), Offset(0f, size.height / 2 + tick), 3.dp.toPx())
        drawLine(cornerColor, Offset(size.width, size.height / 2 - tick), Offset(size.width, size.height / 2 + tick), 3.dp.toPx())
    }
}
