package com.whisper.android.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.whisper.android.data.PostUiModel
import com.whisper.android.ui.theme.crimsonProFamily
import com.whisper.android.ui.theme.interFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
    post: PostUiModel,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { 400.dp.roundToPx() }

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

        SubcomposeLayout(modifier = Modifier.fillMaxWidth().clipToBounds()) { constraints ->
            val placeables = subcompose("content") {
                Text(
                    text = post.content,
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
                        text = if (post.replyCount == 1) "+ 1 reply" else "+ ${post.replyCount} replies",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                    )
                }
            }
            if (overflows) {
                Text(
                    text = if (expanded) "Show less" else "Read more",
                    fontFamily = interFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                    modifier = Modifier.clickable { expanded = !expanded },
                )
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
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}
