package com.whisper.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisper.android.data.PostUiModel
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = Color(0xFFDDDDDD), shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.authorPubkey.take(8) + "..." + post.authorPubkey.takeLast(4),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.Black,
                )
                Text(
                    text = formatTimestamp(post.createdAt),
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                )
            }
            FollowButton(
                isFollowing = isFollowing,
                onClick = if (isFollowing) onUnfollowClick else onFollowClick,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = post.content,
            fontSize = 15.sp,
            color = Color.Black,
            lineHeight = 22.sp,
        )
    }
}

private fun formatTimestamp(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}
