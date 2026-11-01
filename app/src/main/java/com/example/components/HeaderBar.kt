package com.example.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorBorderSubtle
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary

@Composable
fun HeaderBar(
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage,
    isSoundEnabled: Boolean,
    onMenuClick: () -> Unit,
    onLanguageToggle: (AppLanguage) -> Unit,
    onSoundToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left hamburger menu button with 48dp minimum touch target
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x1F000000))
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, DoctorBorderSubtle, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = DoctorBluePrimary),
                    onClick = onMenuClick
                )
                .testTag("menu_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Menu",
                tint = DoctorTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Voice Doctor AI Brand Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x18000000))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .border(1.dp, DoctorBorderSubtle, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp)
                .testTag("brand_pill")
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_medical_heart),
                contentDescription = "Heartbeat",
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Voice Doctor AI",
                color = DoctorTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Language toggle pill (EN / తెలుగు) with 44dp height
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x18000000))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .border(1.dp, DoctorBorderSubtle, RoundedCornerShape(22.dp))
                .padding(3.dp)
                .testTag("language_toggle_container")
        ) {
            LanguageChip(
                label = "EN",
                isSelected = currentLanguage == AppLanguage.ENGLISH,
                onClick = { onLanguageToggle(AppLanguage.ENGLISH) },
                testTag = "lang_en_chip"
            )
            LanguageChip(
                label = "తెలుగు",
                isSelected = currentLanguage == AppLanguage.TELUGU,
                onClick = { onLanguageToggle(AppLanguage.TELUGU) },
                testTag = "lang_te_chip"
            )
        }

        // Right speaker mute/unmute button with 48dp minimum touch target
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x1F000000))
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, DoctorBorderSubtle, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = DoctorBluePrimary),
                    onClick = onSoundToggle
                )
                .testTag("sound_toggle_button")
        ) {
            Icon(
                imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (isSoundEnabled) "Mute sound" else "Enable sound",
                tint = if (isSoundEnabled) DoctorBluePrimary else DoctorTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) DoctorBluePrimary else Color.Transparent,
        animationSpec = tween(200),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else DoctorTextSecondary,
        animationSpec = tween(200),
        label = "chip_text"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = DoctorBlueSoft),
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
