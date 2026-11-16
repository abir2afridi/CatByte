package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * CapCut-style Fullscreen Export Done Screen (Screenshot 4) - the export
 * RESULT page: video preview card, Share to TikTok button, Create Template
 * button, social icons (Instagram, Stories, YouTube, WhatsApp, Facebook,
 * Other), Quick actions and the AI Lab promo banner.
 *
 * Reachable only after a successful render: the host routes here when
 * `ExportUiState.isCompleted` is true and `ExportOutput` has been saved.
 */
@Composable
fun ExportDoneScreen(
    videoPath: String,
    onDone: () -> Unit,
    onSharePlatform: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var boostOnTikTok by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF24303E))
            .statusBarsPadding()
            .testTag("export_done_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ExportDoneHeader(onDone = onDone)

            Spacer(modifier = Modifier.height(16.dp))

            ExportDoneResultCard(
                boostOnTikTok = boostOnTikTok,
                onToggleBoost = { boostOnTikTok = !boostOnTikTok },
                onSharePlatform = onSharePlatform,
                onDone = onDone
            )

            Spacer(modifier = Modifier.height(22.dp))

            ExportDoneQuickActions(onDone = onDone)

            Spacer(modifier = Modifier.height(20.dp))

            ExportDoneAiLabBanner()

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
