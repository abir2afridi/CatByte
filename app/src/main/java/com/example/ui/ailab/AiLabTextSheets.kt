package com.example.ui.ailab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiClient
import com.example.data.ai.AiConfig
import com.example.data.ai.AiResult
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Shared busy/error state for the prompt-driven tools. */
private class TextToolState {
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

/**
 * Runs [block] once, flipping [TextToolState.busy] around it and storing the
 * failure reason in [TextToolState.error]. A configured endpoint is required;
 * when there is none the tool explains that instead of firing a doomed request.
 */
private fun runTool(
    state: TextToolState,
    scope: CoroutineScope,
    config: AiConfig,
    block: suspend () -> Unit
) {
    if (state.busy) return
    if (!config.isConfigured) {
        state.error = AiClient.NO_ENDPOINT
        return
    }
    state.busy = true
    state.error = null
    scope.launch {
        try {
            block()
        } finally {
            state.busy = false
        }
    }
}

// ---------------------------------------------------------------------------
// Script Writer
// ---------------------------------------------------------------------------

@Composable
internal fun ScriptWriterSheet(
    config: AiConfig,
    onDismiss: () -> Unit,
    onCreateProject: (title: String, lines: List<String>) -> Unit
) {
    val colors = PencilTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = remember { TextToolState() }
    var topic by rememberSaveable { mutableStateOf("") }
    var tone by rememberSaveable { mutableStateOf(AiLabEngine.ScriptTones.first()) }
    var lines by rememberSaveable { mutableStateOf(listOf<String>()) }

    AiLabSheet(
        title = "Script Writer",
        subtitle = "A ready-to-read script for a vertical short",
        accent = AiLabAccent.CYAN,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("TOPIC")
            AiLabInputField(
                value = topic,
                onValueChange = { topic = it },
                placeholder = "e.g. 5 tips for shooting better phone video"
            )
            AiLabSectionLabel("TONE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiLabEngine.ScriptTones.forEach { option ->
                    AiLabChip(
                        label = option,
                        selected = option == tone,
                        onClick = { tone = option }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AiLabPrimaryButton(
                label = "Generate script",
                enabled = topic.isNotBlank(),
                busy = state.busy,
                onClick = {
                    runTool(state, scope, config) {
                        when (val result = AiLabEngine.writeScript(config, topic, tone)) {
                            is AiResult.Failure -> state.error = result.message
                            is AiResult.Ok -> {
                                lines = result.value
                                state.error = null
                            }
                        }
                    }
                }
            )
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            if (lines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .testTag("ai_lab_script_result")
                ) {
                    Column {
                        lines.forEachIndexed { index, line ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    "${index + 1}",
                                    color = colors.accentCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(18.dp)
                                )
                                Text(
                                    line,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (index != lines.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiLabSecondaryButton(
                        label = "Copy",
                        modifier = Modifier.weight(1f),
                        onClick = { copyToClipboard(context, lines.joinToString("\n")) }
                    )
                    AiLabSecondaryButton(
                        label = "New project",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onCreateProject(topic.trim(), lines)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Titles & Hooks
// ---------------------------------------------------------------------------

@Composable
internal fun TitlesHooksSheet(
    config: AiConfig,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = remember { TextToolState() }
    var topic by rememberSaveable { mutableStateOf("") }
    var titles by remember { mutableStateOf(listOf<String>()) }
    var hooks by remember { mutableStateOf(listOf<String>()) }

    AiLabSheet(
        title = "Titles & Hooks",
        subtitle = "Scroll-stopping titles and opening lines",
        accent = AiLabAccent.PINK,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("TOPIC")
            AiLabInputField(
                value = topic,
                onValueChange = { topic = it },
                placeholder = "e.g. street food tour in Dhaka"
            )
            Spacer(modifier = Modifier.height(12.dp))
            AiLabPrimaryButton(
                label = "Generate",
                enabled = topic.isNotBlank(),
                busy = state.busy,
                onClick = {
                    runTool(state, scope, config) {
                        when (val value = AiLabEngine.titlesAndHooks(config, topic)) {
                            is AiResult.Failure -> state.error = value.message
                            is AiResult.Ok -> {
                                titles = value.value.titles
                                hooks = value.value.hooks
                                state.error = null
                            }
                        }
                    }
                }
            )
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            if (titles.isNotEmpty() || hooks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                titles.forEachIndexed { index, title ->
                    AiLabResultCard(
                        title = "TITLE ${index + 1}",
                        text = title,
                        onCopy = { copyToClipboard(context, title) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                hooks.forEachIndexed { index, hook ->
                    AiLabResultCard(
                        title = "HOOK ${index + 1}",
                        text = hook,
                        onCopy = { copyToClipboard(context, hook) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AiLabSecondaryButton(
                    label = "Copy all",
                    onClick = { copyToClipboard(context, (titles + hooks).joinToString("\n")) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Translator
// ---------------------------------------------------------------------------

@Composable
internal fun TranslatorSheet(
    config: AiConfig,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = remember { TextToolState() }
    var source by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf(AiLabEngine.TranslationTargets.first()) }
    var translated by remember { mutableStateOf("") }

    AiLabSheet(
        title = "Translator",
        subtitle = "Translate a script and keep the line order",
        accent = AiLabAccent.BLUE,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("TEXT")
            AiLabInputField(
                value = source,
                onValueChange = { source = it },
                placeholder = "Paste the lines you want to translate",
                minLines = 4,
                maxLines = 10
            )
            AiLabSectionLabel("TRANSLATE TO")
            AiLabEngine.TranslationTargets.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { option ->
                        AiLabChip(
                            label = option,
                            selected = option == target,
                            onClick = { target = option }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            AiLabPrimaryButton(
                label = "Translate",
                enabled = source.isNotBlank(),
                busy = state.busy,
                onClick = {
                    runTool(state, scope, config) {
                        when (val value = AiLabEngine.translate(config, source, target)) {
                            is AiResult.Failure -> state.error = value.message
                            is AiResult.Ok -> {
                                translated = value.value.text
                                state.error = null
                            }
                        }
                    }
                }
            )
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            if (translated.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AiLabResultCard(
                    title = "TRANSLATION",
                    text = translated,
                    onCopy = { copyToClipboard(context, translated) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hashtags & SEO
// ---------------------------------------------------------------------------

@Composable
internal fun HashtagsSheet(
    config: AiConfig,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = PencilTheme.colors
    val state = remember { TextToolState() }
    var topic by rememberSaveable { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var description by remember { mutableStateOf("") }

    AiLabSheet(
        title = "Hashtags & SEO",
        subtitle = "A tag set and a search-friendly description",
        accent = AiLabAccent.GREEN,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("TOPIC")
            AiLabInputField(
                value = topic,
                onValueChange = { topic = it },
                placeholder = "e.g. budget travel vlog"
            )
            Spacer(modifier = Modifier.height(12.dp))
            AiLabPrimaryButton(
                label = "Generate",
                enabled = topic.isNotBlank(),
                busy = state.busy,
                onClick = {
                    runTool(state, scope, config) {
                        when (val value = AiLabEngine.hashtags(config, topic)) {
                            is AiResult.Failure -> state.error = value.message
                            is AiResult.Ok -> {
                                tags = value.value.tags
                                description = value.value.description
                                state.error = null
                            }
                        }
                    }
                }
            )
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            if (tags.isNotEmpty() || description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                if (tags.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "TAGS",
                                color = colors.textTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                tags.joinToString(" ") { "#$it" },
                                color = colors.accentCyan,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (description.isNotBlank()) {
                    AiLabResultCard(
                        title = "DESCRIPTION",
                        text = description,
                        onCopy = { copyToClipboard(context, description) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AiLabSecondaryButton(
                    label = "Copy all",
                    onClick = {
                        copyToClipboard(
                            context,
                            (tags.joinToString(" ") { "#$it" } + "\n" + description).trim()
                        )
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AI Assistant
// ---------------------------------------------------------------------------

@Composable
internal fun AssistantSheet(
    config: AiConfig,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = PencilTheme.colors
    val turns = remember { mutableStateOf(listOf<AiLabChatTurn>()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun send() {
        val message = draft.trim()
        if (message.isEmpty() || busy) return
        if (!config.isConfigured) {
            error = AiClient.NO_ENDPOINT
            return
        }
        busy = true
        error = null
        val history = turns.value
        turns.value = history + AiLabChatTurn("user", message)
        draft = ""
        scope.launch {
            try {
                when (val value = AiLabEngine.chat(config, history, message)) {
                    is AiResult.Failure -> {
                        error = value.message
                        turns.value = history
                    }
                    is AiResult.Ok -> {
                        turns.value = history +
                            AiLabChatTurn("user", message) +
                            AiLabChatTurn("assistant", value.value)
                        error = null
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    AiLabSheet(
        title = "AI Assistant",
        subtitle = "Ideas, research and jokes — nothing is saved",
        accent = AiLabAccent.PURPLE,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (turns.value.isEmpty()) {
                    Text(
                        "Ask anything about your video: hooks, titles, structure, " +
                            "what to film next. The assistant reads the whole conversation.",
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                turns.value.forEach { turn ->
                    val isUser = turn.role == "user"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .width(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isUser) colors.accentCyan.copy(alpha = 0.16f)
                                    else colors.surfaceVariant.copy(alpha = 0.7f)
                                )
                                .border(
                                    1.dp,
                                    if (isUser) colors.accentCyan.copy(alpha = 0.35f) else colors.borderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                                .testTag("ai_lab_chat_${turn.role}")
                        ) {
                            Text(
                                turn.text,
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    AiLabInputField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = if (config.isConfigured) {
                            "Type a message…"
                        } else {
                            "Add an AI endpoint in Settings first"
                        },
                        maxLines = 4
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AiLabPrimaryButton(
                    label = "Send",
                    enabled = draft.isNotBlank(),
                    busy = busy,
                    modifier = Modifier.width(96.dp),
                    onClick = { send() }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (turns.value.isNotEmpty()) {
                AiLabSecondaryButton(
                    label = "Copy last answer",
                    enabled = turns.value.lastOrNull()?.role == "assistant",
                    onClick = {
                        val last = turns.value.lastOrNull { it.role == "assistant" }?.text
                        if (last != null) copyToClipboard(context, last)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
