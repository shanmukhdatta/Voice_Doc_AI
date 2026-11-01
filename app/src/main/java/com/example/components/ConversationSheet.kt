package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.ui.theme.DoctorBorderSubtle

@Composable
fun ConversationSheet(
    messages: List<ChatMessage>,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color.White)
            .draggable(
                state = rememberDraggableState { delta ->
                    // Swiping down produces positive delta
                    if (delta > 20f) {
                        onCollapse()
                    }
                },
                orientation = Orientation.Vertical
            )
            .testTag("conversation_sheet_container")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Drag handle at top
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DoctorBorderSubtle)
                        .testTag("conversation_sheet_drag_handle")
                )
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                content = {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            showTimestamp = true,
                            useMiniDoctorAvatar = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Disclaimer card at the bottom of messages
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        DemoDisclaimerCard()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            )
        }
    }
}
