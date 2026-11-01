package com.example.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DoctorBlueLight
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorBorderSubtle
import com.example.ui.theme.DoctorTextMuted
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary

@Composable
fun VoiceInputBar(
    isTextMode: Boolean,
    isRecording: Boolean,
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSubmitTextMessage: () -> Unit,
    onToggleTextMode: () -> Unit,
    onHoldToTalkStart: () -> Unit,
    onHoldToTalkRelease: () -> Unit,
    onHoldToTalkCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun performHaptic(durationMs: Long = 40) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    // Mic pulsation during recording
    val infiniteTransition = rememberInfiniteTransition(label = "bar_mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val containerBgColor by animateColorAsState(
        targetValue = if (isRecording) DoctorBlueSoft else Color.White,
        animationSpec = tween(200),
        label = "bar_bg"
    )
    val containerBorderColor by animateColorAsState(
        targetValue = if (isRecording) DoctorBlueLight else DoctorBorderSubtle,
        animationSpec = tween(200),
        label = "bar_border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("voice_input_bar_container")
    ) {
        // ONE large prominent pill-shaped container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (isRecording) 6.dp else 3.dp,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = Color(0x1F000000)
                )
                .clip(RoundedCornerShape(30.dp))
                .background(containerBgColor)
                .border(1.2.dp, containerBorderColor, RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isTextMode) {
                // VOICE MODE: Large hold-to-talk area + keyboard icon switch
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prominent Hold to talk touch target
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .pointerInput(isRecording) {
                                detectTapGestures(
                                    onPress = {
                                        performHaptic(40)
                                        onHoldToTalkStart()
                                        val released = tryAwaitRelease()
                                        if (released) {
                                            performHaptic(30)
                                            onHoldToTalkRelease()
                                        } else {
                                            onHoldToTalkCancel()
                                        }
                                    }
                                )
                            }
                            .testTag("hold_to_talk_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Microphone",
                                tint = DoctorBluePrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(micScale)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRecording) {
                                    stringResource(id = R.string.release_to_send)
                                } else {
                                    stringResource(id = R.string.hold_to_talk)
                                },
                                color = DoctorBluePrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Keyboard switch button with accessible 48dp touch size
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = DoctorBluePrimary),
                                onClick = onToggleTextMode
                            )
                            .testTag("switch_to_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = "Switch to keyboard typing",
                            tint = DoctorTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // TEXT MODE: Switch to voice + text input + send button inside the same pill
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch back to mic button with accessible 48dp touch target
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = DoctorBluePrimary),
                                onClick = onToggleTextMode
                            )
                            .testTag("switch_to_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Switch to voice mode",
                            tint = DoctorTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Text input field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.type_message_hint),
                                color = DoctorTextMuted,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputTextChanged,
                            textStyle = TextStyle(
                                color = DoctorTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(DoctorBluePrimary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { onSubmitTextMessage() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("message_text_input")
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Circular Send button with accessible 48dp touch target
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape, spotColor = DoctorBluePrimary.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(DoctorBluePrimary)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White),
                                onClick = onSubmitTextMessage
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
