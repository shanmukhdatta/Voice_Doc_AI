package com.example.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.theme.DoctorBlueLight
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary

@Composable
fun AboutDoctorDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onClearChat: () -> Unit,
    onRestoreSample: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DoctorBluePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.TELUGU) "సరే" else "Got it",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onClearChat()
                    onDismiss()
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DoctorBluePrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.TELUGU) "క్రొత్త సంభాషణ" else "New Chat",
                    color = DoctorBluePrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_medical_heart),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.TELUGU) "వాయిస్ డాక్టర్ AI గురించి" else "About Voice Doctor AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DoctorTextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (language == AppLanguage.TELUGU)
                        "వాయిస్ డాక్టర్ AI అనేది స్వరం-మొదటగా పనిచేసే స్థానిక ఆండ్రాయిడ్ వైద్య సహాయ ప్రోటోటైప్."
                    else
                        "Voice Doctor AI is a minimal voice-first native Android medical assistant prototype designed with Jetpack Compose.",
                    fontSize = 14.sp,
                    color = DoctorTextPrimary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem(
                    text = if (language == AppLanguage.TELUGU) "ఆఫ్‌లైన్ మోడ్ లో పనిచేస్తుంది" else "Runs completely offline (Airplane mode ready)"
                )
                FeatureItem(
                    text = if (language == AppLanguage.TELUGU) "ఇంగ్లీష్ మరియు తెలుగు భాషల మద్దతు" else "Bilingual: English & Telugu UI toggling"
                )
                FeatureItem(
                    text = if (language == AppLanguage.TELUGU) "వాయిస్ హోల్డ్-టు-టాక్ మరియు టైపింగ్ చాట్" else "Hold-to-talk voice recording simulation & text chat"
                )
                FeatureItem(
                    text = if (language == AppLanguage.TELUGU) "విస్తరించదగిన సంభాషణ చరిత్ర" else "Expandable conversation sheet with full message history"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DoctorBlueSoft)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.TELUGU)
                            "గమనిక: ఈ ప్రోటోటైప్ సాధారణ ఆరోగ్య అవగాహన కోసం మాత్రమే, ఇది వైద్య నిర్ధారణ లేదా చికిత్సను భర్తీ చేయదు."
                        else
                            "Notice: This prototype provides general health information only and is not a medical diagnosis or treatment.",
                        fontSize = 11.sp,
                        color = DoctorTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = DoctorBluePrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = DoctorTextSecondary
        )
    }
}
