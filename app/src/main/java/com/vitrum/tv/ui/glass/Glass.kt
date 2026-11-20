package com.vitrum.tv.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vitrum.tv.ui.theme.LocalVitrumColors
import kotlin.math.roundToInt

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    focused: Boolean = false,
    allowRealBlur: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalVitrumColors.current
    val style = LocalGlassStyle.current
    val backdrop = LocalGlassBackdrop.current
    val resolvedShape = shape ?: RoundedCornerShape(style.cornerRadiusDp.dp)
    var position by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (focused) 20.dp else 12.dp,
                shape = resolvedShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = if (focused) colors.accent.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f),
            )
            .clip(resolvedShape)
            .onGloballyPositioned { coordinates -> position = coordinates.positionInRoot() },
    ) {
        BackdropLayer(
            backdrop = backdrop,
            position = position,
            allowRealBlur = allowRealBlur,
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.glassTintStrong,
                            colors.glassTint,
                            colors.panelScrim,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val radius = size.width.coerceAtLeast(size.height) * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (colors.isLight) 0.34f else 0.15f),
                                Color.Transparent,
                            ),
                            center = Offset(0f, 0f),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset.Zero,
                    )
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f)),
                            startY = size.height * 0.55f,
                        ),
                    )
                },
        )
        Box(
            Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(colors.borderStart, colors.borderEnd),
                    ),
                    shape = resolvedShape,
                ),
        )
        if (focused) {
            Box(
                Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = 0.95f),
                                colors.accent.copy(alpha = 0.35f),
                            ),
                        ),
                        shape = resolvedShape,
                    ),
            )
        }
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun BackdropLayer(
    backdrop: GlassBackdrop,
    position: Offset,
    allowRealBlur: Boolean,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val realBlur = allowRealBlur && backdrop.mode == BackdropMode.REAL
    if (realBlur) {
        val widthDp = with(density) { backdrop.screenSize.width.toDp() }
        val heightDp = with(density) { backdrop.screenSize.height.toDp() }
        Box(Modifier.fillMaxSize().clipToBounds()) {
            Box(
                Modifier
                    .requiredSize(widthDp, heightDp)
                    .offset(
                        x = with(density) { (-position.x).toDp() },
                        y = with(density) { (-position.y).toDp() },
                    )
                    .blur(
                        radius = backdrop.blurRadiusDp.dp,
                        edgeTreatment = BlurredEdgeTreatment.Rectangle,
                    ),
            ) {
                LiquidBackground(
                    preset = backdrop.preset,
                    phase = backdrop.phase,
                    image = backdrop.image,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    } else {
        val blurred = backdrop.blurred
        if (backdrop.mode != BackdropMode.NONE && blurred != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawImage(
                            image = blurred,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(blurred.width, blurred.height),
                            dstOffset = IntOffset(
                                -position.x.roundToInt(),
                                -position.y.roundToInt(),
                            ),
                            dstSize = IntSize(backdrop.screenSize.width, backdrop.screenSize.height),
                            filterQuality = FilterQuality.Low,
                        )
                    },
            )
        }
    }
}

/** A thin translucent strip used between settings groups. */
@Composable
fun GlassRule(modifier: Modifier = Modifier) {
    val colors = LocalVitrumColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.borderEnd),
    )
}
