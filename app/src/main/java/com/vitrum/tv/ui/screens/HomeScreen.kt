package com.vitrum.tv.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.core.AppLogic
import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.CustomRow
import com.vitrum.tv.data.RowKind
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.SortOrder
import com.vitrum.tv.ota.OtaState
import com.vitrum.tv.ui.MainViewModel
import com.vitrum.tv.ui.components.AppContextMenu
import com.vitrum.tv.ui.components.AppTile
import com.vitrum.tv.ui.components.ConfirmDialog
import com.vitrum.tv.ui.components.FocusableGlass
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.components.TextEntryDialog
import com.vitrum.tv.ui.components.TopBar
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.util.HdmiInput
import com.vitrum.tv.util.SystemUtils
import com.vitrum.tv.util.TvInputs
import kotlinx.coroutines.delay

private sealed interface HomeEntry {
    val key: String
    data class AppRow(
        override val key: String,
        val title: String,
        val apps: List<AppInfo>,
        val seeAll: Boolean = false,
        val customRow: CustomRow? = null,
    ) : HomeEntry

    data class Shortcuts(override val key: String = "shortcuts") : HomeEntry
}

private enum class ShortcutIcon { WIFI, BLUETOOTH, DISPLAY, SOUND, NETWORK, SETTINGS, INFO, HDMI, HOME }

private data class ShortcutItem(
    val id: String,
    val label: String,
    val icon: ShortcutIcon,
    val intent: Intent? = null,
    val special: (() -> Unit)? = null,
)

private class FocusRegistry {
    private val requesters = mutableMapOf<String, FocusRequester>()
    fun register(key: String, requester: FocusRequester) { requesters[key] = requester }
    fun unregister(key: String) { requesters.remove(key) }
    fun request(key: String?): Boolean {
        if (key == null) return false
        val requester = requesters[key] ?: return false
        return runCatching { requester.requestFocus() }.isSuccess
    }
}

