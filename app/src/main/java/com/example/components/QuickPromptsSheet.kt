package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.DoctorBlueLight
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorBorderSubtle
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPromptsSheet(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSelectPrompt: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    val prompts = if (language == AppLanguage.TELUGU) {
        listOf(
            "ఉదయం నుండి నాకు తలనొప్పిగా ఉంది.",
            "నాకు కొద్దిగా జ్వరం మరియు నీరసంగా ఉంది.",
            "మింగేటప్పుడు గొంతు నొప్పిగా ఉంది.",
            "ఆహారం తిన్న తర్వాత కడుపులో అసౌకర్యంగా ఉంది.",
            "రోజుకు ఎన్ని గ్లాసుల నీరు త్రాగడం మంచిది?",
            "మంచి నిద్ర కోసం కొన్ని సాధారణ చిట్కాలు చెప్పండి."
        )
    } else {
        listOf(
            "I have a headache since morning.",
            "I feel a bit feverish and tired.",
            "My throat hurts when swallowing.",
            "I have stomach discomfort after lunch.",
            "How much water should I drink daily?",
            "What are some simple tips for better sleep?"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("quick_prompts_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DoctorBlueSoft)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = DoctorBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (language == AppLanguage.TELUGU) "సాధారణ ఆరోగ్య ప్రశ్నలు" else "Quick Health Questions",
                    color = DoctorTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DoctorBorderSubtle.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onDismiss
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = DoctorTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(prompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DoctorBlueSoft.copy(alpha = 0.6f))
                            .border(1.dp, DoctorBlueLight.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = DoctorBluePrimary),
                                onClick = { onSelectPrompt(prompt) }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = DoctorTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
