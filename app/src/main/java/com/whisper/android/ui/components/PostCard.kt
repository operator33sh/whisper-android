package com.whisper.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.whisper.android.data.PostUiModel
import com.whisper.android.ui.theme.crimsonProFamily
import com.whisper.android.ui.theme.interFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

private val imageUrlRegex = Regex(
    """https?://\S+\.(?:jpg|jpeg|png|gif|webp)(\?\S*)?""",
    RegexOption.IGNORE_CASE,
)

private fun extractImageUrls(content: String): List<String> =
    imageUrlRegex.findAll(content).map { it.value }.toList()

private fun stripImageUrls(content: String): String =
    imageUrlRegex.replace(content, "").trim()

@Composable
fun PostCard(
    post: PostUiModel,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    followedPubkeys: Set<String>,
    onFollowReply: (String) -> Unit,
    onUnfollowReply: (String) -> Unit,
    getReplies: (String) -> Flow<List<PostUiModel>>,
    onReplySubmit: (content: String, parentEventId: String, parentPubkey: String, rootEventId: String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    var repliesExpanded by remember { mutableStateOf(false) }
    var showReplyModal by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { 400.dp.roundToPx() }

    val imageUrls = remember(post.content) { extractImageUrls(post.content) }
    val displayContent = remember(post.content) { stripImageUrls(post.content) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                AuthorAvatar(
                    pictureUrl = post.authorPictureUrl,
                    displayName = post.authorName ?: post.authorPubkey,
                )
                Text(
                    text = post.authorName ?: (post.authorPubkey.take(8) + "..." + post.authorPubkey.takeLast(4)),
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color(0xFF2D2D2D).copy(alpha = 0.6f),
                )
            }
            FollowButton(
                isFollowing = isFollowing,
                onClick = if (isFollowing) onUnfollowClick else onFollowClick,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (displayContent.isNotBlank()) {
            SubcomposeLayout(modifier = Modifier.fillMaxWidth().clipToBounds()) { constraints ->
                val placeables = subcompose("content") {
                    Text(
                        text = displayContent,
                        fontFamily = crimsonProFamily,
                        fontSize = 16.sp,
                        color = Color(0xFF2D2D2D),
                        lineHeight = 24.sp,
                    )
                }.map { it.measure(constraints.copy(maxHeight = Int.MAX_VALUE)) }

                val naturalHeight = placeables.maxOfOrNull { it.height } ?: 0
                overflows = naturalHeight > maxHeightPx
                val layoutHeight = if (expanded) naturalHeight else minOf(naturalHeight, maxHeightPx)

                layout(constraints.maxWidth, layoutHeight) {
                    placeables.forEach { it.placeRelative(0, 0) }
                }
            }
        }

        if (imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                imageUrls.forEach { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { fullscreenImageUrl = url },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = formatTimestamp(post.createdAt),
                    fontFamily = interFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                )
                if (post.replyCount > 0) {
                    Text(
                        text = if (repliesExpanded) "\u2212 ${post.replyCount} ${if (post.replyCount == 1) "reply" else "replies"}"
                               else "+ ${post.replyCount} ${if (post.replyCount == 1) "reply" else "replies"}",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                        modifier = Modifier.clickable { repliesExpanded = !repliesExpanded },
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (overflows) {
                    Text(
                        text = if (expanded) "Show less" else "Read more",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                        modifier = Modifier.clickable { expanded = !expanded },
                    )
                }
                Text(
                    text = "Reply",
                    fontFamily = interFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showReplyModal = true },
                )
            }
        }

        if (repliesExpanded) {
            Spacer(Modifier.height(8.dp))
            ReplyTree(
                eventId = post.id,
                getReplies = getReplies,
                followedPubkeys = followedPubkeys,
                onFollowClick = onFollowReply,
                onUnfollowClick = onUnfollowReply,
                onReplySubmit = onReplySubmit,
            )
        }
    }

    if (fullscreenImageUrl != null) {
        Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullscreenImageUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullscreenImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .clickable { fullscreenImageUrl = null },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                }
            }
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
                    color = Color(0xFF2D2D2D),
                )
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF2D2D2D).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = interFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF2D2D2D),
                    ),
                    decorationBox = { inner ->
                        if (replyText.isEmpty()) {
                            Text(
                                "Write your reply\u2026",
                                fontFamily = interFamily,
                                fontSize = 14.sp,
                                color = Color(0xFF2D2D2D).copy(alpha = 0.4f),
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
                        color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { showReplyModal = false; replyText = "" }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (replyText.isNotBlank()) Color(0xFF2D2D2D) else Color(0xFF2D2D2D).copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(enabled = replyText.isNotBlank()) {
                                onReplySubmit(replyText, post.id, post.authorPubkey, null)
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

@Composable
private fun AuthorAvatar(pictureUrl: String?, displayName: String) {
    if (pictureUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(pictureUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDDDDD)),
        ) {
            Text(
                text = displayName.take(1).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
            )
        }
    }
}

private fun formatTimestamp(epochSeconds: Long): String {
    val date = Date(epochSeconds * 1000)
    val datePart = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    val diffMs = System.currentTimeMillis() - epochSeconds * 1000
    val diffMinutes = diffMs / 60_000
    val diffHours = diffMs / 3_600_000
    val diffDays = diffMs / 86_400_000
    val relative = when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "$diffMinutes minutes ago"
        diffHours < 24 -> "$diffHours hours ago"
        diffDays < 7 -> "$diffDays days ago"
        else -> ""
    }
    return if (relative.isNotEmpty()) "$datePart · $relative" else datePart
}
