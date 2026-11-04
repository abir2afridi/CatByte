package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ultra-minimalist and theme-aware Splash Screen for CatByte.
 * Automatically adapts to light (warm cream) or dark (pitch dark) themes
 * with smooth typography animations.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val colors = PencilTheme.colors

    // App Name & Subtitle animations
    val nameAlpha = remember { Animatable(0f) }
    val nameOffsetY = remember { Animatable(24f) }
    val letterSpacing = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progressWidth = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            nameAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            nameOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            letterSpacing.animateTo(
                targetValue = 2f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }

        delay(150)

        launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = LinearEasing)
            )
        }
        launch {
            progressWidth.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        }

        delay(600)
        onSplashFinished()
    }

    // Theme-aware background (Light: warm cream white, Dark: pitch dark)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Ultra-clean Minimalist Logo (no heavy borders or neon glow)
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated App Name ("CatByte")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(nameAlpha.value)
                    .graphicsLayer {
                        translationY = nameOffsetY.value
                    }
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = colors.textPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = letterSpacing.value.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "PRO VIDEO EDITOR",
                    color = colors.accentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.alpha(subtitleAlpha.value)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Minimalist Progress Line
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.surfaceVariant)
                        .alpha(subtitleAlpha.value)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressWidth.value)
                            .height(2.dp)
                            .background(colors.accentCyan)
                    )
                }
            }
        }
    }
}
