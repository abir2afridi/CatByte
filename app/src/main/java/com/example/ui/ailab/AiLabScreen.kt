package com.example.ui.ailab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiConfig
import com.example.ui.theme.PencilTheme
import java.util.Locale

/**
 * The AI Lab: a home-screen tab that groups every AI tool in the app.
 *
 * The page itself is read-only glue — the real work lives in the tool sheets
 * ([AiLabSheets]) and the engines behind them. It stays honest about what each
 * tool needs: an endpoint, nothing, or a model family the app cannot provide.
 */
@Composable
fun AiLabScreen(
    aiConfig: AiConfig,
    history: List<AiLabRun>,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit,
    onCreateProjectWithScript: (title: String, lines: List<String>) -> Unit,
    onRunRecorded: (toolId: String, title: String, summary: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(AiLabCategory.ALL) }
    var activeToolId by rememberSaveable { mutableStateOf<String?>(null) }

    val visibleTools = rememberTools(category, searchQuery)
    val activeTool = AiLabTools.firstOrNull { it.id == activeToolId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        AiLabHeader(
            configured = aiConfig.isConfigured,
            onOpenSettings = onOpenSettings
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ai_lab_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item {
                AiLabSearchField(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it }
                )
            }
            item { AiLabHeroCard(configured = aiConfig.isConfigured, onOpenSettings = onOpenSettings) }
            item {
                AiLabCategoryChips(
                    selected = category,
                    onSelect = { category = it }
                )
            }
            if (visibleTools.isEmpty()) {
                item { AiLabEmptyState(searchQuery) }
            } else {
                items(visibleTools.chunked(2), key = { row -> row.joinToString("_") { it.id } }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { tool ->
                            AiLabToolCard(
                                tool = tool,
                                configured = aiConfig.isConfigured,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeToolId = tool.id
                                    onRunRecorded(tool.id, tool.title, tool.description)
                                }
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                item { AiLabHistoryHeader(onClearHistory = onClearHistory) }
                items(history, key = { it.atMs.toString() + it.toolId }) { run ->
                    // Tapping a past run re-opens that tool's sheet.
                    AiLabHistoryRow(run, onClick = { activeToolId = run.toolId })
                }
            }
        }
    }

    activeTool?.let { tool ->
        AiLabToolSheet(
            tool = tool,
            aiConfig = aiConfig,
            onOpenSettings = onOpenSettings,
            onDismiss = { activeToolId = null },
            onCreateProjectWithScript = onCreateProjectWithScript
        )
    }
}

/**
 * Opens the sheet that belongs to [tool].
 *
 * Tools that need a model family the app cannot provide land in
 * [NotAvailableSheet] instead of being silently dead cards.
 */
@Composable
private fun AiLabToolSheet(
    tool: AiLabTool,
    aiConfig: AiConfig,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    onCreateProjectWithScript: (title: String, lines: List<String>) -> Unit
) {
    when (tool.id) {
        "script" -> ScriptWriterSheet(
            config = aiConfig,
            onDismiss = onDismiss,
            onCreateProject = onCreateProjectWithScript
        )
        "titles" -> TitlesHooksSheet(config = aiConfig, onDismiss = onDismiss)
        "translate" -> TranslatorSheet(config = aiConfig, onDismiss = onDismiss)
        "hashtags" -> HashtagsSheet(config = aiConfig, onDismiss = onDismiss)
        "chat" -> AssistantSheet(config = aiConfig, onDismiss = onDismiss)
        "image" -> ImageGeneratorSheet(config = aiConfig, onDismiss = onDismiss)
        "upscale" -> UpscalerSheet(onDismiss = onDismiss)
        "captions" -> CaptionsSheet(config = aiConfig, onDismiss = onDismiss)
        "voiceover" -> VoiceoverSheet(onDismiss = onDismiss)
        "music" -> MusicSheet(onDismiss = onDismiss)
        "beats" -> BeatFinderSheet(onDismiss = onDismiss)
        else -> NotAvailableSheet(tool = tool, onOpenSettings = onOpenSettings, onDismiss = onDismiss)
    }
}

/** Honest answer for tools that need a model the app does not ship. */
@Composable
private fun NotAvailableSheet(
    tool: AiLabTool,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AiLabSheet(
        title = tool.title,
        subtitle = tool.description,
        accent = tool.accent,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            Spacer(modifier = Modifier.height(4.dp))
            AiLabErrorText(
                "${tool.title} needs a specialised model (segmentation, lip sync or avatar " +
                    "generation) that a chat endpoint cannot provide. The text, image and speech " +
                    "tools in this lab do work with the endpoint you configure in Settings → AI."
            )
            Spacer(modifier = Modifier.height(12.dp))
            AiLabSecondaryButton(label = "Open AI settings", onClick = {
                onOpenSettings()
                onDismiss()
            })
        }
    }
}

