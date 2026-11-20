package com.vitrum.tv.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.ui.glass.BackgroundPreset
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Text(
        text = text,
        style = type.headline,
        color = colors.textPrimary,
        modifier = modifier.padding(top = 6.dp, bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun SettingsHint(text: String, modifier: Modifier = Modifier) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Text(
        text = text,
        style = type.caption,
        color = colors.textSecondary,
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth().height(58.dp).bringIntoViewOnFocus(),
        allowRealBlur = false,
        focusedScale = 1.01f,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { focused ->
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = type.body,
                    color = if (focused) colors.accent else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = type.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ToggleSwitch(checked = checked, focused = focused)
        }
    }
}

@Composable
fun ToggleSwitch(checked: Boolean, focused: Boolean) {
    val colors = LocalVitrumColors.current
    Canvas(Modifier.width(46.dp).height(26.dp)) {
        val radius = size.height / 2f
        drawRoundRect(
            color = if (checked) colors.accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
        )
        if (focused) {
            drawRoundRect(
                color = colors.accent.copy(alpha = 0.9f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                style = Stroke(width = 1.5f),
            )
        }
        val knobX = if (checked) size.width - radius else radius
        drawCircle(
            color = if (checked) Color.White else Color.White.copy(alpha = 0.72f),
            radius = radius - 4f,
            center = Offset(knobX, size.height / 2f),
        )
    }
}

@Composable
fun ActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(if (description == null) 54.dp else 64.dp).bringIntoViewOnFocus(),
        allowRealBlur = false,
        focusedScale = 1.01f,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { focused ->
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Image(
                    painter = rememberVectorPainter(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (focused) colors.accent else colors.textSecondary),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = type.body,
                    color = if (focused) colors.accent else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = type.caption,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = type.label,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
            Chevron(tint = if (focused) colors.accent else colors.textSecondary.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun Chevron(tint: Color) {
    Canvas(Modifier.size(16.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.35f, size.height * 0.22f)
            lineTo(size.width * 0.68f, size.height * 0.50f)
            lineTo(size.width * 0.35f, size.height * 0.78f)
        }
        drawPath(
            path,
            tint,
            style = Stroke(width = size.width * 0.11f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Composable
fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Row(
        modifier = modifier.fillMaxWidth().height(58.dp).bringIntoViewOnFocus(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(label, style = type.body, color = colors.textPrimary, maxLines = 1)
            if (description != null) {
                Text(
                    description,
                    style = type.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StepButton(label = "−", enabled = minusEnabled, onClick = onMinus)
        Text(
            text = value,
            style = type.label,
            color = colors.accent,
            modifier = Modifier.width(76.dp),
            maxLines = 1,
        )
        StepButton(label = "+", enabled = plusEnabled, onClick = onPlus)
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = Modifier.size(38.dp).bringIntoViewOnFocus(),
        enabled = enabled,
        allowRealBlur = false,
        focusedScale = 1.08f,
        contentPadding = PaddingValues(0.dp),
    ) { focused ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = type.title,
                color = when {
                    !enabled -> colors.textSecondary.copy(alpha = 0.4f)
                    focused -> colors.accent
                    else -> colors.textPrimary
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Column(modifier.fillMaxWidth().bringIntoViewOnFocus()) {
        Text(
            label,
            style = type.body,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
        )
        if (description != null) {
            SettingsHint(description)
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                FocusableGlass(
                    onClick = { onSelect(index) },
                    modifier = Modifier.height(40.dp),
                    allowRealBlur = false,
                    focusedScale = 1.04f,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) { focused ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = option,
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
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun ColorSwatch(
    label: String?,
    argb: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        FocusableGlass(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            allowRealBlur = false,
            focusedScale = 1.12f,
        ) { focused ->
            Box(Modifier.fillMaxSize().padding(4.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(argb)),
                )
                if (selected || focused) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = colors.textPrimary.copy(alpha = if (selected) 0.95f else 0.5f),
                            radius = size.minDimension / 2f - 1f,
                            style = Stroke(width = 2f),
                        )
                    }
                }
            }
        }
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(label, style = type.caption, color = if (selected) colors.accent else colors.textSecondary, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackgroundChooser(
    presets: List<BackgroundPreset>,
    selectedId: String,
    onSelectPreset: (String) -> Unit,
    onSelectCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        presets.forEach { preset ->
            BackgroundTile(
                label = stringResource(preset.labelRes),
                preset = preset,
                selected = selectedId == preset.id,
                onClick = { onSelectPreset(preset.id) },
            )
        }
        BackgroundTile(
            label = stringResource(R.string.background_custom),
            preset = null,
            selected = selectedId == "custom",
            onClick = onSelectCustom,
        )
    }
}

@Composable
fun BackgroundTile(
    label: String,
    preset: BackgroundPreset?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = Modifier.width(154.dp).height(88.dp).bringIntoViewOnFocus(),
        shape = RoundedCornerShape(14.dp),
        allowRealBlur = false,
        focusedScale = 1.04f,
    ) { focused ->
        Box(Modifier.fillMaxSize()) {
            if (preset != null) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(Color(preset.topArgb), Color(preset.bottomArgb)),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                    )
                    preset.blobs.forEach { blob ->
                        val center = Offset(blob.x * size.width, blob.y * size.height)
                        val radius = blob.radius * minOf(size.width, size.height)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(blob.argb).copy(alpha = blob.alpha),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    }
                }
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF111A28), Color(0xFF243047)),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                    )
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.25f),
                        radius = size.minDimension * 0.35f,
                        center = Offset(size.width * 0.72f, size.height * 0.25f),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Text(label, style = type.caption, color = Color.White, maxLines = 1)
            }
            if (selected || focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = colors.accent.copy(alpha = if (selected) 0.95f else 0.55f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                            style = Stroke(width = if (selected) 2.5f else 1.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowOrderRow(
    label: String,
    visible: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onToggle: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Row(
        modifier.fillMaxWidth().height(48.dp).bringIntoViewOnFocus(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FocusableGlass(
            onClick = onToggle,
            modifier = Modifier.weight(1f).height(44.dp),
            allowRealBlur = false,
            focusedScale = 1.01f,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) { focused ->
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ToggleSwitch(checked = visible, focused = focused)
                Text(
                    label,
                    style = type.body,
                    color = if (focused) colors.accent else if (visible) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OrderButton(Icons.Filled.KeyboardArrowUp, canUp, onUp)
        OrderButton(Icons.Filled.KeyboardArrowDown, canDown, onDown)
    }
}

@Composable
private fun OrderButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalVitrumColors.current
    FocusableGlass(
        onClick = onClick,
        modifier = Modifier.size(36.dp).bringIntoViewOnFocus(),
        enabled = enabled,
        allowRealBlur = false,
        focusedScale = 1.06f,
    ) { focused ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    when {
                        !enabled -> colors.textSecondary.copy(alpha = 0.3f)
                        focused -> colors.accent
                        else -> colors.textSecondary
                    },
                ),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.body, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = type.label, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun presetWave(value: Float): Float = (sin(value * Math.PI) * cos(value * Math.PI / 2)).toFloat()