@Composable
fun HomeScreen(
    vm: MainViewModel,
    settings: Settings,
    listState: LazyListState,
    restoreKey: String?,
) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loading by vm.appsLoading.collectAsStateWithLifecycle()
    val network by vm.network.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val otaState by vm.otaState.collectAsStateWithLifecycle()
    val resetTick by vm.homeResetTick.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusRegistry = remember { FocusRegistry() }
    val searchFocus = remember { FocusRequester() }
    val initialRestore = remember { restoreKey }
    var handledReset by remember { mutableIntStateOf(resetTick) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }
    var uninstallApp by remember { mutableStateOf<AppInfo?>(null) }

    val installed = remember(apps, settings.hiddenApps) {
        apps.filter { it.packageName !in settings.hiddenApps.toSet() }
    }
    val filtered = remember(installed, settings.showSystemApps, settings.sortOrder, settings.manualOrder, settings.recentApps) {
        AppLogic.sorted(
            AppLogic.filter(installed, showSystem = settings.showSystemApps),
            settings.sortOrder,
            settings.recentApps,
            settings.manualOrder,
        )
    }
    val favorites = remember(filtered, settings.favorites) {
        settings.favorites.mapNotNull { pkg -> filtered.firstOrNull { it.packageName == pkg } }
    }
    val recent = remember(filtered, settings.recentApps) {
        settings.recentApps.mapNotNull { pkg -> filtered.firstOrNull { it.packageName == pkg } }.take(18)
    }
    val shortcuts = remember(context) { buildShortcuts(context) }

    val entries = remember(settings.rowOrder, settings.visibleRows, favorites, recent, filtered, settings.customRows, shortcuts) {
        val list = mutableListOf<HomeEntry>()
        for (kind in settings.rowOrder) {
            when (kind) {
                RowKind.FAVORITES -> if (favorites.isNotEmpty()) {
                    list += HomeEntry.AppRow("favorites", context.getString(R.string.favorites), favorites)
                }
                RowKind.RECENT -> if (recent.isNotEmpty()) {
                    list += HomeEntry.AppRow("recent", context.getString(R.string.recent), recent)
                }
                RowKind.ALL_APPS -> if (filtered.isNotEmpty()) {
                    list += HomeEntry.AppRow(
                        key = "all",
                        title = context.getString(R.string.all_apps),
                        apps = filtered.take(18),
                        seeAll = true,
                    )
                }
                RowKind.SHORTCUTS -> if (shortcuts.isNotEmpty()) {
                    list += HomeEntry.Shortcuts()
                }
            }
        }
        settings.customRows.forEach { row ->
            val rowApps = row.packages.mapNotNull { pkg -> filtered.firstOrNull { it.packageName == pkg } }
            list += HomeEntry.AppRow("custom:${row.id}", row.name, rowApps, customRow = row)
        }
        list.take(settings.visibleRows.coerceAtLeast(1))
    }

    val firstFocusKey = remember(entries) {
        entries.firstNotNullOfOrNull { entry ->
            when (entry) {
                is HomeEntry.AppRow -> when {
                    entry.apps.isNotEmpty() -> "${entry.key}/${entry.apps.first().packageName}"
                    entry.seeAll -> "${entry.key}/__all"
                    else -> null
                }
                is HomeEntry.Shortcuts -> shortcuts.firstOrNull()?.let { "shortcuts/${it.id}" }
            }
        }
    }

    LaunchedEffect(resetTick, entries.size, filtered.size, firstFocusKey) {
        withFrameNanos { }
        withFrameNanos { }
        delay(40)
        val target = if (handledReset != resetTick) {
            handledReset = resetTick
            runCatching { listState.scrollToItem(0) }
            firstFocusKey
        } else {
            initialRestore ?: firstFocusKey
        }
        // A lazy row can attach its FocusRequester a few frames after the data lands.
        // Request more than once: a request that lands before the window regains focus
        // is silently lost on some TV firmwares.
        repeat(5) { attempt ->
            if (focusRegistry.request(target)) {
                if (attempt == 0) {
                    delay(180)
                    focusRegistry.request(target)
                }
                return@LaunchedEffect
            }
            delay(if (attempt == 0) 40 else 140)
        }
        // Never leave the home without a visible focus ring: search is always reachable.
        repeat(3) {
            runCatching { searchFocus.requestFocus() }
            delay(160)
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            TopBar(
                settings = settings,
                network = network,
                weather = weather,
                updateAvailable = otaState is OtaState.Available,
                onSearch = { vm.openAllApps(focusSearch = true) },
                onSettings = { vm.openSettings() },
                searchFocusRequester = searchFocus,
            )
            Spacer(Modifier.height(8.dp))
            if (loading && apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.loading),
                        style = LocalVitrumType.current.title,
                        color = LocalVitrumColors.current.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 46.dp),
                ) {
                    items(entries, key = { it.key }) { entry ->
                        when (entry) {
                            is HomeEntry.AppRow -> {
                                RowHeader(entry.title)
                                AppRow(
                                    entry = entry,
                                    settings = settings,
                                    registry = focusRegistry,
                                    autoFocusKey = if (initialRestore == null) firstFocusKey else null,
                                    onFocusKey = { vm.lastHomeFocusKey.value = it },
                                    onOpen = vm::launchApp,
                                    onMenu = { menuApp = it },
                                    onSeeAll = { vm.openAllApps() },
                                )
                            }
                            is HomeEntry.Shortcuts -> {
                                RowHeader(stringResource(R.string.shortcuts))
                                ShortcutsRow(
                                    shortcuts = shortcuts,
                                    registry = focusRegistry,
                                    autoFocusKey = if (initialRestore == null) firstFocusKey else null,
                                    onFocusKey = { vm.lastHomeFocusKey.value = it },
                                    onAction = { shortcut ->
                                        if (shortcut.id == "launcher") {
                                            vm.openLauncherGuide()
                                        } else {
                                            val special = shortcut.special
                                            if (special != null) {
                                                special.invoke()
                                            } else if (shortcut.intent != null) {
                                                runCatching { context.startActivity(shortcut.intent) }
                                                    .onFailure { vm.toast(R.string.action_failed) }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.no_apps),
                                style = LocalVitrumType.current.body,
                                color = LocalVitrumColors.current.textSecondary,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
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
                if (settings.confirmUninstall) {
                    uninstallApp = selectedApp
                } else {
                    vm.uninstallApp(selectedApp)
                }
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
private fun RowHeader(title: String) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Text(
        text = title,
        style = type.title,
        color = colors.textPrimary,
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AppRow(
    entry: HomeEntry.AppRow,
    settings: Settings,
    registry: FocusRegistry,
    autoFocusKey: String?,
    onFocusKey: (String) -> Unit,
    onOpen: (AppInfo) -> Unit,
    onMenu: (AppInfo) -> Unit,
    onSeeAll: () -> Unit,
) {
    val rowState = rememberLazyListState()
    var focusedIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(focusedIndex) {
        val index = focusedIndex
        if (index >= 0) {
            val layout = rowState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
            if (item != null) {
                val target = item.offset - (layout.viewportSize.width - item.size) / 2
                rowState.animateScrollBy(target.toFloat())
            }
        }
    }
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entry.apps, key = { it.packageName }) { app ->
            val focusKey = "${entry.key}/${app.packageName}"
            val requester = remember(focusKey) { FocusRequester() }
            SideEffect { registry.register(focusKey, requester) }
            DisposableEffect(focusKey) {
                onDispose { registry.unregister(focusKey) }
            }
            if (focusKey == autoFocusKey) {
                LaunchedEffect(focusKey) {
                    delay(450)
                    repeat(4) {
                        runCatching { requester.requestFocus() }
                        delay(140)
                    }
                }
            }
            AppTile(
                app = app,
                settings = settings,
                onClick = { onOpen(app) },
                onMenu = { onMenu(app) },
                onFocused = { focused ->
                    if (focused) {
                        focusedIndex = entry.apps.indexOf(app)
                        onFocusKey(focusKey)
                    }
                },
                modifier = Modifier.focusRequester(requester),
            )
        }
        if (entry.seeAll) {
            val focusKey = "${entry.key}/__all"
            item(key = "__all") {
                val requester = remember(focusKey) { FocusRequester() }
                SideEffect { registry.register(focusKey, requester) }
                DisposableEffect(focusKey) {
                    onDispose { registry.unregister(focusKey) }
                }
                AllAppsTile(
                    count = entry.apps.size,
                    onClick = onSeeAll,
                    onFocused = {
                        focusedIndex = entry.apps.size
                        onFocusKey(focusKey)
                    },
                    modifier = Modifier.focusRequester(requester),
                )
            }
        }
    }
}

@Composable
private fun AllAppsTile(
    count: Int,
    onClick: () -> Unit,
    onFocused: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.size(120.dp),
        focusedScale = 1.08f,
        allowRealBlur = false,
        onFocusChanged = onFocused,
    ) { focused ->
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(34.dp)) {
                val cell = size.width * 0.36f
                val gap = size.width * 0.12f
                val start = (size.width - (cell * 2 + gap)) / 2f
                for (row in 0..1) {
                    for (col in 0..1) {
                        drawRoundRect(
                            color = if (focused) colors.accent else colors.textPrimary,
                            topLeft = Offset(start + col * (cell + gap), start + row * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.22f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.all_apps),
                style = type.caption,
                color = if (focused) colors.accent else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ShortcutsRow(
    shortcuts: List<ShortcutItem>,
    registry: FocusRegistry,
    autoFocusKey: String?,
    onFocusKey: (String) -> Unit,
    onAction: (ShortcutItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(shortcuts, key = { it.id }) { shortcut ->
            val focusKey = "shortcuts/${shortcut.id}"
            val requester = remember(focusKey) { FocusRequester() }
            SideEffect { registry.register(focusKey, requester) }
            DisposableEffect(focusKey) {
                onDispose { registry.unregister(focusKey) }
            }
            if (focusKey == autoFocusKey) {
                LaunchedEffect(focusKey) {
                    delay(450)
                    repeat(4) {
                        runCatching { requester.requestFocus() }
                        delay(140)
                    }
                }
            }
            ShortcutTile(
                shortcut = shortcut,
                onClick = { onAction(shortcut) },
                onFocused = {
                    if (it) onFocusKey(focusKey)
                },
                modifier = Modifier.focusRequester(requester),
            )
        }
    }
}

@Composable
private fun ShortcutTile(
    shortcut: ShortcutItem,
    onClick: () -> Unit,
    onFocused: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier
            .width(168.dp)
            .height(104.dp)
            .semantics { contentDescription = shortcut.label },
        focusedScale = 1.06f,
        allowRealBlur = false,
        onFocusChanged = onFocused,
    ) { focused ->
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShortcutGlyph(
                icon = shortcut.icon,
                tint = if (focused) colors.accent else colors.textPrimary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = shortcut.label,
                style = type.caption,
                color = if (focused) colors.accent else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ShortcutGlyph(icon: ShortcutIcon, tint: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.075f
        when (icon) {
            ShortcutIcon.WIFI -> {
                val center = Offset(size.width / 2f, size.height * 0.74f)
                drawCircle(tint, radius = size.width * 0.06f, center = center)
                for (i in 1..3) {
                    val radius = size.width * (0.16f + i * 0.14f)
                    drawArc(
                        color = tint,
                        startAngle = -135f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            ShortcutIcon.BLUETOOTH -> {
                val path = Path().apply {
                    moveTo(size.width * 0.35f, size.height * 0.25f)
                    lineTo(size.width * 0.65f, size.height * 0.72f)
                    lineTo(size.width * 0.50f, size.height * 0.86f)
                    lineTo(size.width * 0.50f, size.height * 0.14f)
                    lineTo(size.width * 0.65f, size.height * 0.28f)
                    lineTo(size.width * 0.35f, size.height * 0.75f)
                }
                drawPath(path, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            ShortcutIcon.DISPLAY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.10f, size.height * 0.16f),
                    size = Size(size.width * 0.80f, size.height * 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.06f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    Offset(size.width * 0.50f, size.height * 0.71f),
                    Offset(size.width * 0.50f, size.height * 0.88f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            ShortcutIcon.SOUND -> {
                val path = Path().apply {
                    moveTo(size.width * 0.16f, size.height * 0.40f)
                    lineTo(size.width * 0.36f, size.height * 0.40f)
                    lineTo(size.width * 0.58f, size.height * 0.22f)
                    lineTo(size.width * 0.58f, size.height * 0.78f)
                    lineTo(size.width * 0.36f, size.height * 0.60f)
                    lineTo(size.width * 0.16f, size.height * 0.60f)
                    close()
                }
                drawPath(path, tint)
                drawArc(
                    tint,
                    -55f,
                    110f,
                    false,
                    Offset(size.width * 0.42f, size.height * 0.20f),
                    Size(size.width * 0.46f, size.height * 0.60f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            ShortcutIcon.NETWORK -> {
                drawCircle(tint, size.width * 0.36f, Offset(size.width / 2f, size.height / 2f), style = Stroke(stroke))
                drawOval(
                    tint,
                    Offset(size.width * 0.30f, size.height * 0.14f),
                    Size(size.width * 0.40f, size.height * 0.72f),
                    style = Stroke(stroke),
                )
                drawLine(tint, Offset(size.width * 0.14f, size.height / 2f), Offset(size.width * 0.86f, size.height / 2f), stroke, cap = StrokeCap.Round)
            }
            ShortcutIcon.SETTINGS -> {
                drawCircle(tint, size.width * 0.18f, Offset(size.width / 2f, size.height / 2f), style = Stroke(stroke))
                drawCircle(tint, size.width * 0.05f, Offset(size.width / 2f, size.height / 2f))
                for (i in 0 until 8) {
                    val angle = Math.PI / 4 * i
                    val inner = size.width * 0.24f
                    val outer = size.width * 0.40f
                    drawLine(
                        tint,
                        Offset(size.width / 2f + (Math.cos(angle) * inner).toFloat(), size.height / 2f + (Math.sin(angle) * inner).toFloat()),
                        Offset(size.width / 2f + (Math.cos(angle) * outer).toFloat(), size.height / 2f + (Math.sin(angle) * outer).toFloat()),
                        stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
            ShortcutIcon.INFO -> {
                drawCircle(tint, size.width * 0.36f, Offset(size.width / 2f, size.height / 2f), style = Stroke(stroke))
                drawLine(tint, Offset(size.width / 2f, size.height * 0.46f), Offset(size.width / 2f, size.height * 0.70f), stroke, cap = StrokeCap.Round)
                drawCircle(tint, size.width * 0.045f, Offset(size.width / 2f, size.height * 0.31f))
            }
            ShortcutIcon.HDMI -> {
                val path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.32f)
                    lineTo(size.width * 0.90f, size.height * 0.32f)
                    lineTo(size.width * 0.76f, size.height * 0.68f)
                    lineTo(size.width * 0.24f, size.height * 0.68f)
                    close()
                }
                drawPath(path, tint, style = Stroke(width = stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawLine(tint, Offset(size.width * 0.30f, size.height * 0.80f), Offset(size.width * 0.70f, size.height * 0.80f), stroke, cap = StrokeCap.Round)
            }
            ShortcutIcon.HOME -> {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.48f)
                    lineTo(size.width * 0.50f, size.height * 0.16f)
                    lineTo(size.width * 0.88f, size.height * 0.48f)
                    moveTo(size.width * 0.24f, size.height * 0.46f)
                    lineTo(size.width * 0.24f, size.height * 0.84f)
                    lineTo(size.width * 0.76f, size.height * 0.84f)
                    lineTo(size.width * 0.76f, size.height * 0.46f)
                }
                drawPath(path, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

private fun buildShortcuts(context: Context): List<ShortcutItem> {
    val items = mutableListOf<ShortcutItem>()
    fun addSettings(id: String, label: Int, icon: ShortcutIcon, action: String, fallback: String? = null) {
        val primary = Intent(action)
        val secondary = fallback?.let { Intent(it) }
        if (SystemUtils.canResolve(context, primary) ||
            (secondary != null && SystemUtils.canResolve(context, secondary))
        ) {
            items += ShortcutItem(
                id = id,
                label = context.getString(label),
                icon = icon,
                special = { SystemUtils.openSettings(context, action, fallback) },
            )
        }
    }
    addSettings("wifi", R.string.open_wifi, ShortcutIcon.WIFI, SystemSettings.ACTION_WIFI_SETTINGS, SystemSettings.ACTION_WIRELESS_SETTINGS)
    addSettings("bluetooth", R.string.open_bluetooth, ShortcutIcon.BLUETOOTH, SystemSettings.ACTION_BLUETOOTH_SETTINGS)
    addSettings("display", R.string.open_display, ShortcutIcon.DISPLAY, SystemSettings.ACTION_DISPLAY_SETTINGS)
    addSettings("sound", R.string.open_sound, ShortcutIcon.SOUND, SystemSettings.ACTION_SOUND_SETTINGS)
    addSettings("network", R.string.open_network, ShortcutIcon.NETWORK, SystemSettings.ACTION_WIRELESS_SETTINGS, SystemSettings.ACTION_SETTINGS)
    addSettings("settings", R.string.open_settings, ShortcutIcon.SETTINGS, SystemSettings.ACTION_SETTINGS)
    addSettings("device", R.string.device_info, ShortcutIcon.INFO, SystemSettings.ACTION_DEVICE_INFO_SETTINGS)
    items += ShortcutItem(
        id = "launcher",
        label = context.getString(R.string.launcher_setup),
        icon = ShortcutIcon.HOME,
    )
    val hdmi = TvInputs.hdmiInputs(context)
    hdmi.forEach { input: HdmiInput ->
        items += ShortcutItem(
            id = "hdmi:${input.id}",
            label = input.label,
            icon = ShortcutIcon.HDMI,
            intent = input.intent,
        )
    }
    return items
}