@Composable
private fun rememberTools(
    category: AiLabCategory,
    query: String
): List<AiLabTool> {
    return androidx.compose.runtime.remember(category, query) {
        AiLabTools.filtered(category, query)
    }
}

@Composable
private fun AiLabHeader(
    configured: Boolean,
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(colors.accentPurple.copy(alpha = 0.28f), colors.accentCyan.copy(alpha = 0.28f))
                    )
                )
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = colors.accentPurple,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "AI Lab",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (configured) "Connected to your AI endpoint" else "AI tools for your videos",
                color = if (configured) colors.accentCyan else colors.textTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("ai_lab_settings")) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "AI settings",
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AiLabSearchField(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    val colors = PencilTheme.colors
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("ai_lab_search"),
        placeholder = { Text("Search AI tools…", color = colors.textTertiary, fontSize = 13.sp) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentCyan,
            unfocusedBorderColor = colors.borderSubtle,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accentCyan
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
    )
}

@Composable
private fun AiLabHeroCard(
    configured: Boolean,
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.accentPurple.copy(alpha = 0.22f),
                        colors.accentCyan.copy(alpha = 0.18f),
                        colors.accentPink.copy(alpha = 0.16f)
                    )
                )
            )
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .clickable(enabled = !configured) { onOpenSettings() }
            .padding(16.dp)
            .testTag("ai_lab_hero")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Turn ideas into edits",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (configured) {
                        "Your endpoint is ready. Pick a tool below to start."
                    } else {
                        "Connect an OpenAI-compatible endpoint in Settings → AI to unlock the online tools. The on-device tools already work."
                    },
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (!configured) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentCyan.copy(alpha = 0.18f))
                            .border(1.dp, colors.accentCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Connect AI",
                            color = colors.accentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoFixHigh,
                    contentDescription = null,
                    tint = colors.accentPurple,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun AiLabCategoryChips(
    selected: AiLabCategory,
    onSelect: (AiLabCategory) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AiLabCategory.entries.toList(), key = { it.name }) { entry ->
            AiLabChip(
                label = entry.label,
                selected = entry == selected,
                onClick = { onSelect(entry) }
            )
        }
    }
}

@Composable
internal fun AiLabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) colors.accentCyan.copy(alpha = 0.18f) else colors.surface
            )
            .border(
                1.dp,
                if (selected) colors.accentCyan else colors.borderSubtle,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) colors.accentCyan else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AiLabToolCard(
    tool: AiLabTool,
    configured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val usable = tool.isRunnable && (!tool.needsEndpoint || configured)
    Box(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(
                1.dp,
                if (usable) colors.borderSubtle else colors.borderSubtle.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = tool.isRunnable) { onClick() }
            .padding(12.dp)
            .testTag("ai_lab_tool_${tool.id}")
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tool.accent.soft()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        tool.icon,
                        contentDescription = null,
                        tint = tool.accent.color(),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AiLabBadge(tool = tool, configured = configured)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                tool.title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                if (tool.needsEndpoint && !configured) "Add an AI endpoint in Settings to use this" else tool.description,
                color = colors.textTertiary,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AiLabBadge(tool: AiLabTool, configured: Boolean) {
    val colors = PencilTheme.colors
    val tint = when {
        !tool.isRunnable -> colors.textTertiary
        tool.needsEndpoint && !configured -> colors.accentYellow
        else -> colors.accentGreen
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(tool.badge, color = tint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AiLabEmptyState(query: String) {
    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.LinkOff,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            if (query.isBlank()) "No tools to show" else "No tools match \"$query\"",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AiLabHistoryHeader(onClearHistory: () -> Unit) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.History,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Recently used",
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClearHistory) {
            Text("Clear", color = colors.textTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AiLabHistoryRow(run: AiLabRun, onClick: () -> Unit) {
    val colors = PencilTheme.colors
    val tool = AiLabTools.firstOrNull { it.id == run.toolId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background((tool?.accent ?: AiLabAccent.CYAN).soft()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tool?.icon ?: Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = (tool?.accent ?: AiLabAccent.CYAN).color(),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                run.title.ifBlank { "AI run" },
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (run.summary.isNotBlank()) {
                Text(
                    run.summary,
                    color = colors.textTertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            formatRunTime(run.atMs),
            color = colors.textTertiary,
            fontSize = 9.sp
        )
    }
}

/** Short relative timestamp for the recently-used list. */
internal fun formatRunTime(atMs: Long): String {
    if (atMs <= 0L) return ""
    val delta = System.currentTimeMillis() - atMs
    val minutes = delta / 60_000L
    return when {
        delta < 60_000L -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> {
            val days = minutes / (60 * 24)
            if (days == 1L) "yesterday" else "${days}d ago"
        }
    }
}
