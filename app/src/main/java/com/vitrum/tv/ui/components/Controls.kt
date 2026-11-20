package com.vitrum.tv.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.glass.LocalGlassStyle
import com.vitrum.tv.ui.theme.LocalAnimationsOff
import com.vitrum.tv.ui.theme.LocalMotionReduced
import com.vitrum.tv.ui.theme.LocalNavSounds
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType

@Composable
fun focusAnimationSpec(): AnimationSpec<Float> = when {
    LocalAnimationsOff.current -> snap()
    LocalMotionReduced.current -> tween(durationMillis = 160)
    else -> spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusableGlass(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    focusedScale: Float = 1.08f,
    enabled: Boolean = true,
    allowRealBlur: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused && enabled) focusedScale else 1f,
        animationSpec = focusAnimationSpec(),
        label = "vitrum-focus-scale",
    )
    val colors = LocalVitrumColors.current
    val style = LocalGlassStyle.current
    val resolvedShape = shape ?: RoundedCornerShape(style.cornerRadiusDp.dp)
    val sounds = LocalNavSounds.current
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (nowFocused != focused) {
                    focused = nowFocused
                    if (nowFocused && sounds) {
                        runCatching { view.playSoundEffect(SoundEffectConstants.CLICK) }
                    }
                    onFocusChanged?.invoke(nowFocused)
                }
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Menu && onMenu != null) {
                    onMenu.invoke()
                    true
                } else {
                    false
                }
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        // The panel is measured after the content and drawn behind it. The content
        // determines the control's intrinsic size unless the caller sets one.
        GlassPanel(
            modifier = Modifier.matchParentSize(),
            shape = resolvedShape,
            focused = focused,
            allowRealBlur = allowRealBlur,
        ) {}
        Box(Modifier.fillMaxHeight().padding(contentPadding)) {
            content(focused)
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.height(if (compact) 44.dp else 50.dp),
        enabled = enabled,
        focusedScale = 1.05f,
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 2.dp),
        ) {
            if (icon != null) {
                Image(
                    painter = rememberVectorPainter(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        when {
                            !enabled -> colors.textSecondary.copy(alpha = 0.5f)
                            primary || focused -> colors.accent
                            else -> colors.textPrimary
                        },
                    ),
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = type.label,
                color = when {
                    !enabled -> colors.textSecondary.copy(alpha = 0.5f)
                    primary -> colors.accent
                    else -> colors.textPrimary
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: androidx.compose.ui.unit.Dp = 50.dp,
) {
    val colors = LocalVitrumColors.current
    FocusableGlass(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        enabled = enabled,
        focusedScale = 1.09f,
        contentPadding = PaddingValues(0.dp),
    ) { focused ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = rememberVectorPainter(icon),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(if (focused) colors.accent else colors.textPrimary),
                modifier = Modifier.size(if (buttonSize < 44.dp) 18.dp else 22.dp),
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean = false,
    tone: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val resolvedTone = tone ?: colors.accent
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) {
                    Modifier.background(resolvedTone.copy(alpha = 0.20f))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = type.caption,
            color = if (selected) resolvedTone else colors.textSecondary,
            maxLines = 1,
        )
    }
}
