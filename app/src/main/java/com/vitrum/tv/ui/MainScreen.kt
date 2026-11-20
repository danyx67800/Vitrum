package com.vitrum.tv.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.vitrum.tv.ui.components.LocalAppIconLoader
import com.vitrum.tv.ui.glass.GlassStyle
import com.vitrum.tv.ui.glass.LocalGlassStyle
import com.vitrum.tv.ui.glass.VitrumBackdropHost
import com.vitrum.tv.ui.glass.rememberGlassBackdrop
import com.vitrum.tv.ui.screens.AllAppsScreen
import com.vitrum.tv.ui.screens.HomeScreen
import com.vitrum.tv.ui.screens.LauncherGuideScreen
import com.vitrum.tv.ui.screens.OnboardingScreen
import com.vitrum.tv.ui.screens.SettingsScreen
import com.vitrum.tv.ui.theme.VitrumTheme

@Composable
fun VitrumRoot(vm: MainViewModel) {
    val settingsOrNull by vm.settings.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val homeFocusKey by vm.lastHomeFocusKey.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val homeListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val settings = settingsOrNull
    if (settings == null) {
        Box(
            Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF05070D)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Vitrum", color = androidx.compose.ui.graphics.Color.White)
        }
        return
    }

    val backdrop = rememberGlassBackdrop(settings)
    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    val autoScale = (configuration.screenHeightDp / 540f).coerceIn(0.8f, 2.2f)
    val scaledDensity = Density(
        density = baseDensity.density * autoScale * settings.uiScale,
        fontScale = baseDensity.fontScale,
    )

    VitrumTheme(settings, backdrop.accent) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalGlassStyle provides GlassStyle(
                cornerRadiusDp = settings.cornerRadius,
                blurRadiusDp = backdrop.blurRadiusDp,
            ),
            LocalAppIconLoader provides vm.iconLoader,
        ) {
            VitrumBackdropHost(backdrop, Modifier.fillMaxSize()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val horizontalInset = maxWidth * (settings.overscanMargin / 100f)
                    val verticalInset = maxHeight * (settings.overscanMargin / 100f)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalInset, vertical = verticalInset)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Search) {
                                    vm.openAllApps(focusSearch = true)
                                    true
                                } else {
                                    false
                                }
                            },
                    ) {
                        when (val current = screen) {
                            Screen.Onboarding -> OnboardingScreen(vm, settings)
                            Screen.Home -> HomeScreen(
                                vm = vm,
                                settings = settings,
                                listState = homeListState,
                                restoreKey = homeFocusKey,
                            )
                            is Screen.AllApps -> AllAppsScreen(
                                vm = vm,
                                settings = settings,
                                focusSearchOnOpen = current.focusSearch,
                            )
                            is Screen.Settings -> SettingsScreen(
                                vm = vm,
                                settings = settings,
                                section = current.section,
                            )
                            Screen.LauncherGuide -> LauncherGuideScreen(vm, settings)
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = screen == Screen.Home) {
        vm.goHome(forceTop = true)
    }
}

