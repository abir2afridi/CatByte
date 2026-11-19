package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.TransitionCategory
import com.example.model.TransitionType
import java.util.Locale

/**
 * The transition picker.
 *
 * The category tabs really filter the offered transitions - every transition
 * belongs to the category the render graph draws it in - and the duration
 * slider writes the clip's real transition length. The clip id travels with
 * every callback, so a transition can never land on a different clip than
 * the one the picker was opened for.
 */
@Composable
fun TransitionsPickerSheet(
    clipId: String,
    /** The transition the clip currently uses. */
    currentTransition: TransitionType = TransitionType.NONE,
    /** The clip's current transition length, in milliseconds. */
    currentDurationMs: Long = 400L,
    onSelectTransition: (clipId: String, transition: TransitionType) -> Unit,
    onDurationChanged: (clipId: String, durationMs: Long) -> Unit,
    onClose: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<TransitionCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val transitions = TransitionType.entries.filter { it.isRenderable }
    val filtered = transitions.filter { transition ->
        val inCategory = selectedCategory == null || transition.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
            transition.displayName.contains(searchQuery, ignoreCase = true)
        inCategory && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color(0xFF262832), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("transitions_sheet")
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5A5B64))
            )
        }

        // Top Search Bar and Checkmark (✓)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF22242B))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFA5A8B2),
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text("Search transitions", color = Color(0xFFA5A8B2), fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22242B))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = Color(0xFF26E5D7),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Transition duration: the value is written to the clip, so the
        // slider really changes how long the transition takes.
        TransitionDurationRow(
            durationMs = currentDurationMs,
            onDurationChanged = { durationMs -> onDurationChanged(clipId, durationMs) }
        )

        // Category tabs - every tab filters the grid below it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryTab(
                label = "All",
                selected = selectedCategory == null,
                onClick = { selectedCategory = null }
            )
            TransitionCategory.entries.forEach { category ->
                CategoryTab(
                    label = category.displayName,
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // Transitions Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filtered, key = { it.name }) { trans ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2026))
                        .border(1.dp, Color(0xFF2D303B), RoundedCornerShape(8.dp))
                        .clickable {
                            onSelectTransition(clipId, trans)
                            onClose()
                        }
                        .padding(8.dp)
                        .testTag("transition_${trans.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF262832))
                    ) {
                        CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Transition",
                                tint = Color(0xFF26E5D7),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = trans.displayName,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp)
            .testTag("transition_tab_${label.lowercase()}")
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFA5A8B2),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(if (selected) Color(0xFF26E5D7) else Color.Transparent)
        )
    }
}

/**
 * The duration slider.
 *
 * The draft value follows the committed one while the sheet is idle, and the
 * gesture itself is the source of truth - the ViewModel coalesces the whole
 * drag into a single undo step.
 */
@Composable
private fun TransitionDurationRow(
    durationMs: Long,
    onDurationChanged: (Long) -> Unit
) {
    val range = TransitionType.MIN_DURATION_MS.toFloat()..TransitionType.MAX_DURATION_MS.toFloat()
    val committed = durationMs.toFloat().coerceIn(range.start, range.endInclusive)
    var draft by remember { mutableFloatStateOf(committed) }

    LaunchedEffect(committed) {
        draft = committed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Duration: ${String.format(Locale.US, "%.1fs", draft / 1000f)}",
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
        androidx.compose.material3.Slider(
            value = draft.coerceIn(range.start, range.endInclusive),
            onValueChange = {
                draft = it.coerceIn(range.start, range.endInclusive)
                onDurationChanged(draft.toLong())
            },
            valueRange = range,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF26E5D7),
                inactiveTrackColor = Color(0xFF22242B)
            ),
            modifier = Modifier
                .width(180.dp)
                .height(24.dp)
                .testTag("transition_duration")
        )
    }
}
