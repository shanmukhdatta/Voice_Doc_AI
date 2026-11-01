package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DoctorUiState
import com.example.model.isListening
import com.example.model.isSpeaking
import com.example.model.isProcessing
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import com.example.ui.theme.DoctorAvatarCircleBg
import com.example.ui.theme.DoctorBlueLight
import com.example.ui.theme.DoctorBluePrimary

@Composable
fun DoctorAvatar(
    modifier: Modifier = Modifier,
    uiState: DoctorUiState = DoctorUiState.Idle,
    isSoundEnabled: Boolean = true,
    targetSize: Dp = 175.dp,
    onAvatarClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Animated size between normal and compact states
    val animatedSize by animateDpAsState(
        targetValue = targetSize,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "avatar_size_animation"
    )

    // Pulsing halo animation when listening or responding
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (uiState.isListening || uiState.isSpeaking || uiState.isProcessing) 1.07f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 600))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(animatedSize + 20.dp)
                .testTag("doctor_avatar_container")
        ) {
            // Subtle animated halo backdrop
            if (uiState.isListening || uiState.isSpeaking || uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .size(animatedSize + 16.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    DoctorBluePrimary.copy(alpha = 0.22f),
                                    DoctorBlueLight.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Main avatar circular card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(animatedSize)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = if (targetSize > 120.dp) 10.dp else 4.dp,
                        shape = CircleShape,
                        spotColor = DoctorBluePrimary.copy(alpha = 0.22f),
                        ambientColor = DoctorBlueLight.copy(alpha = 0.30f)
                    )
                    .clip(CircleShape)
                    .background(DoctorAvatarCircleBg)
                    .border(
                        width = if (targetSize > 120.dp) 3.dp else 2.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White, DoctorBlueLight.copy(alpha = 0.7f))
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = DoctorBluePrimary),
                        onClick = onAvatarClick
                    )
                    .testTag("doctor_avatar_image")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.doctor_avatar),
                    contentDescription = "Voice Doctor Avatar",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Small sound toggle indicator badge attached to the avatar
            if (targetSize > 120.dp) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, DoctorBlueLight, CircleShape)
                        .testTag("doctor_avatar_sound_badge")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (isSoundEnabled) "Sound on" else "Sound muted",
                        tint = if (isSoundEnabled) DoctorBluePrimary else Color.Gray,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
