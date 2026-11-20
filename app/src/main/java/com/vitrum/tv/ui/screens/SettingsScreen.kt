package com.vitrum.tv.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.vitrum.tv.BuildConfig
import com.vitrum.tv.R
import com.vitrum.tv.data.AccentPalette
import com.vitrum.tv.data.AnimationMode
import com.vitrum.tv.data.CustomRow
import com.vitrum.tv.data.EffectsQuality
import com.vitrum.tv.data.HomeBehavior
import com.vitrum.tv.data.RowKind
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.SortOrder
import com.vitrum.tv.data.TextSizeOption
import com.vitrum.tv.data.ThemeMode
import com.vitrum.tv.data.TileSize
import com.vitrum.tv.data.TileStyle
import com.vitrum.tv.data.UpdateChannel
import com.vitrum.tv.data.UpdateFrequency
import com.vitrum.tv.ota.OtaState
import com.vitrum.tv.ui.MainViewModel
import com.vitrum.tv.ui.SettingsSection
import com.vitrum.tv.ui.components.ActionRow
import com.vitrum.tv.ui.components.AppPickerDialog
import com.vitrum.tv.ui.components.BackgroundChooser
import com.vitrum.tv.ui.components.ChoiceRow
import com.vitrum.tv.ui.components.ColorSwatch
import com.vitrum.tv.ui.components.ConfirmDialog
import com.vitrum.tv.ui.components.FocusableGlass
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.components.GlassIconButton
import com.vitrum.tv.ui.components.InfoRow
import com.vitrum.tv.ui.components.RowOrderRow
import com.vitrum.tv.ui.components.SettingsHint
import com.vitrum.tv.ui.components.SettingsSectionTitle
import com.vitrum.tv.ui.components.StepperRow
import com.vitrum.tv.ui.components.TextEntryDialog
import com.vitrum.tv.ui.components.ToggleRow
import com.vitrum.tv.ui.components.bringIntoViewOnFocus
import com.vitrum.tv.ui.glass.BackgroundPresets
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.util.SystemUtils
import com.vitrum.tv.util.TvInputs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface SettingsDialog {
    data object Favorites : SettingsDialog
    data class RowApps(val row: CustomRow) : SettingsDialog
    data object NewRow : SettingsDialog
    data object BackgroundUrl : SettingsDialog
    data object City : SettingsDialog
    data object Reset : SettingsDialog
    data class DeleteRow(val row: CustomRow) : SettingsDialog
}

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    settings: Settings,
    section: SettingsSection,
) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val otaState by vm.otaState.collectAsStateWithLifecycle()
    val permission by vm.installPermission.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var showOverscan by remember { mutableStateOf(false) }
    val categoryFocus = remember { FocusRequester() }
    val contentState = rememberLazyListState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) vm.exportSettings(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.importSettings(uri) }

    BackHandler {
        if (showOverscan) showOverscan = false else vm.goHome()
    }
    LaunchedEffect(Unit) {
        vm.refreshInstallPermission()
        delay(120)
        runCatching { categoryFocus.requestFocus() }
    }
    LaunchedEffect(section) {
        runCatching { contentState.scrollToItem(0) }
    }

    Row(Modifier.fillMaxSize().padding(22.dp)) {
        GlassPanel(
            modifier = Modifier.width(286.dp).fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            allowRealBlur = true,
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = type.headline,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(SettingsSection.entries, key = { it.name }) { entry ->
                        CategoryItem(
                            section = entry,
                            selected = entry == section,
                            updateBadge = entry == SettingsSection.ABOUT && otaState is OtaState.Available,
                            onClick = { vm.openSection(entry) },
                            modifier = if (entry == section) {
                                Modifier.focusRequester(categoryFocus)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        GlassPanel(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(0.dp),
            allowRealBlur = true,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(section.titleRes),
                        style = type.headline,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (section == SettingsSection.ABOUT && otaState is OtaState.Available) {
                        Text(
                            text = stringResource(R.string.update_badge),
                            style = type.caption,
                            color = colors.accent,
                            modifier = Modifier
                                .background(colors.accent.copy(alpha = 0.16f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                LazyColumn(
                    state = contentState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 4.dp, bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (section) {
                        SettingsSection.APPEARANCE -> appearanceSection(
                            settings, vm, onDialog = { dialog = it },
                        )
                        SettingsSection.LAYOUT -> layoutSection(
                            settings, vm, onPreviewOverscan = { showOverscan = true },
                        )
                        SettingsSection.APPS -> appsSection(
                            settings, apps, onDialog = { dialog = it }, vm = vm,
                        )
                        SettingsSection.CLOCK_WEATHER -> clockSection(
                            settings, vm, scope, onDialog = { dialog = it },
                        )
                        SettingsSection.BEHAVIOR -> behaviorSection(settings, vm)
                        SettingsSection.SYSTEM -> systemSection(vm, context, settings)
                        SettingsSection.BACKUP -> backupSection(
                            vm = vm,
                            onExport = {
                                runCatching { exportLauncher.launch("vitrum-settings.json") }
                                    .onFailure { vm.toast(R.string.picker_unavailable) }
                            },
                            onImport = {
                                runCatching {
                                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                                }.onFailure { vm.toast(R.string.picker_unavailable) }
                            },
                            onReset = { dialog = SettingsDialog.Reset },
                        )
                        SettingsSection.ABOUT -> aboutSection(
                            vm = vm,
                            settings = settings,
                            otaState = otaState,
                            permission = permission,
                            onDialog = { dialog = it },
                        )
                    }
                }
            }
        }
    }

    if (showOverscan) {
        OverscanPreviewDialog(
            margin = settings.overscanMargin,
            onDismiss = { showOverscan = false },
        )
    }

    when (val current = dialog) {
        SettingsDialog.Favorites -> AppPickerDialog(
            title = stringResource(R.string.manage_favorites),
            apps = apps,
            initiallySelected = settings.favorites.toSet(),
            onApply = { selection ->
                vm.updateSettings {
                    it.copy(favorites = apps.filter { app -> app.packageName in selection }.map { app -> app.packageName })
                }
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is SettingsDialog.RowApps -> AppPickerDialog(
            title = current.row.name,
            apps = apps,
            initiallySelected = current.row.packages.toSet(),
            onApply = { selection ->
                vm.updateSettings { currentSettings ->
                    currentSettings.copy(
                        customRows = currentSettings.customRows.map { row ->
                            if (row.id == current.row.id) {
                                row.copy(packages = apps.filter { it.packageName in selection }.map { it.packageName })
                            } else {
                                row
                            }
                        },
                    )
                }
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.NewRow -> TextEntryDialog(
            title = stringResource(R.string.new_row),
            label = stringResource(R.string.row_name),
            hint = stringResource(R.string.row_name),
            allowEmpty = false,
            onConfirm = { name ->
                vm.addCustomRow(name)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.BackgroundUrl -> TextEntryDialog(
            title = stringResource(R.string.background_custom),
            label = stringResource(R.string.custom_url),
            initialValue = settings.customBackgroundUrl,
            hint = stringResource(R.string.custom_url_hint),
            allowEmpty = false,
            onConfirm = { url ->
                if (url.startsWith("https://", ignoreCase = true) && url.contains(".")) {
                    vm.setCustomBackground(url)
                    dialog = null
                } else {
                    vm.toast(R.string.custom_url_invalid)
                }
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.City -> TextEntryDialog(
            title = stringResource(R.string.city),
            label = stringResource(R.string.city_search_hint),
            initialValue = settings.weatherCity,
            hint = stringResource(R.string.city_search_hint),
            onConfirm = { city ->
                dialog = null
                if (city.isBlank()) {
                    vm.clearWeatherCity()
                } else {
                    scope.launch {
                        val result = vm.resolveCity(city)
                        if (result == null) vm.toast(R.string.city_not_found) else vm.setWeatherCity(result)
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Reset -> ConfirmDialog(
            title = stringResource(R.string.reset_confirm_title),
            message = stringResource(R.string.reset_confirm_body),
            confirmLabel = stringResource(R.string.reset_defaults),
            destructive = true,
            onConfirm = {
                vm.resetSettings()
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is SettingsDialog.DeleteRow -> ConfirmDialog(
            title = stringResource(R.string.delete_row),
            message = stringResource(R.string.delete_row_confirm, current.row.name),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                vm.deleteCustomRow(current.row.id)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun CategoryItem(
    section: SettingsSection,
    selected: Boolean,
    updateBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(46.dp),
        allowRealBlur = false,
        focusedScale = 1.02f,
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) { focused ->
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(section.titleRes),
                style = type.label,
                color = when {
                    selected -> colors.accent
                    focused -> colors.textPrimary
                    else -> colors.textSecondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (updateBadge) {
                Box(Modifier.size(8.dp).background(colors.accent, CircleShape))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.appearanceSection(
    settings: Settings,
    vm: MainViewModel,
    onDialog: (SettingsDialog) -> Unit,
) {
    item(key = "appearance_theme") {
        ChoiceRow(
            label = stringResource(R.string.theme),
            options = ThemeMode.entries.map {
                stringResource(
                    when (it) {
                        ThemeMode.DARK -> R.string.theme_dark
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.AUTO -> R.string.theme_auto
                    },
                )
            },
            selectedIndex = ThemeMode.entries.indexOf(settings.theme),
            onSelect = { index -> vm.updateSettings { it.copy(theme = ThemeMode.entries[index]) } },
        )
    }
    item(key = "appearance_accent") {
        ToggleRow(
            label = stringResource(R.string.accent_dynamic),
            checked = settings.accentMode == com.vitrum.tv.data.AccentMode.DYNAMIC,
            onToggle = {
                if (settings.accentMode == com.vitrum.tv.data.AccentMode.DYNAMIC) {
                    vm.setAccent(settings.accentArgb)
                } else {
                    vm.setAccentDynamic()
                }
            },
        )
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccentPalette.forEach { choice ->
                ColorSwatch(
                    label = null,
                    argb = choice.argb,
                    selected = settings.accentMode == com.vitrum.tv.data.AccentMode.CUSTOM &&
                        settings.accentArgb == choice.argb,
                    onClick = { vm.setAccent(choice.argb) },
                )
            }
        }
        SettingsHint(stringResource(R.string.accent_color))
    }
    item(key = "appearance_background") {
        SettingsSectionTitle(stringResource(R.string.background))
        BackgroundChooser(
            presets = BackgroundPresets,
            selectedId = settings.backgroundId,
            onSelectPreset = vm::setBackground,
            onSelectCustom = {
                if (settings.customBackgroundUrl.isBlank()) {
                    onDialog(SettingsDialog.BackgroundUrl)
                } else {
                    vm.setBackground("custom")
                }
            },
        )
        if (settings.backgroundId == "custom") {
            ActionRow(
                label = stringResource(R.string.custom_url),
                value = settings.customBackgroundUrl.ifBlank { "—" },
                onClick = { onDialog(SettingsDialog.BackgroundUrl) },
            )
        }
    }
    item(key = "appearance_blur") {
        StepperRow(
            label = stringResource(R.string.blur_strength),
            value = "${settings.blurStrength}",
            onMinus = { vm.updateSettings { it.copy(blurStrength = (it.blurStrength - 10).coerceAtLeast(0)) } },
            onPlus = { vm.updateSettings { it.copy(blurStrength = (it.blurStrength + 10).coerceAtMost(100)) } },
            minusEnabled = settings.blurStrength > 0,
            plusEnabled = settings.blurStrength < 100,
        )
        StepperRow(
            label = stringResource(R.string.glass_opacity),
            value = "${settings.glassOpacity}",
            onMinus = { vm.updateSettings { it.copy(glassOpacity = (it.glassOpacity - 2).coerceAtLeast(0)) } },
            onPlus = { vm.updateSettings { it.copy(glassOpacity = (it.glassOpacity + 2).coerceAtMost(30)) } },
            minusEnabled = settings.glassOpacity > 0,
            plusEnabled = settings.glassOpacity < 30,
        )
        StepperRow(
            label = stringResource(R.string.corner_radius),
            value = "${settings.cornerRadius}",
            onMinus = { vm.updateSettings { it.copy(cornerRadius = (it.cornerRadius - 4).coerceAtLeast(0)) } },
            onPlus = { vm.updateSettings { it.copy(cornerRadius = (it.cornerRadius + 4).coerceAtMost(48)) } },
            minusEnabled = settings.cornerRadius > 0,
            plusEnabled = settings.cornerRadius < 48,
        )
    }
    item(key = "appearance_motion") {
        ChoiceRow(
            label = stringResource(R.string.animations),
            options = AnimationMode.entries.map {
                stringResource(
                    when (it) {
                        AnimationMode.NORMAL -> R.string.anim_normal
                        AnimationMode.REDUCED -> R.string.anim_reduced
                        AnimationMode.OFF -> R.string.anim_off
                    },
                )
            },
            selectedIndex = AnimationMode.entries.indexOf(settings.animations),
            onSelect = { index -> vm.updateSettings { it.copy(animations = AnimationMode.entries[index]) } },
        )
        ChoiceRow(
            label = stringResource(R.string.quality),
            description = stringResource(R.string.quality_auto_desc),
            options = EffectsQuality.entries.map {
                stringResource(
                    when (it) {
                        EffectsQuality.AUTO -> R.string.quality_auto
                        EffectsQuality.HIGH -> R.string.quality_high
                        EffectsQuality.MEDIUM -> R.string.quality_medium
                        EffectsQuality.LOW -> R.string.quality_low
                    },
                )
            },
            selectedIndex = EffectsQuality.entries.indexOf(settings.effectsQuality),
            onSelect = { index -> vm.updateSettings { it.copy(effectsQuality = EffectsQuality.entries[index]) } },
        )
        ChoiceRow(
            label = stringResource(R.string.font_size),
            options = TextSizeOption.entries.map {
                stringResource(
                    when (it) {
                        TextSizeOption.SMALL -> R.string.font_small
                        TextSizeOption.NORMAL -> R.string.font_normal
                        TextSizeOption.LARGE -> R.string.font_large
                    },
                )
            },
            selectedIndex = TextSizeOption.entries.indexOf(settings.textSize),
            onSelect = { index -> vm.updateSettings { it.copy(textSize = TextSizeOption.entries[index]) } },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.layoutSection(
    settings: Settings,
    vm: MainViewModel,
    onPreviewOverscan: () -> Unit,
) {
    item(key = "layout_tiles") {
        ChoiceRow(
            label = stringResource(R.string.tile_size),
            options = listOf(
                stringResource(R.string.size_small),
                stringResource(R.string.size_medium),
                stringResource(R.string.size_large),
            ),
            selectedIndex = TileSize.entries.indexOf(settings.tileSize),
            onSelect = { index -> vm.updateSettings { it.copy(tileSize = TileSize.entries[index]) } },
        )
        ChoiceRow(
            label = stringResource(R.string.tile_style),
            options = listOf(
                stringResource(R.string.style_banner),
                stringResource(R.string.style_icon),
                stringResource(R.string.style_icon_only),
            ),
            selectedIndex = TileStyle.entries.indexOf(settings.tileStyle),
            onSelect = { index -> vm.updateSettings { it.copy(tileStyle = TileStyle.entries[index]) } },
        )
        StepperRow(
            label = stringResource(R.string.rows_visible),
            value = "${settings.visibleRows}",
            onMinus = { vm.updateSettings { it.copy(visibleRows = (it.visibleRows - 1).coerceAtLeast(1)) } },
            onPlus = { vm.updateSettings { it.copy(visibleRows = (it.visibleRows + 1).coerceAtMost(6)) } },
            minusEnabled = settings.visibleRows > 1,
            plusEnabled = settings.visibleRows < 6,
        )
        ToggleRow(
            label = stringResource(R.string.show_names),
            checked = settings.showAppNames,
            onToggle = { vm.updateSettings { it.copy(showAppNames = !it.showAppNames) } },
        )
    }
    item(key = "layout_rows_title") {
        SettingsSectionTitle(stringResource(R.string.row_order))
        SettingsHint(stringResource(R.string.row_visible))
    }
    val allKinds = RowKind.entries
    val ordered = settings.rowOrder + allKinds.filterNot { it in settings.rowOrder }
    items(ordered, key = { "row_${it.name}" }) { kind ->
        val visible = kind in settings.rowOrder
        val index = settings.rowOrder.indexOf(kind)
        RowOrderRow(
            label = stringResource(rowKindLabelRes(kind)),
            visible = visible,
            canUp = visible && index > 0,
            canDown = visible && index in 0 until settings.rowOrder.size - 1,
            onToggle = {
                vm.updateSettings { current ->
                    if (kind in current.rowOrder) {
                        current.copy(rowOrder = current.rowOrder - kind)
                    } else {
                        current.copy(rowOrder = current.rowOrder + kind)
                    }
                }
            },
            onUp = {
                vm.updateSettings { current ->
                    val list = current.rowOrder.toMutableList()
                    val i = list.indexOf(kind)
                    if (i > 0) {
                        val item = list.removeAt(i)
                        list.add(i - 1, item)
                    }
                    current.copy(rowOrder = list)
                }
            },
            onDown = {
                vm.updateSettings { current ->
                    val list = current.rowOrder.toMutableList()
                    val i = list.indexOf(kind)
                    if (i >= 0 && i < list.lastIndex) {
                        val item = list.removeAt(i)
                        list.add(i + 1, item)
                    }
                    current.copy(rowOrder = list)
                }
            },
        )
    }
    item(key = "layout_more") {
        StepperRow(
            label = stringResource(R.string.ui_scale),
            value = "${(settings.uiScale * 100).roundToInt()}%",
            onMinus = { vm.updateSettings { it.copy(uiScale = (it.uiScale - 0.05f).coerceAtLeast(0.8f)) } },
            onPlus = { vm.updateSettings { it.copy(uiScale = (it.uiScale + 0.05f).coerceAtMost(1.3f)) } },
            minusEnabled = settings.uiScale > 0.8f,
            plusEnabled = settings.uiScale < 1.3f,
        )
        StepperRow(
            label = stringResource(R.string.overscan),
            value = "${settings.overscanMargin}%",
            onMinus = { vm.updateSettings { it.copy(overscanMargin = (it.overscanMargin - 1).coerceAtLeast(0)) } },
            onPlus = { vm.updateSettings { it.copy(overscanMargin = (it.overscanMargin + 1).coerceAtMost(10)) } },
            minusEnabled = settings.overscanMargin > 0,
            plusEnabled = settings.overscanMargin < 10,
        )
        ActionRow(
            label = stringResource(R.string.overscan_preview),
            description = stringResource(R.string.overscan_preview_hint),
            onClick = onPreviewOverscan,
        )
        ActionRow(
            label = stringResource(R.string.reset_rows),
            onClick = vm::resetRows,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appsSection(
    settings: Settings,
    apps: List<com.vitrum.tv.data.AppInfo>,
    onDialog: (SettingsDialog) -> Unit,
    vm: MainViewModel,
) {
    item(key = "apps_fav") {
        ActionRow(
            label = stringResource(R.string.manage_favorites),
            description = stringResource(R.string.manage_favorites_desc),
            value = "${settings.favorites.size}",
            onClick = { onDialog(SettingsDialog.Favorites) },
        )
        ActionRow(
            label = stringResource(R.string.default_sort),
            value = when (settings.sortOrder) {
                SortOrder.AZ -> stringResource(R.string.sort_az)
                SortOrder.RECENT -> stringResource(R.string.sort_recent)
                SortOrder.MANUAL -> stringResource(R.string.sort_manual)
            },
            onClick = {
                val next = SortOrder.entries[(SortOrder.entries.indexOf(settings.sortOrder) + 1) % SortOrder.entries.size]
                vm.setSort(next)
            },
        )
        ToggleRow(
            label = stringResource(R.string.show_system_apps),
            checked = settings.showSystemApps,
            onToggle = { vm.updateSettings { it.copy(showSystemApps = !it.showSystemApps) } },
        )
    }
    item(key = "apps_custom_title") {
        SettingsSectionTitle(stringResource(R.string.custom_rows))
    }
    if (settings.customRows.isEmpty()) {
        item(key = "apps_no_rows") {
            SettingsHint(stringResource(R.string.row_name))
            ActionRow(
                label = stringResource(R.string.new_row),
                onClick = { onDialog(SettingsDialog.NewRow) },
            )
        }
    } else {
        items(settings.customRows, key = { "custom_${it.id}" }) { row ->
            CustomRowEditor(
                row = row,
                onEdit = { onDialog(SettingsDialog.RowApps(row)) },
                onDelete = { onDialog(SettingsDialog.DeleteRow(row)) },
            )
        }
        item(key = "apps_new_row") {
            ActionRow(
                label = stringResource(R.string.new_row),
                onClick = { onDialog(SettingsDialog.NewRow) },
            )
        }
    }
    item(key = "apps_hidden_title") {
        SettingsSectionTitle(stringResource(R.string.hidden_apps))
        SettingsHint(stringResource(R.string.hidden_apps_desc))
    }
    val hiddenApps = settings.hiddenApps.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    if (hiddenApps.isEmpty()) {
        item(key = "apps_no_hidden") {
            SettingsHint(stringResource(R.string.no_hidden_apps))
        }
    } else {
        items(hiddenApps, key = { "hidden_${it.packageName}" }) { app ->
            ActionRow(
                label = app.label,
                value = stringResource(R.string.restore),
                onClick = { vm.restoreHidden(app.packageName) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.clockSection(
    settings: Settings,
    vm: MainViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onDialog: (SettingsDialog) -> Unit,
) {
    item(key = "clock") {
        ToggleRow(
            label = stringResource(R.string.use_24h),
            checked = settings.clock24h,
            onToggle = { vm.updateSettings { it.copy(clock24h = !it.clock24h) } },
        )
        ToggleRow(
            label = stringResource(R.string.show_seconds),
            checked = settings.showSeconds,
            onToggle = { vm.updateSettings { it.copy(showSeconds = !it.showSeconds) } },
        )
        ToggleRow(
            label = stringResource(R.string.show_date),
            checked = settings.showDate,
            onToggle = { vm.updateSettings { it.copy(showDate = !it.showDate) } },
        )
        ToggleRow(
            label = stringResource(R.string.weather_enabled),
            checked = settings.weatherEnabled,
            onToggle = { vm.updateSettings { it.copy(weatherEnabled = !it.weatherEnabled) } },
        )
        ActionRow(
            label = stringResource(R.string.city),
            value = settings.weatherCity.ifBlank { stringResource(R.string.weather_setup) },
            description = settings.weatherCity.ifBlank { stringResource(R.string.city_search_hint) },
            onClick = { onDialog(SettingsDialog.City) },
        )
        ChoiceRow(
            label = stringResource(R.string.celsius),
            options = listOf(
                stringResource(R.string.celsius),
                stringResource(R.string.fahrenheit),
            ),
            selectedIndex = if (settings.useCelsius) 0 else 1,
            onSelect = { index -> vm.updateSettings { it.copy(useCelsius = index == 0) } },
        )
        SettingsHint(stringResource(R.string.weather_powered))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.behaviorSection(
    settings: Settings,
    vm: MainViewModel,
) {
    item(key = "behavior") {
        ChoiceRow(
            label = stringResource(R.string.home_behavior),
            options = listOf(
                stringResource(R.string.home_top),
                stringResource(R.string.home_keep),
            ),
            selectedIndex = if (settings.homeBehavior == HomeBehavior.TOP) 0 else 1,
            onSelect = { index ->
                vm.updateSettings {
                    it.copy(homeBehavior = if (index == 0) HomeBehavior.TOP else HomeBehavior.KEEP)
                }
            },
        )
        ToggleRow(
            label = stringResource(R.string.sounds),
            description = stringResource(R.string.sounds_desc),
            checked = settings.soundsEnabled,
            onToggle = { vm.updateSettings { it.copy(soundsEnabled = !it.soundsEnabled) } },
        )
        ToggleRow(
            label = stringResource(R.string.confirm_uninstall),
            checked = settings.confirmUninstall,
            onToggle = { vm.updateSettings { it.copy(confirmUninstall = !it.confirmUninstall) } },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.systemSection(
    vm: MainViewModel,
    context: Context,
    settings: Settings,
) {
    item(key = "system_launcher") {
        ActionRow(
            label = stringResource(R.string.launcher_setup),
            description = stringResource(R.string.launcher_setup_desc),
            onClick = vm::openLauncherGuide,
        )
        InfoRow(
            label = stringResource(R.string.cat_system),
            value = stringResource(
                when (SystemUtils.deviceKind(context)) {
                    com.vitrum.tv.util.DeviceKind.FIRE_TV -> R.string.fire_tv_detect
                    com.vitrum.tv.util.DeviceKind.ANDROID_TV -> R.string.android_tv_detect
                    else -> R.string.other_tv_detect
                },
            ),
        )
        systemShortcutList(context).forEach { shortcut ->
            ActionRow(
                label = stringResource(shortcut.labelRes),
                onClick = { vm.openSystemAction(shortcut.action, shortcut.fallback) },
            )
        }
        ActionRow(
            label = stringResource(R.string.device_info),
            value = "${Build.MANUFACTURER} ${Build.MODEL}",
            onClick = { vm.openSystemAction(SystemSettings.ACTION_DEVICE_INFO_SETTINGS) },
        )
        InfoRow(label = "Android", value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        val hdmi = TvInputs.hdmiInputs(context)
        if (hdmi.isNotEmpty()) {
            SettingsSectionTitle(stringResource(R.string.hdmi_inputs))
            hdmi.forEach { input ->
                ActionRow(
                    label = input.label,
                    onClick = {
                        runCatching { context.startActivity(input.intent) }
                            .onFailure { vm.toast(R.string.action_failed) }
                    },
                )
            }
        }
        if (SystemUtils.deviceKind(context) == com.vitrum.tv.util.DeviceKind.FIRE_TV) {
            SettingsHint(stringResource(R.string.fire_tv_body))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.backupSection(
    vm: MainViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
) {
    item(key = "backup") {
        ActionRow(label = stringResource(R.string.export_settings), onClick = onExport)
        ActionRow(label = stringResource(R.string.import_settings), onClick = onImport)
        ActionRow(
            label = stringResource(R.string.reset_defaults),
            onClick = onReset,
        )
        SettingsHint(stringResource(R.string.picker_unavailable))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.aboutSection(
    vm: MainViewModel,
    settings: Settings,
    otaState: OtaState,
    permission: Boolean,
    onDialog: (SettingsDialog) -> Unit,
) {
    val repoUrl = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"
    item(key = "about_info") {
        InfoRow(stringResource(R.string.version_label), BuildConfig.VERSION_NAME)
        InfoRow(stringResource(R.string.build_label), "${BuildConfig.VERSION_CODE}")
        InfoRow(
            stringResource(R.string.channel_label),
            stringResource(
                if (settings.updateChannel == UpdateChannel.STABLE) R.string.channel_stable
                else R.string.channel_beta,
            ),
        )
        ActionRow(
            label = stringResource(R.string.repository),
            value = "${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}",
            onClick = { vm.openUrl(repoUrl) },
        )
        val qrState by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, repoUrl) {
            value = kotlinx.coroutines.withContext(Dispatchers.Default) {
                com.vitrum.tv.util.QrCode.generate(repoUrl, 420)?.asImageBitmap()
            }
        }
        val qrImage = qrState
        if (qrImage != null) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Image(
                    bitmap = qrImage,
                    contentDescription = stringResource(R.string.qr_repo),
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .padding(10.dp),
                )
                SettingsHint(stringResource(R.string.qr_repo))
            }
        }
    }
    item(key = "about_licenses") {
        SettingsSectionTitle(stringResource(R.string.licenses))
        SettingsHint(stringResource(R.string.licenses_body))
    }
    updatesSection(vm, settings, otaState, permission)
}

@Composable
private fun CustomRowEditor(
    row: CustomRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Row(
        Modifier.fillMaxWidth().height(54.dp).bringIntoViewOnFocus(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FocusableGlass(
            onClick = onEdit,
            modifier = Modifier.weight(1f).height(50.dp),
            allowRealBlur = false,
            focusedScale = 1.01f,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) { focused ->
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.name,
                        style = type.body,
                        color = if (focused) colors.accent else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(R.string.select_apps),
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                }
                Text("${row.packages.size}", style = type.label, color = colors.textSecondary)
            }
        }
        GlassIconButton(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.delete_row),
            onClick = onDelete,
            buttonSize = 42.dp,
        )
    }
}

private data class SystemShortcut(val labelRes: Int, val action: String, val fallback: String? = null)

private fun systemShortcutList(context: Context): List<SystemShortcut> {
    val candidates = listOf(
        SystemShortcut(R.string.open_wifi, SystemSettings.ACTION_WIFI_SETTINGS, SystemSettings.ACTION_WIRELESS_SETTINGS),
        SystemShortcut(R.string.open_bluetooth, SystemSettings.ACTION_BLUETOOTH_SETTINGS),
        SystemShortcut(R.string.open_display, SystemSettings.ACTION_DISPLAY_SETTINGS),
        SystemShortcut(R.string.open_sound, SystemSettings.ACTION_SOUND_SETTINGS),
        SystemShortcut(R.string.open_network, SystemSettings.ACTION_WIRELESS_SETTINGS),
        SystemShortcut(R.string.open_settings, SystemSettings.ACTION_SETTINGS),
    )
    return candidates.filter { shortcut ->
        SystemUtils.canResolve(context, Intent(shortcut.action)) ||
            (shortcut.fallback != null && SystemUtils.canResolve(context, Intent(shortcut.fallback)))
    }
}

private fun rowKindLabelRes(kind: RowKind): Int = when (kind) {
    RowKind.FAVORITES -> R.string.row_favorites
    RowKind.RECENT -> R.string.row_recent
    RowKind.ALL_APPS -> R.string.row_all
    RowKind.SHORTCUTS -> R.string.row_shortcuts
}

@Composable
private fun OverscanPreviewDialog(margin: Int, onDismiss: () -> Unit) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { closeFocus.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val h = maxWidth * (margin / 100f)
                val v = maxHeight * (margin / 100f)
                Canvas(Modifier.fillMaxSize()) {
                    val left = h.toPx()
                    val top = v.toPx()
                    drawRoundRect(
                        color = colors.accent.copy(alpha = 0.9f),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - left * 2,
                            size.height - top * 2,
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                        ),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(30.dp),
            ) {
                Text(
                    stringResource(R.string.overscan_preview),
                    style = type.headline,
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(R.string.overscan_preview_hint),
                    style = type.body,
                    color = colors.textSecondary,
                )
                GlassButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    primary = true,
                    modifier = Modifier.focusRequester(closeFocus),
                )
            }
        }
    }
}
