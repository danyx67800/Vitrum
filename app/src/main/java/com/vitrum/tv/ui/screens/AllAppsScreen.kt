package com.vitrum.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.core.AppLogic
import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.SortOrder
import com.vitrum.tv.data.TileStyle
import com.vitrum.tv.ui.MainViewModel
import com.vitrum.tv.ui.components.AppContextMenu
import com.vitrum.tv.ui.components.AppTile
import com.vitrum.tv.ui.components.ConfirmDialog
import com.vitrum.tv.ui.components.FocusableGlass
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.components.GlassIconButton
import com.vitrum.tv.ui.components.TextEntryDialog
import com.vitrum.tv.ui.components.TvKeyboard
import com.vitrum.tv.ui.components.tileMetrics
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import kotlinx.coroutines.delay

@Composable
fun AllAppsScreen(
    vm: MainViewModel,
    settings: Settings,
    focusSearchOnOpen: Boolean,
) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loading by vm.appsLoading.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    var query by remember { mutableStateOf("") }
    var keyboardOpen by remember { mutableStateOf(focusSearchOnOpen) }
    var shift by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }
    var uninstallApp by remember { mutableStateOf<AppInfo?>(null) }
    val gridState = rememberLazyGridState()
    val registry = remember { mutableMapOf<String, FocusRequester>() }
    val searchPillFocus = remember { FocusRequester() }
    val keyboardFirstKey = remember { FocusRequester() }
    val doneFocus = remember { FocusRequester() }
    var lastFocusedPackage by remember { mutableStateOf(vm.lastAllAppsPackage.value) }

    val visibleApps = remember(apps, settings.hiddenApps, settings.showSystemApps, settings.sortOrder, settings.manualOrder, settings.recentApps, query) {
        AppLogic.sorted(
            AppLogic.filter(
                apps = apps,
                query = query,
                hidden = settings.hiddenApps.toSet(),
                showSystem = settings.showSystemApps,
            ),
            settings.sortOrder,
            settings.recentApps,
            settings.manualOrder,
        )
    }
    val metrics = tileMetrics(settings)
    val columnsMinSize = when (settings.tileStyle) {
        TileStyle.BANNER -> metrics.width + 22.dp
        else -> metrics.width + 18.dp
    }

    BackHandler {
        if (keyboardOpen) {
            keyboardOpen = false
            runCatching { searchPillFocus.requestFocus() }
        } else {
            vm.goHome()
        }
    }

    LaunchedEffect(keyboardOpen) {
        withFrameNanos { }
        withFrameNanos { }
        delay(50)
        if (keyboardOpen) {
            runCatching { keyboardFirstKey.requestFocus() }
        }
    }

    LaunchedEffect(apps.size) {
        withFrameNanos { }
        withFrameNanos { }
        delay(60)
        if (!keyboardOpen && visibleApps.isNotEmpty()) {
            val restore = lastFocusedPackage
            val index = visibleApps.indexOfFirst { it.packageName == restore }
            if (index >= 0) {
                runCatching { gridState.scrollToItem(index) }
                delay(60)
                registry[restore]?.let { runCatching { it.requestFocus() } }
            } else {
                registry[visibleApps.first().packageName]?.let { runCatching { it.requestFocus() } }
            }
        }
    }

    LaunchedEffect(apps) {
        val selected = menuApp
        if (selected != null && apps.none { it.packageName == selected.packageName }) {
            menuApp = null
            vm.toast(R.string.app_unavailable)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassIconButton(
                    icon = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.back),
                    onClick = {
                        if (keyboardOpen) {
                            keyboardOpen = false
                        } else {
                            vm.goHome()
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.all_apps),
                    style = type.headline,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                FocusableGlass(
                    onClick = { keyboardOpen = true },
                    modifier = Modifier
                        .width(320.dp)
                        .height(46.dp)
                        .focusRequester(searchPillFocus),
                    allowRealBlur = false,
                    focusedScale = 1.02f,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) { focused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painter = rememberVectorPainter(Icons.Filled.Search),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (focused) colors.accent else colors.textSecondary),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = query.ifBlank { stringResource(R.string.search_hint) },
                            style = type.body,
                            color = if (query.isBlank()) colors.textSecondary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.apps_count, visibleApps.size),
                    style = type.caption,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip(stringResource(R.string.sort_az), settings.sortOrder == SortOrder.AZ) {
                    vm.setSort(SortOrder.AZ)
                }
                SortChip(stringResource(R.string.sort_recent), settings.sortOrder == SortOrder.RECENT) {
                    vm.setSort(SortOrder.RECENT)
                }
                SortChip(stringResource(R.string.sort_manual), settings.sortOrder == SortOrder.MANUAL) {
                    vm.setSort(SortOrder.MANUAL)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (loading && apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.loading), style = type.title, color = colors.textSecondary)
                }
            } else if (visibleApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_results), style = type.title, color = colors.textSecondary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = columnsMinSize),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = if (keyboardOpen) 230.dp else 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        val requester = remember(app.packageName) { FocusRequester() }
                        SideEffect { registry[app.packageName] = requester }
                        DisposableEffect(app.packageName) {
                            onDispose { registry.remove(app.packageName) }
                        }
                        AppTile(
                            app = app,
                            settings = settings,
                            onClick = { vm.launchApp(app) },
                            onMenu = { menuApp = app },
                            onFocused = {
                                if (it) {
                                    lastFocusedPackage = app.packageName
                                    vm.lastAllAppsPackage.value = app.packageName
                                }
                            },
                            modifier = Modifier.focusRequester(requester),
                        )
                    }
                }
            }
        }

        if (keyboardOpen) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 20.dp),
            ) {
                GlassPanel(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    allowRealBlur = true,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = query.ifBlank { stringResource(R.string.search_hint) },
                            style = type.title,
                            color = if (query.isBlank()) colors.textSecondary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        GlassButton(
                            text = stringResource(R.string.done),
                            onClick = {
                                keyboardOpen = false
                                runCatching { searchPillFocus.requestFocus() }
                            },
                            primary = true,
                            compact = true,
                            modifier = Modifier.focusRequester(doneFocus),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    TvKeyboard(
                        shift = shift,
                        onShift = { shift = !shift },
                        firstKeyFocus = keyboardFirstKey,
                        onKey = { key ->
                            query += if (shift) key.uppercase() else key
                            if (shift) shift = false
                        },
                        onBackspace = { query = query.dropLast(1) },
                        onClear = { query = "" },
                        onSpace = { query += " " },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.keyboard_hint),
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }

    val selectedApp = menuApp
    if (selectedApp != null) {
        AppContextMenu(
            app = selectedApp,
            settings = settings,
            customRows = settings.customRows,
            onDismiss = { menuApp = null },
            onOpen = {
                menuApp = null
                vm.launchApp(selectedApp)
            },
            onToggleFavorite = {
                vm.toggleFavorite(selectedApp.packageName)
                menuApp = null
            },
            onMoveLeft = {
                if (selectedApp.packageName in settings.favorites) {
                    vm.moveFavorite(selectedApp.packageName, -1)
                } else {
                    vm.moveManual(selectedApp.packageName, -1)
                }
            },
            onMoveRight = {
                if (selectedApp.packageName in settings.favorites) {
                    vm.moveFavorite(selectedApp.packageName, 1)
                } else {
                    vm.moveManual(selectedApp.packageName, 1)
                }
            },
            onAddToRow = { row ->
                vm.addToCustomRow(row.id, selectedApp.packageName)
                vm.toast(context.getString(R.string.row_added, row.name))
            },
            onRename = {
                menuApp = null
                renameApp = selectedApp
            },
            onHide = {
                menuApp = null
                vm.hideApp(selectedApp.packageName)
            },
            onAppInfo = {
                menuApp = null
                vm.openAppInfo(selectedApp)
            },
            onUninstall = {
                menuApp = null
                if (settings.confirmUninstall) uninstallApp = selectedApp else vm.uninstallApp(selectedApp)
            },
        )
    }

    renameApp?.let { app ->
        TextEntryDialog(
            title = stringResource(R.string.rename_title, app.label),
            label = stringResource(R.string.rename_hint),
            initialValue = settings.customLabels[app.packageName] ?: "",
            hint = app.label,
            onConfirm = { value ->
                vm.renameApp(app.packageName, value)
                renameApp = null
            },
            onDismiss = { renameApp = null },
        )
    }

    uninstallApp?.let { app ->
        ConfirmDialog(
            title = stringResource(R.string.uninstall_confirm_title, app.label),
            message = stringResource(R.string.uninstall_confirm_body),
            confirmLabel = stringResource(R.string.uninstall),
            destructive = true,
            onConfirm = {
                vm.uninstallApp(app)
                uninstallApp = null
            },
            onDismiss = { uninstallApp = null },
        )
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        focusedScale = 1.04f,
        allowRealBlur = false,
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) { focused ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = type.label,
                color = when {
                    selected -> colors.accent
                    focused -> colors.textPrimary
                    else -> colors.textSecondary
                },
                maxLines = 1,
            )
        }
    }
}
