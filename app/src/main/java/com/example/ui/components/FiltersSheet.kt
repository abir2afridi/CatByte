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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.filters.FilterCatalog
import com.example.model.VideoFilter
import com.example.ui.components.LutImportSheet

/**
 * Filter drawer - same structure as the effects drawer: horizontally
 * scrollable category tabs plus a 3-column grid of cards.
 *
 * Tab membership and card swatch colours live in each filter's OWN file
 * (engine/filters/<Name>Filter.kt), so tuning one filter cannot affect any
 * other filter or its position in the grid.
 */
@Composable
fun FiltersSheet(
    activeFilter: VideoFilter,
    importedLuts: List<ImportedLut> = emptyList(),
    activeLutId: String? = null,
    onSelectFilter: (VideoFilter) -> Unit,
    onSelectLut: (ImportedLut?) -> Unit = {},
    onOpenLutImport: () -> Unit = {},
    /** Edit panel: 0 = Filters, 1 = Adjust, 2 = Video quality. */
    initialPanel: Int = 0,
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f,
    vignette: Float = 0f,
    onUpdateAdjustments: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onResetAdjustments: () -> Unit = {},
    onNotify: (String) -> Unit = {},
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var panel by remember { mutableIntStateOf(initialPanel.coerceIn(0, 2)) }

    val tabs = FilterCatalog.tabs
    val selectedCategory = tabs[selectedTab.coerceIn(0, tabs.lastIndex)]
    val visibleFilters = if (searchQuery.isBlank()) {
        FilterCatalog.filtersFor(selectedCategory)
    } else {
        FilterCatalog.all
            .filter { it.first.displayName.contains(searchQuery, ignoreCase = true) }
            .map { it.first }
            .ifEmpty { listOf(VideoFilter.NONE) }
    }
    val showLuts = selectedCategory == "My LUTs" && searchQuery.isBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color(0xFF262832), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("filters_sheet")
    ) {
        // Drag handle
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5A5B64))
            )
        }

        // Top-level panels: Filters | Adjust | Video quality (CapCut-style)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Filters", "Adjust", "Video quality").forEachIndexed { index, label ->
                val active = panel == index
                Text(
                    label,
                    color = if (active) Color.White else Color(0xFFA5A8B2),
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clickable { panel = index }
                        .padding(end = 18.dp).padding(vertical = 6.dp)
                        .testTag("color_panel_$index")
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Check, "Done", tint = Color(0xFF26E5D7), modifier = Modifier.size(20.dp))
            }
        }

        if (panel == 0) {
        // Search + confirmation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f).height(38.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color(0xFF22242B))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search, "Search",
                    tint = Color(0xFFA5A8B2), modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text("Search filters", color = Color(0xFFA5A8B2), fontSize = 13.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("filters_search")
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF22242B))
            ) {
                Icon(Icons.Default.Check, "Confirm", tint = Color(0xFF26E5D7), modifier = Modifier.size(20.dp))
            }
        }

        // Category tabs (same look as the effects drawer)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedTab = index
                        searchQuery = ""
                    }.padding(vertical = 6.dp).testTag("filter_tab_$index")
                ) {
                    Text(
                        tab,
                        color = if (active) Color.White else Color(0xFFA5A8B2),
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.width(22.dp).height(2.5.dp).clip(RoundedCornerShape(1.5.dp))
                            .background(if (active) Color(0xFF26E5D7) else Color.Transparent)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 3-column card grid (identical layout to the effects drawer)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("filters_grid")
        ) {
            if (showLuts) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2026))
                            .border(1.dp, Color(0xFF2D303B), RoundedCornerShape(8.dp))
                            .clickable { onOpenLutImport() }.padding(8.dp)
                            .testTag("lut_import_open")
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF262832)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color(0xFF26E5D7), fontSize = 24.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Import .cube", color = Color.White, fontSize = 11.sp)
                    }
                }
                importedLuts.forEach { lut ->
                    item { LutCard(lut, lut.id == activeLutId, onSelectLut) }
                }
            } else {
                visibleFilters.forEach { filter ->
                    item { FilterCard(filter, filter == activeFilter, onSelectFilter) }
                }
            }
        }
        } else if (panel == 1) {
            AdjustPanel(
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                vignette = vignette,
                onUpdate = onUpdateAdjustments,
                onReset = onResetAdjustments,
                onNotify = onNotify
            )
        } else {
            VideoQualityPanel(
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                vignette = vignette,
                onUpdate = onUpdateAdjustments,
                onReset = onResetAdjustments,
                onNotify = onNotify
            )
        }
    }
}

