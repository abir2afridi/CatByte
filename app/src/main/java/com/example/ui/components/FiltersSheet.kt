package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoFilter

@Composable
fun FiltersSheet(
    activeFilter: VideoFilter,
    onSelectFilter: (VideoFilter) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filters = VideoFilter.values().toList()
    val filtered = if (searchQuery.isBlank()) filters else filters.filter { it.displayName.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color(0xFF262832), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("filters_sheet")
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
                                Text("Search filters", color = Color(0xFFA5A8B2), fontSize = 13.sp)
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

        // Custom 3D .CUBE LUT Import row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Custom 3D LUTs (.cube)",
                color = Color(0xFFA5A8B2),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF22242B))
                    .border(1.dp, Color(0xFF33353E), RoundedCornerShape(6.dp))
                    .clickable {
                        // Import .cube LUT callback stub / action
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "📥 Import .cube LUT",
                    color = Color(0xFF26E5D7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Filters Horizontal Scroll or Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = "All Filters",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filtered.forEach { filter ->
                    val isSelected = filter == activeFilter
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(86.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF26E5D7).copy(alpha = 0.2f) else Color(0xFF22242B))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) Color(0xFF26E5D7) else Color(0xFF33353E),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectFilter(filter) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        when (filter) {
                                            VideoFilter.SKETCH_GRAPHITE -> listOf(Color(0xFF2C2B28), Color(0xFFD6CEBD))
                                            VideoFilter.BLUEPRINT -> listOf(Color(0xFF0F172A), Color(0xFF38BDF8))
                                            VideoFilter.SEPIA_NOTEBOOK -> listOf(Color(0xFF78350F), Color(0xFFFDE68A))
                                            VideoFilter.CHARCOAL_SMUDGE -> listOf(Color.Black, Color(0xFF52525B))
                                            VideoFilter.HALFTONE_MANGA -> listOf(Color(0xFF18181B), Color(0xFFE4E4E7))
                                            VideoFilter.CYBERPUNK -> listOf(Color.Cyan, Color.Magenta)
                                            VideoFilter.GOLDEN_HOUR -> listOf(Color.Yellow, Color(0xFFFF5722))
                                            VideoFilter.VINTAGE -> listOf(Color(0xFF8D6E63), Color(0xFFFFE082))
                                            VideoFilter.NOIR -> listOf(Color.Black, Color.White)
                                            VideoFilter.TEAL_ORANGE -> listOf(Color(0xFF00B4D8), Color(0xFFFF6B6B))
                                            VideoFilter.GLITCH_ART -> listOf(Color.Red, Color.Blue)
                                            else -> listOf(Color.DarkGray, Color.LightGray)
                                        }
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = filter.displayName,
                            color = if (isSelected) Color(0xFF26E5D7) else Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
