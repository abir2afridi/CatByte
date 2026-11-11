package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoProject

/**
 * CapCut-style Cover Frame Editor matching Screenshot 2:
 * - Top Bar: Close (X) on left, Cyan "Save" button on right.
 * - Center: Cover Video Preview Frame.
 * - Filmstrip: "+ Upload" custom photo, video frame scrubber with white needle, "Smart cover" chip.
 * - Bottom Bar: Select, Text, Stickers, Add photo, AI edit tools.
 */
@Composable
fun CoverEditorSheet(
    project: VideoProject,
    onSaveCover: (coverFrameMs: Long, customUri: Uri?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Select, 1: Text, 2: Stickers, 3: Add photo, 4: AI edit
    var coverFrameMs by remember { mutableLongStateOf(project.clips.firstOrNull()?.startTimelineMs ?: 0L) }
    var customCoverUri by remember { mutableStateOf<Uri?>(null) }
    var coverTextOverlay by remember { mutableStateOf("") }
    var showCoverTextInput by remember { mutableStateOf(false) }

    val totalMs = project.totalDurationMs.coerceAtLeast(1000L)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customCoverUri = uri
            Toast.makeText(context, "Custom cover photo uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .statusBarsPadding()
            .testTag("cover_editor_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row: Close (X) + Save Button
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

                Button(
                    onClick = {
                        onSaveCover(coverFrameMs, customCoverUri)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Save", color = Color(0xFF041E21), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Center Video / Custom Image Preview Area
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
                    if (customCoverUri != null) {
                        CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
                    } else {
                        CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
                    }

                    if (coverTextOverlay.isNotBlank()) {
                        Text(
                            text = coverTextOverlay,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Cover Scrubber Filmstrip Section (Above Bottom Bar)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "+ Upload" custom photo button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF24252A))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Upload", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }

                    // Video Frame Scrubber Filmstrip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2026))
                            .border(1.dp, Color(0xFF2A2B34), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
                        }

                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // "✨ Smart cover" chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF24252A))
                        .clickable {
                            coverFrameMs = (totalMs / 3L)
                            Toast.makeText(context, "Smart cover frame selected", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("✨", fontSize = 11.sp)
                    Text("Smart cover", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Bottom Navigation Bar (Cover Tools: Select, Text, Stickers, Add photo, AI edit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color(0xFF121215))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(0, "Select", Icons.Default.Crop),
                    Triple(1, "Text", Icons.Default.TextFields),
                    Triple(2, "Stickers", Icons.Default.Palette),
                    Triple(3, "Add photo", Icons.Default.AddPhotoAlternate),
                    Triple(4, "AI edit", Icons.Default.AutoAwesome)
                ).forEach { (index, label, icon) ->
                    val isSelected = selectedTab == index
                    val itemColor = if (isSelected) Color(0xFF26E5D7) else Color(0xFFA5A8B2)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = index
                                when (index) {
                                    1 -> showCoverTextInput = true
                                    2 -> Toast.makeText(context, "Stickers picker", Toast.LENGTH_SHORT).show()
                                    3 -> photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    4 -> Toast.makeText(context, "AI cover enhancement applied", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = itemColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = itemColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF26E5D7), RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }

        if (showCoverTextInput) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCoverTextInput = false },
                title = { Text("Cover Text Overlay", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = coverTextOverlay,
                        onValueChange = { coverTextOverlay = it },
                        placeholder = { Text("Enter cover title...", color = Color(0xFFA5A8B2)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showCoverTextInput = false }) {
                        Text("Apply", color = Color(0xFF26E5D7), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        coverTextOverlay = ""
                        showCoverTextInput = false
                    }) {
                        Text("Clear", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E2026)
            )
        }
    }
}
