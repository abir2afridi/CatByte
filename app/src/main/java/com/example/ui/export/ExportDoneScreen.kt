package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CapCutIconPlayTriangle
import com.example.ui.components.CapCutMeadowCanvas

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
            // Header: Left Arrow, Centered "Saved to device", Right "Done"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDone, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Saved to device",
                    color = Color(0xFFA0ADB8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Done",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDone() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Slate Card Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF334356))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Video Preview Card
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2632)),
                        contentAlignment = Alignment.Center
                    ) {
                        CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

                        // Play triangle button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CapCutIconPlayTriangle(tint = Color.White, size = 18.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Boost on TikTok radio option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { boostOnTikTok = !boostOnTikTok }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = boostOnTikTok,
                            onClick = { boostOnTikTok = !boostOnTikTok },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF26E5D7),
                                unselectedColor = Color(0xFFA0ADB8)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Boost your video on TikTok ⓘ",
                            color = Color(0xFFA0ADB8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Share to TikTok white button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { onSharePlatform("TikTok") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "TikTok",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share to TikTok",
                            color = Color(0xFF141416),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Create template slate button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF42556C))
                            .clickable { onDone() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create template",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Social Share Icons Row (Instagram, Stories, YouTube, WhatsApp, Facebook, Other)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("Instagram", Icons.Default.CameraAlt, Color(0xFFE1306C)),
                            Triple("Stories", Icons.Default.AddCircleOutline, Color(0xFF00F2FE)),
                            Triple("YouTube", Icons.Default.PlayCircleOutline, Color(0xFFFF0000)),
                            Triple("WhatsApp", Icons.Default.Chat, Color(0xFF25D366)),
                            Triple("Facebook", Icons.Default.Share, Color(0xFF1877F2)),
                            Triple("Other", Icons.Default.MoreHoriz, Color.White)
                        ).forEach { (label, icon, iconColor) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSharePlatform(label) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF263242)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = Color(0xFFA0ADB8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Quick actions Section
            Text(
                text = "Quick actions",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("New project", Icons.Default.Add, "new_project"),
                    Triple("Export options", Icons.Default.FileUpload, "export_options"),
                    Triple("Video quality", Icons.Default.Hd, "video_quality"),
                    Triple("Remove watermark", Icons.Default.VisibilityOff, "watermark"),
                    Triple("Edit photo", Icons.Default.PhotoLibrary, "edit_photo")
                ).forEach { (label, icon, tag) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF334356))
                            .clickable { onDone() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            color = Color(0xFFA0ADB8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Lab Promo Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF334356))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Lab: Make Your Black Friday Pop",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF24303E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
