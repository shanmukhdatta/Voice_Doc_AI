package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.components.AboutDoctorDialog
import com.example.components.ConversationHistorySheet
import com.example.components.ConversationPreview
import com.example.components.ConversationSheet
import com.example.components.DoctorAvatar
import com.example.components.HeaderBar
import com.example.components.ModelDownloadDialog
import com.example.components.RecordingSheet
import com.example.components.VoiceInputBar
import com.example.model.AppLanguage
import com.example.model.DoctorUiState
import com.example.model.isListening
import com.example.model.isProcessing
import com.example.modelmanager.ModelInstallState
import com.example.ui.theme.DoctorBackground
import com.example.ui.theme.DoctorBluePrimary
import com.example.ui.theme.DoctorBlueSoft
import com.example.ui.theme.DoctorTextPrimary
import com.example.ui.theme.DoctorTextSecondary
import com.example.viewmodel.VoiceDoctorViewModel

@Composable
fun VoiceDoctorScreen(
    viewModel: VoiceDoctorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRecording = state.uiState.isListening
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    // Handle Android system back button
    BackHandler(enabled = state.isConversationExpanded || isRecording || state.isTextMode || state.showHistorySheet || state.showModelDownloadDialog) {
        when {
            state.showModelDownloadDialog -> viewModel.setModelDownloadDialog(false)
            state.showHistorySheet -> viewModel.setHistorySheet(false)
            isRecording -> viewModel.cancelRecording()
            state.isConversationExpanded -> viewModel.setConversationExpanded(false)
            state.isTextMode -> viewModel.toggleTextMode()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DoctorBackground),
        containerColor = DoctorBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                HeaderBar(
                    currentLanguage = state.currentLanguage,
                    isSoundEnabled = state.isSoundEnabled,
                    onMenuClick = { viewModel.setInfoDialog(true) },
                    onLanguageToggle = { viewModel.setLanguage(it) },
                    onSoundToggle = { viewModel.toggleSound() }
                )

                // Optional model status banner if not installed yet
                if (state.currentLanguage == AppLanguage.ENGLISH && state.modelInstallState !is ModelInstallState.Installed) {
                    ModelStatusBanner(
                        state = state.modelInstallState,
                        onClick = { viewModel.setModelDownloadDialog(true) }
                    )
                }

                // Main Center Stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isRecording) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                DoctorAvatar(
                                    uiState = state.uiState,
                                    isSoundEnabled = state.isSoundEnabled,
                                    targetSize = 175.dp,
                                    onAvatarClick = { viewModel.toggleSound() }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                GreetingSection(
                                    uiState = state.uiState,
                                    language = state.currentLanguage,
                                    isCompact = false
                                )
                            }

                            RecordingSheet(
                                onCancel = { viewModel.cancelRecording() },
                                onEdit = {
                                    viewModel.cancelRecording()
                                    if (!state.isTextMode) viewModel.toggleTextMode()
                                }
                            )
                        }
                    } else if (state.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                DoctorAvatar(
                                    uiState = state.uiState,
                                    isSoundEnabled = state.isSoundEnabled,
                                    targetSize = 175.dp,
                                    onAvatarClick = { viewModel.toggleSound() }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                GreetingSection(
                                    uiState = state.uiState,
                                    language = state.currentLanguage,
                                    isCompact = false
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(if (state.isConversationExpanded) 4.dp else 10.dp))

                            DoctorAvatar(
                                uiState = state.uiState,
                                isSoundEnabled = state.isSoundEnabled,
                                targetSize = if (state.isConversationExpanded) 95.dp else 165.dp,
                                onAvatarClick = { viewModel.toggleSound() }
                            )

                            Spacer(modifier = Modifier.height(if (state.isConversationExpanded) 4.dp else 10.dp))

                            GreetingSection(
                                uiState = state.uiState,
                                language = state.currentLanguage,
                                isCompact = state.isConversationExpanded
                            )

                            Spacer(modifier = Modifier.height(if (state.isConversationExpanded) 8.dp else 12.dp))

                            if (!state.isConversationExpanded) {
                                ConversationPreview(
                                    messages = state.messages,
                                    onExpand = { viewModel.setConversationExpanded(true) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                ConversationSheet(
                                    messages = state.messages,
                                    onCollapse = { viewModel.setConversationExpanded(false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Processing / Responding subtle badge
                    if (state.uiState.isProcessing) {
                        ProcessingIndicator(
                            language = state.currentLanguage,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }
                }

                // Voice Input Bar
                VoiceInputBar(
                    isTextMode = state.isTextMode,
                    isRecording = isRecording,
                    inputText = state.inputText,
                    onInputTextChanged = { viewModel.onInputTextChanged(it) },
                    onSubmitTextMessage = { viewModel.submitTextMessage() },
                    onToggleTextMode = { viewModel.toggleTextMode() },
                    onHoldToTalkStart = {
                        if (state.currentLanguage == AppLanguage.ENGLISH && !viewModel.hasRecordAudioPermission()) {
                            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    onHoldToTalkRelease = { viewModel.releaseRecording() },
                    onHoldToTalkCancel = { viewModel.cancelRecording() }
                )
            }

            // Floating feedback toast
            state.statusMessage?.let { status ->
                StatusFeedbackBadge(
                    message = status,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                )
            }

            // Info Dialog with Evidence Mode Toggle
            if (state.showInfoDialog) {
                AboutDoctorDialog(
                    language = state.currentLanguage,
                    isEvidenceModeEnabled = state.isEvidenceModeEnabled,
                    onToggleEvidenceMode = { viewModel.toggleEvidenceMode() },
                    onDismiss = { viewModel.setInfoDialog(false) },
                    onClearChat = { viewModel.clearConversation() },
                    onRestoreSample = { viewModel.loadSampleConversation() },
                    onOpenModelManager = { viewModel.setModelDownloadDialog(true) },
                    onOpenHistory = { viewModel.setHistorySheet(true) }
                )
            }

            // Model Download Dialog
            if (state.showModelDownloadDialog) {
                ModelDownloadDialog(
                    language = state.currentLanguage,
                    state = state.modelInstallState,
                    onDismiss = { viewModel.setModelDownloadDialog(false) },
                    onStartDownload = { viewModel.startModelDownload() },
                    onCancelDownload = { viewModel.cancelModelDownload() },
                    onDeleteModel = { viewModel.deleteInstalledModel() }
                )
            }

            // Conversation History Sheet
            if (state.showHistorySheet) {
                ConversationHistorySheet(
                    language = state.currentLanguage,
                    isHistoryEnabled = state.isHistoryEnabled,
                    conversations = state.savedConversations,
                    onDismiss = { viewModel.setHistorySheet(false) },
                    onToggleHistory = { viewModel.toggleHistoryOptIn(it) },
                    onSelectConversation = { viewModel.loadSavedConversation(it) },
                    onDeleteConversation = { viewModel.deleteSavedConversation(it) },
                    onClearAll = { viewModel.clearAllSavedConversations() }
                )
            }
        }
    }
}

@Composable
private fun ModelStatusBanner(
    state: ModelInstallState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DoctorBlueSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            tint = DoctorBluePrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        val text = when (state) {
            is ModelInstallState.Downloading -> "Downloading AI Model (${(state.progress * 100).toInt()}%)..."
            is ModelInstallState.Verifying -> "Verifying AI Model..."
            is ModelInstallState.Error -> "Model download error. Tap to retry."
            else -> "Download AI Model (~484 MB) for offline conversation"
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DoctorBluePrimary
        )
    }
}

@Composable
private fun GreetingSection(
    uiState: DoctorUiState,
    language: AppLanguage,
    isCompact: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        when {
            uiState.isListening -> {
                Text(
                    text = stringResource(id = R.string.listening_title),
                    color = DoctorTextPrimary,
                    fontSize = if (isCompact) 17.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.listening_subtitle),
                    color = DoctorTextSecondary,
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Text(
                    text = stringResource(id = R.string.greeting_title),
                    color = DoctorTextPrimary,
                    fontSize = if (isCompact) 17.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProcessingIndicator(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1F000000))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            color = DoctorBluePrimary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.processing_status),
            color = DoctorTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusFeedbackBadge(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(20.dp))
            .background(DoctorBluePrimary)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("status_feedback_badge")
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
