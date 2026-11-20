package com.vitrum.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.AppIconLoader
import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.Artwork
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.TileSize
import com.vitrum.tv.data.TileStyle
import com.vitrum.tv.ui.glass.LocalGlassStyle
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType

val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader?> { null }

data class TileMetrics(val width: Dp, val height: Dp, val square: Boolean)

fun tileMetrics(settings: Settings, itemScale: Float = 1f): TileMetrics {
    val baseWidth = when (settings.tileSize) {
        TileSize.SMALL -> 0
        TileSize.MEDIUM -> 1
        TileSize.LARGE -> 2
    }
    return when (settings.tileStyle) {
        TileStyle.BANNER -> {
            val width = when (baseWidth) {
                0 -> 178.dp
                1 -> 236.dp
                else -> 300.dp
            }
            TileMetrics(width * itemScale, width * 0.5625f * itemScale, square = false)
        }
        TileStyle.ICON -> {
            val side = when (baseWidth) {
                0 -> 114.dp
                1 -> 142.dp
                else -> 172.dp
            }
            TileMetrics(side * itemScale, side * itemScale, square = true)
        }
        TileStyle.ICON_ONLY -> {
            val side = when (baseWidth) {
                0 -> 78.dp
                1 -> 100.dp
                else -> 124.dp
            }
            TileMetrics(side * itemScale, side * itemScale, square = true)
        }
    }
}

@Composable
fun AppTile(
    app: AppInfo,
    settings: Settings,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (Boolean) -> Unit = {},
) {
    val metrics = tileMetrics(settings)
    val density = LocalDensity.current
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val style = LocalGlassStyle.current
    val showLabel = settings.showAppNames && settings.tileStyle != TileStyle.ICON_ONLY
    val banner = settings.tileStyle == TileStyle.BANNER
    val artworkWidth = with(density) { metrics.width.roundToPx() }
    val artworkHeight = with(density) { metrics.height.roundToPx() }
    val loader = LocalAppIconLoader.current
    val artwork by produceState<Artwork?>(null, app.packageName, app.activityName, banner, artworkWidth) {
        value = loader?.load(app, banner, artworkWidth, artworkHeight)
    }
    val description = stringResource(R.string.cd_app_tile, app.label)

    FocusableGlass(
        onClick = onClick,
        onLongClick = onMenu,
        onMenu = onMenu,
        onFocusChanged = onFocused,
        modifier = modifier
            .width(metrics.width)
            .height(metrics.height)
            .semantics { contentDescription = description },
        focusedScale = 1.075f,
        allowRealBlur = false,
    ) { focused ->
        Box(Modifier.fillMaxSize()) {
            if (focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    colors.accent.copy(alpha = 0.22f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
            ArtworkContent(
                app = app,
                artwork = artwork,
                banner = banner,
                showLabel = showLabel,
                labelBottomPadding = if (showLabel) 30.dp else 0.dp,
                corner = (style.cornerRadiusDp - 6).coerceAtLeast(8),
            )
            if (showLabel) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                            ),
                        )
                        .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 7.dp),
                ) {
                    Text(
                        text = app.label,
                        style = type.caption,
                        color = Color.White.copy(alpha = 0.96f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ArtworkContent(
    app: AppInfo,
    artwork: Artwork?,
    banner: Boolean,
    showLabel: Boolean,
    labelBottomPadding: Dp,
    corner: Int,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val bitmap = artwork?.bitmap
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val isBannerArtwork = artwork?.isBanner == true
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = if (isBannerArtwork && banner) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isBannerArtwork && banner) 4.dp else 12.dp)
                .padding(bottom = labelBottomPadding)
                .clip(RoundedCornerShape(corner.dp)),
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .padding(bottom = labelBottomPadding)
                    .width(54.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = type.title,
                    color = colors.accent,
                )
            }
        }
    }
}
