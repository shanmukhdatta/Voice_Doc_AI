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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.modelmanager.ModelInstallState
import com.example.modelmanager.ModelRepository
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary

@Composable
fun ModelDownloadDialog(
    language: AppLanguage,
    state: ModelInstallState,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            when (state) {
                is ModelInstallState.NotInstalled -> {
                    Button(
                        onClick = onStartDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = DoctorBluePrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("download_model_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.TELUGU) "డౌన్‌లోడ్ చేయండి (~484 MB)" else "Download (~484 MB)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is ModelInstallState.Downloading -> {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("cancel_download_button")
                    ) {
                        Text(
                            text = if (language == AppLanguage.TELUGU) "రద్దు చేయి" else "Cancel",
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
                is ModelInstallState.Verifying -> {
                    // Nothing needed while verifying
                }
                is ModelInstallState.Installed -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DoctorBluePrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("close_model_dialog_button")
                    ) {
                        Text(text = if (language == AppLanguage.TELUGU) "పూర్తయింది" else "Done")
                    }
                }
                is ModelInstallState.Error -> {
                    Button(
                        onClick = onStartDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = DoctorBluePrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("retry_download_button")
                    ) {
                        Text(text = if (language == AppLanguage.TELUGU) "మళ్లీ ప్రయత్నించండి" else "Retry Download")
                    }
                }
            }
        },
        dismissButton = {
            if (state is ModelInstallState.Installed) {
                OutlinedButton(
                    onClick = onDeleteModel,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("delete_model_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (language == AppLanguage.TELUGU) "మోడల్‌ను తొలగించు" else "Delete Model")
                }
            } else if (state !is ModelInstallState.Downloading) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = if (language == AppLanguage.TELUGU) "మూసివేయి" else "Close")
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = DoctorBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.TELUGU) "ఆఫ్‌లైన్ AI మోడల్" else "Offline AI Model",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DoctorTextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Qwen3-0.6B (Q4_K_M GGUF)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DoctorTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (language == AppLanguage.TELUGU)
                        "ఈ మోడల్ మీ ఫోన్‌లో పూర్తిగా ఆఫ్‌లైన్‌లో రన్ అవుతుంది. ఏ ప్రశ్నలు లేదా వాయిస్ రికార్డింగ్‌లు క్లౌడ్‌కు వెళ్లవు."
                    else
                        "Enables genuine on-device English conversation with zero cloud inference. All text and speech remain on this phone.",
                    fontSize = 13.sp,
                    color = DoctorTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    is ModelInstallState.NotInstalled -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DoctorBlueSoft)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.TELUGU) "పరిమాణం: 484 MB (ఒక్కసారి డౌన్‌లోడ్)" else "Size: ~484 MB (One-time download)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DoctorBluePrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (language == AppLanguage.TELUGU) "వై-ఫై కనెక్షన్ సిఫార్సు చేయబడింది." else "Wi-Fi connection recommended.",
                                    fontSize = 11.sp,
                                    color = DoctorTextSecondary
                                )
                            }
                        }
                    }
                    is ModelInstallState.Downloading -> {
                        val mbDownloaded = state.bytesDownloaded / 1_000_000
                        val mbTotal = state.totalBytes / 1_000_000
                        val percent = (state.progress * 100).toInt()

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (language == AppLanguage.TELUGU) "డౌన్‌లోడ్ అవుతోంది..." else "Downloading model...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DoctorTextPrimary
                                )
                                Text(
                                    text = "$percent%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DoctorBluePrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = DoctorBluePrimary,
                                trackColor = DoctorBlueSoft
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$mbDownloaded MB / $mbTotal MB",
                                fontSize = 11.sp,
                                color = DoctorTextSecondary
                            )
                        }
                    }
                    is ModelInstallState.Verifying -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = DoctorBluePrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == AppLanguage.TELUGU) "ధృవీకరిస్తోంది (SHA-256)..." else "Verifying SHA-256 checksum...",
                                fontSize = 13.sp,
                                color = DoctorTextPrimary
                            )
                        }
                    }
                    is ModelInstallState.Installed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.TELUGU) "ఇన్‌స్టాల్ చేయబడింది మరియు సిద్ధంగా ఉంది" else "Installed & Ready Offline",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = if (language == AppLanguage.TELUGU) "పరిమాణం: 484.2 MB (యాప్ స్టోరేజ్)" else "Size: 484.2 MB in app private storage",
                                    fontSize = 11.sp,
                                    color = Color(0xFF388E3C)
                                )
                            }
                        }
                    }
                    is ModelInstallState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

