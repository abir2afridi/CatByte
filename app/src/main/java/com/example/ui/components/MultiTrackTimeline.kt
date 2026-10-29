package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.TransitionType
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder

private const val DP_PER_SECOND = 65f

@Composable
fun MultiTrackTimeline(
    project: VideoProject,
    currentPositionMs: Long,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedTextId: String?,
    zoom: Float,
    magneticSnap: Boolean = true,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onTransitionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val totalMs = project.totalDurationMs.coerceAtLeast(4000L)
    val msToDp = (DP_PER_SECOND * zoom) / 1000f
    val timelineWidthDp = (totalMs * msToDp).dp + 260.dp
    val scrollState = rememberScrollState()

    var isTrackMuted by remember { mutableStateOf(false) }

    // Precompute cut points for magnetic snapping
    val cutPoints = remember(project) {
        val cuts = mutableListOf(0L)
        var acc = 0L
        for (c in project.clips) {
            acc += c.durationMs
            cuts.add(acc)
        }
        for (a in project.audioTracks) {
            cuts.add(a.startTimelineMs)
            cuts.add(a.startTimelineMs + a.durationMs)
        }
        for (t in project.textOverlays) {
            cuts.add(t.startTimelineMs)
            cuts.add(t.startTimelineMs + t.durationMs)
        }
        cuts.distinct().sorted()
    }

    LaunchedEffect(currentPositionMs, isPlaying) {
        if (isPlaying) {
            val playheadPx = currentPositionMs * msToDp
            val targetScroll = (playheadPx - 260).coerceAtLeast(0f).toInt()
            scrollState.scrollTo(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.timelineTrack)
            .border(1.2.dp, colors.borderSubtle)
            .testTag("multi_track_timeline")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Track Left Controls (Mute clip audio, Cover)
            TimelineTrackHeader(
                isTrackMuted = isTrackMuted,
                onMuteClick = { isTrackMuted = !isTrackMuted }
            )

            // Scrollable Timeline Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
                    .pointerInput(totalMs, msToDp, magneticSnap, cutPoints) {
                        detectTapGestures { offset ->
                            var targetMs = (offset.x / msToDp).toLong().coerceIn(0L, totalMs)
                            if (magneticSnap) {
                                val nearest = cutPoints.minByOrNull { kotlin.math.abs(it - targetMs) }
                                if (nearest != null && kotlin.math.abs(nearest - targetMs) <= 180L) {
                                    targetMs = nearest
                                }
                            }
                            onSeek(targetMs)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Time Ruler with sketched notebook grid
                        TimelineTimeRuler(
                            totalMs = totalMs,
                            msToDp = msToDp
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // 2. Main Video Clips Track
                        VideoClipsTrack(
                            clips = project.clips,
                            selectedClipId = selectedClipId,
                            msToDp = msToDp,
                            onSelectClip = onSelectClip,
                            onTransitionClick = onTransitionClick,
                            onAddMediaClick = onAddMediaClick
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // 3. Audio / Music Track
                        if (project.audioTracks.isNotEmpty()) {
                            AudioClipsTrack(
                                tracks = project.audioTracks,
                                msToDp = msToDp
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        // 4. Text / Subtitle Track
                        if (project.textOverlays.isNotEmpty()) {
                            TextClipsTrack(
                                texts = project.textOverlays,
                                selectedTextId = selectedTextId,
                                msToDp = msToDp,
                                onSelectText = onSelectText
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        // 5. Visual Effects Track
                        if (project.effects.isNotEmpty()) {
                            EffectsClipsTrack(
                                effects = project.effects,
                                msToDp = msToDp
                            )
                        }
                    }

                    // 6. Sketched Sharpened Wooden Pencil Playhead
                    val playheadOffsetDp = (currentPositionMs * msToDp).dp
                    Box(
                        modifier = Modifier
                            .offset(x = playheadOffsetDp - 7.dp)
                            .fillMaxHeight()
                    ) {
                        // Floating timestamp badge pinned to pencil
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-18).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.border, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            val sec = currentPositionMs / 1000f
                            Text(
                                text = String.format("%.1fs", sec),
                                color = colors.textPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Vertical 2B graphite guide line extending through tracks
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = 22.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(colors.playhead)
                        )
                        // Top Sharpened Pencil Tip (Yellow pencil body, wood collar, 2B graphite lead point)
                        SharpenedPencilTip(
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTrackHeader(
    isTrackMuted: Boolean,
    onMuteClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .width(54.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .border(width = 1.2.dp, color = colors.borderSubtle)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Video track icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Track",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Mute button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isTrackMuted) colors.surfaceVariant else colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                .clickable { onMuteClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTrackMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Mute Track",
                tint = if (isTrackMuted) colors.textTertiary else colors.accentCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        // Cover thumbnail button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cover",
                color = colors.textPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimelineTimeRuler(
    totalMs: Long,
    msToDp: Float
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(colors.surfaceElevated)
            .border(width = 0.8.dp, color = colors.borderSubtle)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height

            // 1. Bottom ruler brass edge
            drawLine(
                color = colors.border.copy(alpha = 0.4f),
                start = Offset(0f, h),
                end = Offset(size.width, h),
                strokeWidth = 1.2f
            )

            // 2. Architect drafting scale graduations (200ms mm-ticks, 500ms half-marks, 1000ms major marks)
            val subStepMs = 200L
            var timeMs = 0L
            while (timeMs <= totalMs + 4000L) {
                val x = timeMs * msToDp
                val isSecond = timeMs % 1000L == 0L
                val isHalfSecond = timeMs % 500L == 0L

                val tickHeight = when {
                    isSecond -> h * 0.55f
                    isHalfSecond -> h * 0.38f
                    else -> h * 0.22f
                }

                val strokeW = if (isSecond) 1.5f else 0.9f
                val tickColor = when {
                    isSecond -> colors.border
                    isHalfSecond -> colors.textSecondary.copy(alpha = 0.6f)
                    else -> colors.borderSubtle.copy(alpha = 0.7f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(x, 0f),
                    end = Offset(x, tickHeight),
                    strokeWidth = strokeW
                )

                timeMs += subStepMs
            }
        }

        // Stamped second markings (00:00, 00:01, ...)
        Row(modifier = Modifier.fillMaxSize()) {
            var labelMs = 0L
            while (labelMs <= totalMs + 4000L) {
                val sec = labelMs / 1000
                Box(
                    modifier = Modifier
                        .offset(x = (labelMs * msToDp).dp + 2.dp)
                        .padding(top = 13.dp)
                ) {
                    Text(
                        text = String.format("%02d:%02d", sec / 60, sec % 60),
                        color = colors.textPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                labelMs += 1000L
            }
        }
    }
}

@Composable
private fun VideoClipsTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit,
    onTransitionClick: (String) -> Unit,
    onAddMediaClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        clips.forEachIndexed { index, clip ->
            val isSelected = clip.id == selectedClipId
            val clipWidthDp = (clip.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .width(clipWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.clipSelected else colors.clipBg)
                    .pencilHatching(
                        color = if (isSelected) colors.accentCyan.copy(alpha = 0.18f) else colors.border.copy(alpha = 0.08f),
                        spacing = 7.dp
                    )
                    .sketchPencilBorder(
                        strokeWidth = if (isSelected) 2.2.dp else 1.4.dp,
                        color = if (isSelected) colors.accentCyan else colors.border,
                        cornerRadius = 8.dp
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                // Waveform rendering
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 2.5.dp.toPx()
                    val spacing = 2.5.dp.toPx()
                    var currX = 0f
                    var i = 0
                    while (currX < size.width) {
                        val factor = clip.waveform.getOrElse(i % clip.waveform.size) { 0.5f }
                        val barHeight = size.height * 0.42f * factor
                        drawLine(
                            color = if (isSelected) colors.border.copy(alpha = 0.55f) else colors.textTertiary.copy(alpha = 0.4f),
                            start = Offset(currX, size.height - barHeight),
                            end = Offset(currX, size.height),
                            strokeWidth = barWidth
                        )
                        currX += barWidth + spacing
                        i++
                    }
                }

                // Title & Info labels
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clip.title,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%.1fs", clip.durationMs / 1000f),
                            color = colors.accentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (clip.speed != 1.0f) {
                            Text(
                                text = "${clip.speed}x",
                                color = colors.accentYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sketched tactile drag handles
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(colors.handle, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(colors.handle, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    )
                }
            }

            // Transition Icon Button between clips
            if (index < clips.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .border(1.2.dp, colors.border, RoundedCornerShape(6.dp))
                        .clickable { onTransitionClick(clip.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Transition",
                        tint = if (clip.transition != TransitionType.NONE) colors.accentCyan else colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Add Clip (+) Button at end of track
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.2.dp, colors.border, RoundedCornerShape(8.dp))
                .clickable { onAddMediaClick() }
                .testTag("add_clip_to_timeline_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Clip",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AudioClipsTrack(
    tracks: List<AudioClip>,
    msToDp: Float
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tracks.forEach { audio ->
            val startOffsetDp = (audio.startTimelineMs * msToDp).dp
            val widthDp = (audio.durationMs * msToDp).dp.coerceAtLeast(54.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentPurple.copy(alpha = 0.25f))
                    .border(1.2.dp, colors.accentPurple, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio",
                        tint = colors.accentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = audio.title,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TextClipsTrack(
    texts: List<TextOverlay>,
    selectedTextId: String?,
    msToDp: Float,
    onSelectText: (String) -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        texts.forEach { item ->
            val isSelected = item.id == selectedTextId
            val startOffsetDp = (item.startTimelineMs * msToDp).dp
            val widthDp = (item.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentYellow.copy(alpha = if (isSelected) 0.5f else 0.25f))
                    .border(
                        width = if (isSelected) 2.dp else 1.2.dp,
                        color = colors.accentYellow,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectText(item.id) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text",
                        tint = colors.accentYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = item.text,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectsClipsTrack(
    effects: List<TimelineEffect>,
    msToDp: Float
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        effects.forEach { effect ->
            val startOffsetDp = (effect.startTimelineMs * msToDp).dp
            val widthDp = (effect.durationMs * msToDp).dp.coerceAtLeast(44.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentGreen.copy(alpha = 0.25f))
                    .border(1.2.dp, colors.accentGreen, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "FX: ${effect.name}",
                    color = colors.textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
