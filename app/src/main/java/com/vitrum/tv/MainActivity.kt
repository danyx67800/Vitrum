package com.vitrum.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.vitrum.tv.data.Settings
import com.vitrum.tv.ui.components.GlassButton
import com.vitrum.tv.ui.glass.GlassPanel
import com.vitrum.tv.ui.glass.GlassStyle
import com.vitrum.tv.ui.glass.LocalGlassStyle
import com.vitrum.tv.ui.glass.VitrumBackdropHost
import com.vitrum.tv.ui.glass.rememberGlassBackdrop
import com.vitrum.tv.ui.theme.LocalVitrumType
import com.vitrum.tv.ui.theme.VitrumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = Settings()
            val backdrop = rememberGlassBackdrop(settings)
            VitrumTheme(settings, backdrop.accent) {
                CompositionLocalProvider(
                    LocalGlassStyle provides GlassStyle(
                        cornerRadiusDp = settings.cornerRadius,
                        blurRadiusDp = 24f,
                    ),
                ) {
                    VitrumBackdropHost(backdrop, Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().padding(64.dp), contentAlignment = Alignment.Center) {
                            GlassPanel(Modifier.padding(24.dp)) {
                                Box(Modifier.padding(48.dp)) {
                                    Text("Vitrum", style = LocalVitrumType.current.display)
                                    GlassButton("Open", onClick = {})
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