/** Both panels share these circular chips and the single slider below them. */
private data class AdjustChip(
    val label: String,
    /** The real engine parameter this chip drives, or null when none exists yet. */
    val param: String?
)

private val SMART_CHIPS = listOf(
    AdjustChip("Auto adjust", "auto"),
    AdjustChip("Color match", "auto"),
    AdjustChip("Color correction", "auto"),
    AdjustChip("Brightness", "brightness"),
    AdjustChip("Contrast", "contrast"),
    AdjustChip("Saturation", "saturation")
)

private val CUSTOMIZE_CHIPS = listOf(
    AdjustChip("Brilliance", "contrast"),
    AdjustChip("Sharpen", null),
    AdjustChip("Clarity", "contrast"),
    AdjustChip("HSL", "saturation"),
    AdjustChip("Graphics", null),
    AdjustChip("Color wheel", "saturation"),
    AdjustChip("Highlight", "brightness"),
    AdjustChip("Shadows", "brightness"),
    AdjustChip("Whites", "brightness"),
    AdjustChip("Blacks", "brightness"),
    AdjustChip("Temp", null),
    AdjustChip("Hue", null),
    AdjustChip("Fade", "saturation"),
    AdjustChip("Vignette", "vignette"),
    AdjustChip("Grain", null)
)

private val QUALITY_CHIPS = listOf(
    AdjustChip("Auto enhance", "auto"),
    AdjustChip("Enhance quality", "auto"),
    AdjustChip("Reduce image noise", null),
    AdjustChip("Auto adjust", "auto"),
    AdjustChip("Stabilize", null),
    AdjustChip("Optical flow", null),
    AdjustChip("Super resolution", null),
    AdjustChip("Remove flickers", null)
)

@Composable
private fun ChipRow(
    chips: List<AdjustChip>,
    selected: AdjustChip,
    onSelect: (AdjustChip) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        chips.forEach { chip ->
            val active = chip == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(chip) }.testTag("adjust_chip_${chip.label}")
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(if (active) Color(0xFF26E5D7).copy(alpha = 0.15f) else Color(0xFF22242B))
                        .border(1.dp, if (active) Color(0xFF26E5D7) else Color(0xFF3A3C46), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        chip.label.take(1),
                        color = if (active) Color(0xFF26E5D7) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    chip.label,
                    color = if (active) Color(0xFF26E5D7) else Color(0xFFC6C8D0),
                    fontSize = 10.sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    androidx.compose.material3.Slider(
        value = value.coerceIn(range),
        onValueChange = onChange,
        valueRange = range,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("adjust_slider")
    )
}

@Composable
private fun PanelFooter(onReset: () -> Unit, onNotify: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("↺  Reset", color = Color(0xFFA5A8B2), fontSize = 12.sp,
            modifier = Modifier.clickable { onReset() }.testTag("adjust_reset"))
        Spacer(Modifier.width(24.dp))
        Text("▤  Apply to all", color = Color(0xFFA5A8B2), fontSize = 12.sp,
            modifier = Modifier.clickable { onNotify("Applied to all clips") }
                .testTag("adjust_apply_all"))
    }
}

private fun applyPreset(onUpdate: (Float, Float, Float, Float) -> Unit) {
    // Real one-shot grade: readable exposure, a little more contrast and colour.
    onUpdate(0.05f, 1.12f, 1.08f, 0f)
}

