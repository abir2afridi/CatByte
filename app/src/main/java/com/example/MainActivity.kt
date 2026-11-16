package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LocaleUtils
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.paperNotebookBackground
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.EditorViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels {
        EditorViewModelFactory((application as CatByteApplication).container)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            val defaultResolution by viewModel.defaultResolution.collectAsStateWithLifecycle()
            val defaultFps by viewModel.defaultFps.collectAsStateWithLifecycle()
            val showPaperGrid by viewModel.showPaperGrid.collectAsStateWithLifecycle()
            val watermarkEnabled by viewModel.watermarkEnabled.collectAsStateWithLifecycle()
            val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Stop the preview when the app goes to the background.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        viewModel.pausePlayback()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Recreate once when the language preference differs from the applied locale.
            LaunchedEffect(appLanguage) {
                if (LocaleUtils.readLanguage(this@MainActivity) != appLanguage) {
                    LocaleUtils.persistLanguage(this@MainActivity, appLanguage)
                    recreate()
                }
            }

            MyApplicationTheme(themeMode = appThemeMode) {
                val colors = PencilTheme.colors
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .paperNotebookBackground(
                            showGrid = showPaperGrid,
                            gridColor = if (colors.isLight) Color(0x182C2B28) else Color(0x18F6F6F9),
                            marginLineColor = if (colors.isLight) Color(0x24D90429) else Color(0x2238BDF8)
                        ),
                    color = colors.background
                ) {
                    CapCutApp(
                        viewModel = viewModel,
                        onOpenSettings = { viewModel.openSettings() }
                    )
                }
            }
        }
    }
}
