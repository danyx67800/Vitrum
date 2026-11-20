package com.vitrum.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.R
import com.vitrum.tv.data.Settings
import com.vitrum.tv.data.WeatherNow
import com.vitrum.tv.data.weatherDescriptionKey
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.theme.LocalVitrumColors
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.util.NetState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopBar(
    settings: Settings,
    network: NetState,
    weather: WeatherNow?,
    updateAvailable: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    searchFocusRequester: FocusRequester? = null,
) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.locales.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            configuration.locale ?: Locale.getDefault()
        }
    }
    val clockPattern = remember(settings.clock24h, settings.showSeconds) {
        when {
            settings.clock24h && settings.showSeconds -> "HH:mm:ss"
            settings.clock24h -> "HH:mm"
            settings.showSeconds -> "h:mm:ss"
            else -> "h:mm"
        }
    }
    val clockFormat = remember(clockPattern, locale) { SimpleDateFormat(clockPattern, locale) }
    val dateFormat = remember(locale) { SimpleDateFormat("EEE d MMM", locale) }
    val now by produceState(System.currentTimeMillis(), clockPattern) {
        while (true) {
            value = System.currentTimeMillis()
            delay(if (settings.showSeconds) 1000 else 5_000)
        }
    }
    val networkLabel = when (network) {
        NetState.WIFI -> stringResource(R.string.wifi)
        NetState.ETHERNET -> stringResource(R.string.ethernet)
        NetState.OTHER -> "LAN"
        NetState.OFFLINE -> stringResource(R.string.offline)
    }

    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        allowRealBlur = true,
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VitrumMark()
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = clockFormat.format(Date(now)),
                    style = type.title,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                if (settings.showDate) {
                    Text(
                        text = dateFormat.format(Date(now)),
                        style = type.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (settings.weatherEnabled) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = weather?.let {
                            "${it.temperature.toInt()}°"
                        } ?: "—",
                        style = type.label,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = weather?.let {
                            stringResource(weatherStringRes(weatherDescriptionKey(it.code)))
                        } ?: stringResource(R.string.weather_unavailable),
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                }
                Spacer(Modifier.width(20.dp))
            }
            NetworkChip(label = networkLabel, connected = network != NetState.OFFLINE)
            Spacer(Modifier.width(14.dp))
            GlassIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search_button),
                onClick = onSearch,
                modifier = if (searchFocusRequester != null) {
                    Modifier.focusRequester(searchFocusRequester)
                } else {
                    Modifier
                },
            )
            Spacer(Modifier.width(8.dp))
            Box {
                GlassIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_settings_button),
                    onClick = onSettings,
                )
                if (updateAvailable) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(9.dp)
                            .then(
                                Modifier.background(
                                    color = colors.accent,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun VitrumMark() {
    val colors = LocalVitrumColors.current
    Canvas(Modifier.size(38.dp)) {
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.04f),
                ),
                center = Offset(size.width * 0.3f, size.height * 0.25f),
                radius = size.minDimension * 0.7f,
            ),
            radius = size.minDimension / 2f,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = size.minDimension / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.30f)
            lineTo(size.width * 0.50f, size.height * 0.74f)
            lineTo(size.width * 0.68f, size.height * 0.30f)
        }
        drawPath(
            path = path,
            color = colors.accent,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = size.width * 0.09f,
                cap = StrokeCap.Round,
            ),
        )
    }
}

@Composable
private fun NetworkChip(label: String, connected: Boolean) {
    val colors = LocalVitrumColors.current
    val type = LocalVitrumType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    color = if (connected) colors.accent else colors.textSecondary.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                ),
        )
        Text(
            text = label,
            style = type.caption,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

fun weatherStringRes(key: String): Int = when (key) {
    "clear" -> R.string.weather_clear
    "partly" -> R.string.weather_partly
    "fog" -> R.string.weather_fog
    "drizzle" -> R.string.weather_drizzle
    "rain" -> R.string.weather_rain
    "snow" -> R.string.weather_snow
    "storm" -> R.string.weather_storm
    else -> R.string.weather_cloudy
}
