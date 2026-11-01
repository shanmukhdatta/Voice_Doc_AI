package com.example.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DoctorBlueLight
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorBorderSubtle
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary
import com.example.ui.theme.DoctorWaveActive

@Composable
fun RecordingSheet(
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                spotColor = Color(0x20000000)
            )
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("recording_sheet_container")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Drag handle at top
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DoctorBorderSubtle)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated waveform bars
            AudioWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Status title: Listening...
            Text(
                text = stringResource(id = R.string.listening_status),
                color = DoctorTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Subtitle: "You can speak now." (NO duplicate "Release to send" text!)
            Text(
                text = stringResource(id = R.string.listening_subtitle),
                color = DoctorTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Utility buttons row: Cancel ('✕') and Edit (pencil)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DoctorBlueSoft)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = DoctorBluePrimary),
                            onClick = onCancel
                        )
                        .testTag("recording_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel recording",
                        tint = DoctorTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Edit (switch to keyboard typing) button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DoctorBlueSoft)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = DoctorBluePrimary),
                            onClick = onEdit
                        )
                        .testTag("recording_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Switch to text",
                        tint = DoctorTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AudioWaveform(modifier: Modifier = Modifier) {
    val barCount = 19
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    // Heights factor animations with varying phases
    val animProgress1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_1"
    )

    val animProgress2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_2"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Symmetric heights curve around center
        for (i in 0 until barCount) {
            val distFromCenter = Math.abs(i - barCount / 2).toFloat()
            val maxBaseHeight = 48f * (1f - (distFromCenter / (barCount / 2 + 1f)) * 0.55f)
            val factor = if (i % 2 == 0) animProgress1 else animProgress2
            val barHeight = (maxBaseHeight * (0.35f + 0.65f * factor)).coerceIn(8f, 54f)

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.5.dp)
                    .width(4.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DoctorWaveActive)
            )
        }
    }
}
