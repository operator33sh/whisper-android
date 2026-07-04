package com.whisper.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.whisper.android.data.PostUiModel
import com.whisper.android.ui.theme.crimsonProFamily
import com.whisper.android.ui.theme.interFamily
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val borderColor = Color(0xFF2D2D2D).copy(alpha = 0.2f)
private val textColor = Color(0xFF2D2D2D)

private fun formatTimestamp(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}

@Composable
fun ReplyTree(
    eventId: String,
    getReplies: (String) -> Flow<List<PostUiModel>>,
    followedPubkeys: Set<String>,
    onFollowClick: (String) -> Unit,
    onUnfollowClick: (String) -> Unit,
    depth: Int = 0,
    rootEventId: String = eventId,
    onReplySubmit: (content: String, parentEventId: String, parentPubkey: String, rootEventId: String?) -> Unit,
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
                followedPubkeys = followedPubkeys,
                onFollowClick = onFollowClick,
                onUnfollowClick = onUnfollowClick,
                depth = depth,
                rootEventId = rootEventId,
                onReplySubmit = onReplySubmit,
            )
        }
    }
}

@Composable
private fun ReplyItem(
    reply: PostUiModel,
    getReplies: (String) -> Flow<List<PostUiModel>>,
    followedPubkeys: Set<String>,
    onFollowClick: (String) -> Unit,
    onUnfollowClick: (String) -> Unit,
    depth: Int,
    rootEventId: String,
    onReplySubmit: (content: String, parentEventId: String, parentPubkey: String, rootEventId: String?) -> Unit,
) {
    var subExpanded by remember { mutableStateOf(false) }
    var showReplyModal by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(start = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
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
            FollowButton(
                isFollowing = followedPubkeys.contains(reply.authorPubkey),
                onClick = if (followedPubkeys.contains(reply.authorPubkey))
                    { { onUnfollowClick(reply.authorPubkey) } }
                else
                    { { onFollowClick(reply.authorPubkey) } },
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

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTimestamp(reply.createdAt),
                fontFamily = interFamily,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (reply.replyCount > 0 && depth < 3) {
                    Text(
                        text = if (subExpanded) "\u2212 ${reply.replyCount} ${if (reply.replyCount == 1) "reply" else "replies"}"
                               else "+ ${reply.replyCount} ${if (reply.replyCount == 1) "reply" else "replies"}",
                        fontFamily = interFamily,
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { subExpanded = !subExpanded },
                    )
                }
                Text(
                    text = "Reply",
                    fontFamily = interFamily,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showReplyModal = true },
                )
            }
        }

        if (subExpanded) {
            Spacer(Modifier.height(8.dp))
            ReplyTree(
                eventId = reply.id,
                getReplies = getReplies,
                followedPubkeys = followedPubkeys,
                onFollowClick = onFollowClick,
                onUnfollowClick = onUnfollowClick,
                depth = depth + 1,
                rootEventId = rootEventId,
                onReplySubmit = onReplySubmit,
            )
        }
    }

    if (showReplyModal) {
        Dialog(
            onDismissRequest = { showReplyModal = false; replyText = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color(0xFFF9F9F7), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Reply",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textColor,
                )
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = interFamily,
                        fontSize = 14.sp,
                        color = textColor,
                    ),
                    decorationBox = { inner ->
                        if (replyText.isEmpty()) {
                            Text(
                                "Write your reply\u2026",
                                fontFamily = interFamily,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.4f),
                            )
                        }
                        inner()
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = interFamily,
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { showReplyModal = false; replyText = "" }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (replyText.isNotBlank()) textColor else textColor.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(enabled = replyText.isNotBlank()) {
                                onReplySubmit(replyText, reply.id, reply.authorPubkey, rootEventId)
                                showReplyModal = false
                                replyText = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Whisper",
                            fontFamily = interFamily,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
