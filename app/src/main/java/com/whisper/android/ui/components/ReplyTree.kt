package com.whisper.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.whisper.android.data.PostUiModel
import com.whisper.android.ui.theme.crimsonProFamily
import com.whisper.android.ui.theme.interFamily
import kotlinx.coroutines.flow.Flow

private val borderColor = Color(0xFF2D2D2D).copy(alpha = 0.2f)
private val textColor = Color(0xFF2D2D2D)

@Composable
fun ReplyTree(
    eventId: String,
    getReplies: (String) -> Flow<List<PostUiModel>>,
    depth: Int = 0,
) {
    if (depth > 3) return

    val repliesFlow = remember(eventId) { getReplies(eventId) }
    val replies by repliesFlow.collectAsState(initial = emptyList())

    if (replies.isEmpty()) return

    Column(
        modifier = Modifier
            .padding(start = 16.dp)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        replies.forEach { reply ->
            ReplyItem(
                reply = reply,
                getReplies = getReplies,
                depth = depth,
            )
        }
    }
}

@Composable
private fun ReplyItem(
    reply: PostUiModel,
    getReplies: (String) -> Flow<List<PostUiModel>>,
    depth: Int,
) {
    var subExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (reply.authorPictureUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(reply.authorPictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDDDDDD)),
                ) {
                    Text(
                        text = (reply.authorName ?: reply.authorPubkey).take(1).uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                    )
                }
            }
            Text(
                text = reply.authorName ?: (reply.authorPubkey.take(8) + "\u2026"),
                fontFamily = interFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = reply.content,
            fontFamily = crimsonProFamily,
            fontSize = 15.sp,
            color = textColor,
            lineHeight = 22.sp,
        )

        if (reply.replyCount > 0 && depth < 3) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (subExpanded) "\u2212 ${reply.replyCount} ${if (reply.replyCount == 1) "reply" else "replies"}"
                       else "+ ${reply.replyCount} ${if (reply.replyCount == 1) "reply" else "replies"}",
                fontFamily = interFamily,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier.clickable { subExpanded = !subExpanded },
            )
        }

        if (subExpanded) {
            Spacer(Modifier.height(8.dp))
            ReplyTree(
                eventId = reply.id,
                getReplies = getReplies,
                depth = depth + 1,
            )
        }
    }
}
