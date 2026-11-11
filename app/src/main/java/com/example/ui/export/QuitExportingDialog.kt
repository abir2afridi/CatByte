package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * CapCut-style Quit Exporting Confirmation Alert Dialog (Screenshot 3).
 * Shown when the user taps back on the fullscreen exporting page: quitting
 * cancels the render, continuing keeps it running.
 */
@Composable
fun QuitExportingDialog(
    onContinue: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .testTag("quit_exporting_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quit exporting? You will go back to your project.",
                    color = Color(0xFF1E2026),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // "Continue exporting" cyan filled button
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(
                        text = "Continue exporting",
                        color = Color(0xFF041E21),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Quit" plain text button
                Text(
                    text = "Quit",
                    color = Color(0xFF6E717C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onQuit() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
