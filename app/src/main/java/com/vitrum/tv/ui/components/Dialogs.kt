package com.vitrum.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import kotlinx.coroutines.delay

@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    width: Dp = 640.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val colors = LocalVitrumColors.current
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            GlassPanel(
                modifier = Modifier.widthIn(max = width).padding(24.dp),
                contentPadding = PaddingValues(22.dp),
                allowRealBlur = true,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    content()
                }
            }
            if (colors.isLight) {
                Box(Modifier.matchParentSize().background(Color.Transparent))
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }
    GlassDialog(onDismiss = onDismiss) {
        Text(title, style = type.title, color = colors.textPrimary)
        Text(message, style = type.body, color = colors.textSecondary)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        ) {
            GlassButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.focusRequester(focusRequester),
            )
            GlassButton(
                text = confirmLabel,
                onClick = onConfirm,
                primary = !destructive,
            )
        }
    }
}

@Composable
fun TextEntryDialog(
    title: String,
    label: String,
    initialValue: String = "",
    hint: String = "",
    allowEmpty: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    var value by remember { mutableStateOf(initialValue) }
    var shift by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstKey.requestFocus() }
    }

    GlassDialog(onDismiss = onDismiss, width = 780.dp) {
        Text(title, style = type.title, color = colors.textPrimary)
        Text(label, style = type.caption, color = colors.textSecondary)
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (allowEmpty || value.isNotBlank()) onConfirm(value.trim())
                }),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                style = type.body,
                                color = colors.textSecondary.copy(alpha = 0.7f),
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TvKeyboard(
            shift = shift,
            onShift = { shift = !shift },
            firstKeyFocus = firstKey,
            onKey = { key ->
                val inserted = if (shift) key.uppercase() else key
                value += inserted
                if (shift) shift = false
            },
            onBackspace = { value = value.dropLast(1) },
            onClear = { value = "" },
            onSpace = { value += " " },
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        ) {
            GlassButton(stringResource(R.string.cancel), onClick = onDismiss)
            GlassButton(
                text = stringResource(R.string.save),
                onClick = {
                    if (allowEmpty || value.isNotBlank()) onConfirm(value.trim())
                },
                primary = true,
                modifier = Modifier.focusRequester(confirmFocus),
            )
        }
    }
}

@Composable
fun TvKeyboard(
    shift: Boolean,
    onShift: () -> Unit,
    firstKeyFocus: FocusRequester,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSpace: () -> Unit,
) {
    val rows = listOf(
        "1234567890".toList(),
        "qwertyuiop".toList(),
        "asdfghjkl;".toList(),
        "zxcvbnm./:".toList(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { rowIndex, keys ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.forEachIndexed { keyIndex, key ->
                    val modifier = if (rowIndex == 0 && keyIndex == 0) {
                        Modifier.focusRequester(firstKeyFocus)
                    } else {
                        Modifier
                    }
                    KeyboardKey(
                        label = if (shift) key.uppercase() else key.toString(),
                        onClick = { onKey(if (shift) key.uppercase() else key.toString()) },
                        modifier = modifier,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyboardKey(
                label = if (shift) "ABC" else "abc",
                onClick = onShift,
                width = 66.dp,
            )
            KeyboardKey(
                label = stringResource(R.string.search_space),
                onClick = onSpace,
                width = 150.dp,
            )
            KeyboardKey(
                label = stringResource(R.string.backspace_key),
                onClick = onBackspace,
                width = 104.dp,
            )
            KeyboardKey(label = "C", onClick = onClear, width = 60.dp)
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 44.dp,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.width(width).height(42.dp),
        focusedScale = 1.06f,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = type.label, color = colors.textPrimary, maxLines = 1)
        }
    }
}

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppInfo>,
    initiallySelected: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    var selected by remember { mutableStateOf(initiallySelected) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstFocus.requestFocus() }
    }
    GlassDialog(onDismiss = onDismiss, width = 700.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = type.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.dialog_apps_selected, selected.size),
                style = type.caption,
                color = colors.textSecondary,
            )
        }
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                val isSelected = app.packageName in selected
                val modifier = if (app == apps.first()) Modifier.focusRequester(firstFocus) else Modifier
                FocusableGlass(
                    onClick = {
                        selected = if (isSelected) selected - app.packageName else selected + app.packageName
                    },
                    modifier = modifier.fillMaxWidth().height(46.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    focusedScale = 1.02f,
                ) { focused ->
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(
                                    if (isSelected) colors.accent else Color.White.copy(alpha = 0.14f),
                                    CircleShape,
                                ),
                        )
                        Text(
                            text = app.label,
                            style = type.body,
                            color = if (focused || isSelected) colors.textPrimary else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        ) {
            GlassButton(stringResource(R.string.cancel), onClick = onDismiss)
            GlassButton(
                text = stringResource(R.string.apply),
                onClick = { onApply(selected) },
                primary = true,
            )
        }
    }
}

@Composable
fun ScreenScrim(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.34f), Color.Black.copy(alpha = 0.55f)),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}
