package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectThumbnailLoader
import com.example.model.VideoProject
import kotlin.math.roundToInt

/**
 * CapCut-style cover editor.
 *
 * The cover is a real frame of the project video (scrubbed with the slider) or a
 * custom photo. Saving stores both on the project, and the home list renders the
 * same image the user picked here.
 */
@Composable
fun CoverEditorSheet(
    project: VideoProject,
    onSaveCover: (coverFrameMs: Long, customUri: Uri?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val totalMs = project.totalDurationMs.coerceAtLeast(1L)
    var coverFrameMs by remember { mutableLongStateOf(project.coverFrameMs.coerceIn(0L, totalMs)) }
    var customCoverUri by remember {
        mutableStateOf(project.coverImageUri.takeIf { it.isNotBlank() }?.let(Uri::parse))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customCoverUri = uri
            Toast.makeText(context, "Custom cover photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Decode the exact frame the user is looking at (custom photo wins).
    val preview by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        coverFrameMs,
        customCoverUri
    ) {
        value = ProjectThumbnailLoader.load(
            context = context,
            project = project,
            frameMs = coverFrameMs,
            coverImageUri = customCoverUri?.toString().orEmpty()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .statusBarsPadding()
            .testTag("cover_editor_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close cover editor",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Cover",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        onSaveCover(coverFrameMs, customCoverUri)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("cover_save")
                ) {
                    Text("Save", color = Color(0xFF041E21), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141416)),
                    contentAlignment = Alignment.Center
                ) {
                    val frame = preview
                    if (frame != null) {
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "Cover preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "No frame available yet",
                            color = Color(0xFFA5A8B2),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Frame ${(coverFrameMs / 1000f).formatOne()}s",
                        color = Color(0xFFA5A8B2),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "of ${(totalMs / 1000f).formatOne()}s",
                        color = Color(0xFFA5A8B2),
                        fontSize = 11.sp
                    )
                }
                Slider(
                    value = coverFrameMs.toFloat(),
                    onValueChange = { coverFrameMs = it.toLong().coerceIn(0L, totalMs) },
                    valueRange = 0f..totalMs.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cover_frame_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoverActionChip(
                        label = if (customCoverUri == null) "Upload photo" else "Change photo",
                        icon = Icons.Default.Upload,
                        testTag = "cover_upload"
                    ) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    CoverActionChip(
                        label = "Smart frame",
                        icon = Icons.Default.Restore,
                        testTag = "cover_smart"
                    ) {
                        // Pick the frame at one third of the timeline: usually
                        // past an intro fade, still before the ending cards.
                        coverFrameMs = (totalMs / 3L).coerceAtLeast(0L)
                    }
                    if (customCoverUri != null) {
                        CoverActionChip(
                            label = "Remove photo",
                            icon = Icons.Default.Close,
                            testTag = "cover_remove_photo"
                        ) {
                            customCoverUri = null
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CoverActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF24252A))
            .border(1.dp, Color(0xFF2A2B34), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF26E5D7),
            modifier = Modifier.size(14.dp)
        )
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Float.formatOne(): String = (this * 10).roundToInt().let { tenths ->
    "${tenths / 10}.${tenths % 10}"
}