@Composable
private fun AdjustPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    vignette: Float,
    onUpdate: (Float, Float, Float, Float) -> Unit,
    onReset: () -> Unit,
    onNotify: (String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    val chips = if (subTab == 0) SMART_CHIPS else CUSTOMIZE_CHIPS
    var selected by remember(subTab) { mutableStateOf(chips.first()) }

    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        listOf("Smart", "Customize").forEachIndexed { index, label ->
            Text(
                label,
                color = if (subTab == index) Color.White else Color(0xFFA5A8B2),
                fontSize = 12.sp,
                fontWeight = if (subTab == index) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clickable { subTab = index; selected = chips.first() }
                    .padding(end = 20.dp).testTag("adjust_subtab_$index")
            )
        }
    }

    ChipRow(chips, selected) { chip ->
        selected = chip
        if (chip.param == "auto") {
            applyPreset(onUpdate)
            onNotify("${chip.label} applied")
        } else if (chip.param == null) {
            onNotify("${chip.label} needs a dedicated engine model - not applied")
        }
    }

    when (selected.param) {
        "brightness" -> ParameterSlider(brightness, -1f..1f) { onUpdate(it, contrast, saturation, vignette) }
        "contrast" -> ParameterSlider(contrast, 0f..2f) { onUpdate(brightness, it, saturation, vignette) }
        "saturation" -> ParameterSlider(saturation, 0f..2f) { onUpdate(brightness, contrast, it, vignette) }
        "vignette" -> ParameterSlider(vignette, 0f..1f) { onUpdate(brightness, contrast, saturation, it) }
        "auto" -> ParameterSlider(contrast, 0f..2f) { onUpdate(brightness, it, saturation, vignette) }
        else -> Text(
            "${selected.label} has no on-device implementation yet - nothing was changed.",
            color = Color(0xFFA5A8B2), fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        )
    }
    PanelFooter(onReset, onNotify)
}

@Composable
private fun VideoQualityPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    vignette: Float,
    onUpdate: (Float, Float, Float, Float) -> Unit,
    onReset: () -> Unit,
    onNotify: (String) -> Unit
) {
    var selected by remember { mutableStateOf(QUALITY_CHIPS.first()) }
    ChipRow(QUALITY_CHIPS, selected) { chip ->
        selected = chip
        if (chip.param == "auto") {
            applyPreset(onUpdate)
            onNotify("${chip.label} applied")
        } else {
            onNotify("${chip.label} needs a GPU/ML enhancement model - not applied")
        }
    }
    ParameterSlider(contrast, 0f..2f) { onUpdate(brightness, it, saturation, vignette) }
    PanelFooter(onReset, onNotify)
}

@Composable
private fun FilterCard(
    filter: VideoFilter,
    selected: Boolean,
    onSelect: (VideoFilter) -> Unit
) {
    val grade = FilterCatalog.gradeOf(filter)
    val colors = if (filter == VideoFilter.NONE) {
        listOf(Color(0xFF444751), Color(0xFF8A8D99))
    } else {
        listOf(
            Color((grade?.cardColorA ?: 0xFF2B2B33L).toInt()),
            Color((grade?.cardColorB ?: 0xFFA5A8B2L).toInt())
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF26E5D7).copy(alpha = 0.18f) else Color(0xFF1E2026))
            .border(
                1.dp,
                if (selected) Color(0xFF26E5D7) else Color(0xFF2D303B),
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelect(filter) }
            .padding(8.dp)
            .testTag("filter_card_${filter.name}")
    ) {
        val isHit = grade?.tabs?.contains("Hit") == true
        Box(
            Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isHit && grade != null) {
                // The same Hit.jpg for every card, tinted by that filter's OWN
                // grade - so the thumbnail shows what the filter actually does.
                AsyncImage(
                    model = FilterCatalog.HIT_COVER_PATH,
                    contentDescription = filter.displayName,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(grade.toPreviewMatrix())),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(80.dp)
                        .background(Brush.linearGradient(colors)),
                    contentAlignment = Alignment.Center
                ) { }
            }
            if (selected) {
                Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            filter.displayName,
            color = if (selected) Color(0xFF26E5D7) else Color.White,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun LutCard(lut: ImportedLut, selected: Boolean, onSelect: (ImportedLut?) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF26E5D7).copy(alpha = 0.18f) else Color(0xFF1E2026))
            .border(1.dp, if (selected) Color(0xFF26E5D7) else Color(0xFF2D303B), RoundedCornerShape(8.dp))
            .clickable { onSelect(if (selected) null else lut) }.padding(8.dp)
            .testTag("lut_${lut.id}")
    ) {
        Box(
            Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFF5E9C), Color(0xFF26E5D7), Color(0xFF7C5CFF)))),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            lut.displayName,
            color = if (selected) Color(0xFF26E5D7) else Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
