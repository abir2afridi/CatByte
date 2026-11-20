package com.example.ui.ailab

import android.content.Context
import java.nio.charset.StandardCharsets
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiAudioExtractor
import com.example.data.MediaDurationFallback
import com.example.data.MediaStoreSaver
import com.example.data.PcmDecoder
import com.example.data.SampleAudioGenerator
import com.example.data.SpeechSynthesizer
import com.example.data.ai.AiClient
import com.example.data.ai.AiConfig
import com.example.data.ai.AiImageSize
import com.example.data.ai.AiResult
import com.example.data.ai.Transcript
import com.example.engine.AudioAnalysis
import com.example.engine.ImageUpscaler
import com.example.engine.MusicGenerator
import com.example.engine.SubtitleCue
import com.example.engine.SubtitleIO
import com.example.ui.theme.PencilTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** `mm:ss` formatting for cue and silence lists. */
private fun formatCueTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val minutes = total / 60L
    val seconds = total % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

/** `1.2s` style formatting used by the analysis read-out. */
internal fun formatSeconds(ms: Long): String =
    String.format(Locale.US, "%.1fs", ms / 1000f)

/** Display name for a picked document, or empty when it cannot be read. */
private fun displayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: uri.lastPathSegment
    }.getOrNull().orEmpty()
}

/** Renders a generated PNG/JPEG byte array as a preview bitmap. */
private suspend fun decodeBitmap(bytes: ByteArray): Bitmap? =
    withContext(Dispatchers.IO) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

// ---------------------------------------------------------------------------
// Image Generator (endpoint)
// ---------------------------------------------------------------------------

