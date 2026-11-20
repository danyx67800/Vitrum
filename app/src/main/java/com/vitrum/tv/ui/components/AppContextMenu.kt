package com.vitrum.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.CustomRow
import com.vitrum.tv.data.Settings
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import kotlinx.coroutines.delay

@Composable
fun AppContextMenu(
    app: AppInfo,
    settings: Settings,
    customRows: List<CustomRow>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onAddToRow: (CustomRow) -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val firstAction = remember { FocusRequester() }
    val isFavorite = app.packageName in settings.favorites
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstAction.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassPanel(
                modifier = Modifier.widthIn(max = 500.dp),
                contentPadding = PaddingValues(20.dp),
                allowRealBlur = true,
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.menu_title),
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = app.label,
                        style = type.title,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = type.caption,
                        color = colors.textSecondary.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item(key = "open") {
                            MenuAction(
                                icon = Icons.Filled.PlayArrow,
                                label = stringResource(R.string.open),
                                onClick = onOpen,
                                modifier = Modifier.focusRequester(firstAction),
                            )
                        }
                        item(key = "favorite") {
                            MenuAction(
                                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                label = stringResource(
                                    if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
                                ),
                                tint = if (isFavorite) colors.accent else null,
                                onClick = onToggleFavorite,
                            )
                        }
                        item(key = "move") {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                MenuAction(
                                    icon = Icons.Filled.KeyboardArrowLeft,
                                    label = stringResource(R.string.move_left),
                                    onClick = onMoveLeft,
                                    modifier = Modifier.weight(1f),
                                )
                                MenuAction(
                                    icon = Icons.Filled.KeyboardArrowRight,
                                    label = stringResource(R.string.move_right),
                                    onClick = onMoveRight,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        item(key = "rename") {
                            MenuAction(
                                icon = Icons.Filled.Create,
                                label = stringResource(R.string.rename),
                                onClick = onRename,
                            )
                        }
                        item(key = "hide") {
                            MenuAction(
                                icon = Icons.Filled.Lock,
                                label = stringResource(R.string.hide),
                                onClick = onHide,
                            )
                        }
                        item(key = "info") {
                            MenuAction(
                                icon = Icons.Filled.Info,
                                label = stringResource(R.string.app_info),
                                onClick = onAppInfo,
                            )
                        }
                        item(key = "uninstall") {
                            MenuAction(
                                icon = Icons.Filled.Delete,
                                label = stringResource(R.string.uninstall),
                                tint = Color(0xFFFF98A8),
                                onClick = onUninstall,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.List),
                            contentDescription = null,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.textSecondary),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.add_to_row),
                            style = type.label,
                            color = colors.textSecondary,
                        )
                    }
                    if (customRows.isEmpty()) {
                        Text(
                            stringResource(R.string.add_to_row_empty),
                            style = type.caption,
                            color = colors.textSecondary.copy(alpha = 0.8f),
                        )
                    } else {
                        LazyColumn(
                            Modifier.fillMaxWidth().heightIn(max = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(customRows, key = { it.id }) { row ->
                                MenuAction(
                                    icon = Icons.Filled.List,
                                    label = row.name,
                                    onClick = { onAddToRow(row) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(44.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        focusedScale = 1.02f,
    ) { focused ->
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(icon),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    tint ?: if (focused) colors.accent else colors.textPrimary,
                ),
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = label,
                style = type.label,
                color = tint ?: colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MenuBadge(text: String, modifier: Modifier = Modifier) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Box(
        modifier
            .background(colors.accent.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = type.caption, color = colors.accent, maxLines = 1)
    }
}
