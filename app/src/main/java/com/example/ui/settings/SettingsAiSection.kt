package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiConfig
import com.example.data.ai.AiEndpoints
import com.example.ui.theme.PencilTheme
import com.example.viewmodel.features.AiFeature
import kotlinx.coroutines.launch

/**
 * AI endpoint settings.
 *
 * The editor AI features are not tied to one vendor: the user pastes any
 * OpenAI-compatible base URL (OpenAI, a self-hosted LiteLLM/Ollama/LM Studio
 * gateway, an Azure-style proxy) plus an optional key, and the editor calls it
 * directly. "Test connection" performs a real completion so the user knows the
 * URL/key/model combination works before using it mid-edit.
 */
@Composable
internal fun SettingsAiSection(
    baseUrl: String,
    apiKey: String,
    model: String,
    imageModel: String,
    onSave: (baseUrl: String, apiKey: String, model: String, imageModel: String) -> Unit
) {
    val colors = PencilTheme.colors
    val scope = rememberCoroutineScope()

    var url by remember(baseUrl) { mutableStateOf(baseUrl) }
    var key by remember(apiKey) { mutableStateOf(apiKey) }
    var textModel by remember(model) { mutableStateOf(model) }
    var imagesModel by remember(imageModel) { mutableStateOf(imageModel) }
    var status by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    SettingsSection(
        title = "AI (OpenAI-compatible)",
        icon = Icons.Outlined.SmartToy,
        iconTint = colors.accentCyan
    ) {
        Text(
            text = "Add the base URL of an AI endpoint and the editor's AI tools " +
                "(remix, lyrics, translate, packaging, emoji, image) will use it. " +
                "Anything OpenAI-compatible works.",
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Base URL") },
            placeholder = { Text("https://api.openai.com") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_ai_base_url")
        )

        val urlLooksValid = url.isBlank() || AiEndpoints.isValidBaseUrl(url)
        if (!urlLooksValid) {
            Text(
                text = "The base URL must start with http:// or https://",
                color = Color(0xFFFF6B6B),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (url.isNotBlank()) {
            Text(
                text = "Calls go to ${AiEndpoints.chatCompletions(url)}",
                color = colors.textSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API key (optional)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_ai_api_key")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = textModel,
            onValueChange = { textModel = it },
            label = { Text("Text model") },
            placeholder = { Text("gpt-4o-mini") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_ai_model")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = imagesModel,
            onValueChange = { imagesModel = it },
            label = { Text("Image model") },
            placeholder = { Text("dall-e-3") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_ai_image_model")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onSave(url, key, textModel, imagesModel)
                    status = "Saved. The editor AI tools now use this endpoint."
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("settings_ai_save")
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (testing) return@Button
                    testing = true
                    status = "Testing…"
                    scope.launch {
                        val config = AiConfig(
                            baseUrl = url,
                            apiKey = key,
                            model = textModel,
                            imageModel = imagesModel
                        )
                        status = AiFeature.testConnection(config)
                        testing = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("settings_ai_test")
            ) {
                Text("Test connection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (status != null) {
            Text(
                text = status!!,
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .testTag("settings_ai_status")
            )
        }

        if (url.isBlank()) {
            Text(
                text = "No endpoint configured — AI chips in the editor will explain this instead of failing.",
                color = colors.textSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