@Composable
internal fun ImageGeneratorSheet(
    config: AiConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PencilTheme.colors
    var prompt by rememberSaveable { mutableStateOf("") }
    var size by rememberSaveable { mutableStateOf(AiImageSize.SQUARE) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bytes) {
        bitmap = bytes?.let { decodeBitmap(it) }
    }

    val generate: () -> Unit = {
        if (!busy && prompt.isNotBlank()) {
            if (!config.isConfigured) {
                error = AiClient.NO_ENDPOINT
            } else {
                busy = true
                error = null
                scope.launch {
                    try {
                        when (val result = AiLabEngine.generateImage(config, prompt, size)) {
                            is AiResult.Failure -> error = result.message
                            is AiResult.Ok -> {
                                bytes = result.value
                                error = null
                            }
                        }
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    AiLabSheet(
        title = "Image Generator",
        subtitle = "A cover, background or sticker from one prompt",
        accent = AiLabAccent.PURPLE,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("PROMPT")
            AiLabInputField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = "e.g. neon city skyline at night, cinematic",
                minLines = 2,
                maxLines = 5
            )
            AiLabSectionLabel("SHAPE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Square" to AiImageSize.SQUARE,
                    "Portrait" to AiImageSize.PORTRAIT,
                    "Landscape" to AiImageSize.LANDSCAPE
                ).forEach { (label, value) ->
                    AiLabChip(
                        label = label,
                        selected = size == value,
                        onClick = { size = value }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AiLabPrimaryButton(
                label = "Generate image",
                enabled = prompt.isNotBlank(),
                busy = busy,
                onClick = generate
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            bitmap?.let { image ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                        .testTag("ai_lab_image_preview")
                ) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "Generated image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiLabSecondaryButton(
                        label = "Save to gallery",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val data = bytes
                            val saved = data?.let {
                                MediaStoreSaver.saveImage(
                                    context, it, "catbyte_ai_${System.currentTimeMillis()}"
                                )
                            }
                            copyToClipboard(
                                context, "",
                                if (saved != null) "Saved to Pictures/CatByte" else "Could not save the image"
                            )
                        }
                    )
                    AiLabSecondaryButton(
                        label = "Copy prompt",
                        modifier = Modifier.weight(1f),
                        onClick = { copyToClipboard(context, prompt) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Image Upscaler (on device)
// ---------------------------------------------------------------------------

@Composable
internal fun UpscalerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PencilTheme.colors
    var factor by rememberSaveable { mutableStateOf(2) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<Bitmap?>(null) }

    fun upscaleAction() {
        val image = source ?: return
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val upscaled = withContext(Dispatchers.IO) {
                    val buffer = IntArray(image.width * image.height)
                    image.getPixels(buffer, 0, image.width, 0, 0, image.width, image.height)
                    ImageUpscaler.upscale(buffer, image.width, image.height, factor)
                }
                result = Bitmap.createBitmap(
                    upscaled.pixels, upscaled.width, upscaled.height, Bitmap.Config.ARGB_8888
                )
            } catch (failure: Throwable) {
                error = failure.message ?: "The upscale failed"
            } finally {
                busy = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            busy = true
            error = null
            scope.launch {
                try {
                    val decoded = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    if (decoded == null) {
                        error = "Could not read that image"
                    } else {
                        source = decoded
                        result = null
                    }
                } finally {
                    busy = false
                }
            }
        }
    }

    AiLabSheet(
        title = "Image Upscaler",
        subtitle = "Lanczos 2x-4x, fully on device",
        accent = AiLabAccent.TEAL,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("SOURCE IMAGE")
            AiLabSecondaryButton(
                label = if (source == null) "Choose image" else "Change image",
                enabled = !busy,
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            AiLabSectionLabel("SCALE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 3, 4).forEach { option ->
                    AiLabChip(
                        label = "${option}x",
                        selected = factor == option,
                        onClick = { factor = option }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AiLabPrimaryButton(
                label = if (result == null) "Upscale" else "Upscale again",
                enabled = source != null,
                busy = busy,
                onClick = { upscaleAction() }
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            result?.let { upscaled ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                        .testTag("ai_lab_upscale_preview")
                ) {
                    Image(
                        bitmap = upscaled.asImageBitmap(),
                        contentDescription = "Upscaled image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${upscaled.width} x ${upscaled.height} px",
                    color = colors.textTertiary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                AiLabSecondaryButton(
                    label = "Save to gallery",
                    onClick = {
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                val bytes = ByteArrayOutputStream().use { out ->
                                    upscaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    out.toByteArray()
                                }
                                MediaStoreSaver.saveImage(
                                    context, bytes, "catbyte_upscale_${System.currentTimeMillis()}"
                                )
                            }
                            copyToClipboard(
                                context, "",
                                if (saved != null) "Saved to Pictures/CatByte" else "Could not save the image"
                            )
                        }
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Auto Captions (endpoint)
// ---------------------------------------------------------------------------

@Composable
internal fun CaptionsSheet(
    config: AiConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PencilTheme.colors
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var transcript by remember { mutableStateOf<Transcript?>(null) }
    var pendingSrt by remember { mutableStateOf<String?>(null) }

    val transcribe: (Uri) -> Unit = { uri ->
        if (!busy) {
            if (!config.isConfigured) {
                error = AiClient.NO_ENDPOINT
            } else {
                busy = true
                error = null
                transcript = null
                scope.launch {
                    try {
                        val duration = withContext(Dispatchers.IO) {
                            MediaDurationFallback.durationMs(context, uri)
                                .takeIf { it > 0 } ?: 600_000L
                        }
                        when (
                            val extracted = AiAudioExtractor.extract(
                                context, uri.toString(), 0L, duration
                            )
                        ) {
                            is AiAudioExtractor.Result.Failure -> error = extracted.message
                            is AiAudioExtractor.Result.Ok -> {
                                val audio = withContext(Dispatchers.IO) { extracted.file.readBytes() }
                                when (
                                    val result = AiClient.transcribe(
                                        config = config,
                                        audio = audio,
                                        fileName = extracted.file.name,
                                        fallbackDurationMs = extracted.durationMs
                                    )
                                ) {
                                    is AiResult.Failure -> error = result.message
                                    is AiResult.Ok -> transcript = result.value
                                }
                                runCatching { extracted.file.delete() }
                            }
                        }
                    } catch (failure: Throwable) {
                        error = failure.message ?: "Transcription failed"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) transcribe(uri)
    }

    val export = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { destination ->
        val srt = pendingSrt
        if (destination != null && srt != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(destination)?.use { out ->
                            out.write(srt.toByteArray(StandardCharsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                copyToClipboard(context, "", if (ok) "SRT saved" else "Could not save the SRT")
            }
        }
    }

    AiLabSheet(
        title = "Auto Captions",
        subtitle = "Transcribe a video and export timed SRT subtitles",
        accent = AiLabAccent.GREEN,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("SOURCE")
            AiLabSecondaryButton(
                label = "Choose a video or audio file",
                enabled = !busy,
                onClick = { picker.launch(arrayOf("video/*", "audio/*")) }
            )
            if (busy) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Extracting audio and transcribing…",
                    color = colors.textTertiary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
            transcript?.let { value ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .testTag("ai_lab_captions_result")
                ) {
                    Column {
                        Text(
                            "${value.segments.size} cues",
                            color = colors.textTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        value.segments.take(12).forEach { segment ->
                            Text(
                                "${formatCueTime(segment.startMs)} → ${formatCueTime(segment.endMs)}",
                                color = colors.accentGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                segment.text,
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (value.segments.size > 12) {
                            Text(
                                "…and ${value.segments.size - 12} more cues",
                                color = colors.textTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                AiLabSecondaryButton(
                    label = "Export SRT",
                    onClick = {
                        val cues = value.segments.map { SubtitleCue(it.startMs, it.endMs, it.text) }
                        pendingSrt = SubtitleIO.toSrt(cues)
                        export.launch("catbyte_captions.srt")
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AI Voiceover (on device TTS)
// ---------------------------------------------------------------------------

@Composable
internal fun VoiceoverSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PencilTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var ready by rememberSaveable { mutableStateOf(false) }
    val synthesizer = remember { SpeechSynthesizer(context) }

    DisposableEffect(Unit) {
        synthesizer.initialise { available ->
            ready = available
            if (!available) {
                error = "The text to speech engine is not available on this device."
            }
        }
        onDispose { synthesizer.release() }
    }

    val generate: () -> Unit = {
        if (!busy && text.isNotBlank() && ready) {
            busy = true
            error = null
            status = "Speaking…"
            synthesizer.synthesize(text) { file, failure ->
                busy = false
                if (file != null) {
                    status = "Voice-over generated"
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            MediaStoreSaver.saveAudioFile(
                                context,
                                file,
                                "catbyte_voice_${System.currentTimeMillis()}",
                                "audio/wav"
                            )
                        }
                        status = if (saved != null) {
                            "Voice-over saved to Music/CatByte"
                        } else {
                            "Generated at ${file.absolutePath}"
                        }
                    }
                } else {
                    status = ""
                    error = failure ?: "The voice-over could not be generated."
                }
            }
        }
    }

    AiLabSheet(
        title = "AI Voiceover",
        subtitle = "Reads your script with the on-device voice engine",
        accent = AiLabAccent.PINK,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("SCRIPT")
            AiLabInputField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Write what the narrator should say",
                minLines = 4,
                maxLines = 10
            )
            Spacer(modifier = Modifier.height(12.dp))
            AiLabPrimaryButton(
                label = "Generate voice-over",
                enabled = text.isNotBlank() && ready,
                busy = busy,
                onClick = generate
            )
            if (status.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(status, color = colors.accentGreen, fontSize = 11.sp)
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AI Music (on device procedural composer)
// ---------------------------------------------------------------------------

@Composable
internal fun MusicSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PencilTheme.colors
    var mood by rememberSaveable { mutableStateOf(MusicGenerator.Mood.CHILL.name) }
    var durationSec by rememberSaveable { mutableStateOf(20f) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var waveform by remember { mutableStateOf(listOf<Float>()) }

    val compose: () -> Unit = {
        if (!busy) {
            busy = true
            error = null
            status = "Composing…"
            scope.launch {
                try {
                    val selected = MusicGenerator.Mood.entries
                        .firstOrNull { it.name == mood } ?: MusicGenerator.Mood.CHILL
                    val generated = withContext(Dispatchers.IO) {
                        MusicGenerator.render(selected, (durationSec * 1000).toLong())
                    }
                    waveform = generated.waveform
                    val saved = withContext(Dispatchers.IO) {
                        val dir = File(context.filesDir, "ai_music").apply { mkdirs() }
                        val target = File(dir, "music_${System.currentTimeMillis()}.wav")
                        SampleAudioGenerator.writeWav(target, generated.samples)
                        MediaStoreSaver.saveAudioFile(
                            context, target,
                            "catbyte_music_${System.currentTimeMillis()}", "audio/wav"
                        ) ?: target.absolutePath
                    }
                    status = when {
                        saved != null && saved.toString().startsWith("/") -> "Saved to ${saved.toString()}"
                        else -> "Saved to Music/CatByte (${durationSec.toInt()}s, ${selected.displayName})"
                    }
                } catch (failure: Throwable) {
                    status = ""
                    error = failure.message ?: "Music generation failed"
                } finally {
                    busy = false
                }
            }
        }
    }

    AiLabSheet(
        title = "AI Music",
        subtitle = "Royalty-free beds composed on device",
        accent = AiLabAccent.YELLOW,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("MOOD")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicGenerator.Mood.entries.forEach { option ->
                    AiLabChip(
                        label = option.displayName,
                        selected = mood == option.name,
                        onClick = { mood = option.name }
                    )
                }
            }
            AiLabSectionLabel("LENGTH")
            Text(
                "${durationSec.toInt()} seconds",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = durationSec,
                onValueChange = { durationSec = it },
                valueRange = 5f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentCyan,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            AiLabPrimaryButton(
                label = "Compose track",
                enabled = true,
                busy = busy,
                onClick = compose
            )
            if (waveform.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    waveform.take(48).forEach { value ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((2 + 40 * value.coerceIn(0f, 1f)).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.accentYellow.copy(alpha = 0.75f))
                        )
                    }
                }
            }
            if (status.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(status, color = colors.accentGreen, fontSize = 11.sp)
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Beat Finder (on device)
// ---------------------------------------------------------------------------

@Composable
internal fun BeatFinderSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = PencilTheme.colors
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var analysis by remember { mutableStateOf<AudioAnalysis.Analysis?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }

    val analyse: (Uri) -> Unit = { uri ->
        if (!busy) {
            busy = true
            error = null
            analysis = null
            scope.launch {
                try {
                    val decoded = PcmDecoder.decode(context, uri, maxDurationMs = 180_000L)
                    if (decoded == null) {
                        error = "Could not decode the audio in that file"
                    } else {
                        analysis = withContext(Dispatchers.Default) {
                            AudioAnalysis.analyse(decoded.samples, decoded.sampleRate)
                        }
                        fileName = displayName(context, uri)
                    }
                } catch (failure: Throwable) {
                    error = failure.message ?: "Analysis failed"
                } finally {
                    busy = false
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) analyse(uri)
    }

    AiLabSheet(
        title = "Beat Finder",
        subtitle = "Tempo, beat grid and silent gaps — on device",
        accent = AiLabAccent.CYAN,
        onDismiss = onDismiss
    ) {
        AiLabSheetBody {
            AiLabSectionLabel("SOURCE")
            AiLabSecondaryButton(
                label = "Choose an audio file",
                enabled = !busy,
                onClick = { picker.launch(arrayOf("audio/*", "video/*")) }
            )
            if (busy) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Decoding and analysing…", color = colors.textTertiary, fontSize = 11.sp)
            }
            analysis?.let { value ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .testTag("ai_lab_beats_result")
                ) {
                    Column {
                        StatRow("File", fileName)
                        StatRow("Length", formatSeconds(value.durationMs))
                        StatRow(
                            "Tempo",
                            value.bpm
                                ?.let { String.format(Locale.US, "%.1f BPM", it) }
                                ?: "No steady beat found"
                        )
                        StatRow("Beats", "${value.beatCount}")
                        StatRow("Peak", String.format(Locale.US, "%.0f%%", value.overallPeak * 100f))
                        StatRow(
                            "Average loudness",
                            String.format(Locale.US, "%.0f%%", value.overallRms * 100f)
                        )
                        StatRow("Silent gaps", "${value.silences.size}")
                        value.silences.take(5).forEach { silence ->
                            Text(
                                "· ${formatCueTime(silence.startMs)} → ${formatCueTime(silence.endMs)} " +
                                    "(${String.format(Locale.US, "%.1f", silence.durationMs / 1000.0)}s)",
                                color = colors.textTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                if (value.beatCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AiLabSecondaryButton(
                        label = "Copy beat timestamps",
                        onClick = {
                            copyToClipboard(
                                context,
                                value.beatsMs.joinToString("\n") { it.toString() }
                            )
                        }
                    )
                }
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                AiLabErrorText(it)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            color = colors.textTertiary,
            fontSize = 11.sp,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            color = colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
