package com.example.ui.components

import androidx.compose.runtime.compositionLocalOf

/** Provides the shared ExoPlayer to the video canvas for real playback. */
val LocalVideoPlayer = compositionLocalOf<androidx.media3.common.Player?> { null }
