package com.whisper.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisper.android.ui.theme.interFamily

private val buttonShape = RoundedCornerShape(4.dp)

@Composable
fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    if (isFollowing) {
        val borderColor = Color(0xFF2D2D2D)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(buttonShape)
                .border(BorderStroke(1.dp, borderColor), buttonShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 1.dp),
        ) {
            Text(
                "Unfollow",
                fontFamily = interFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D2D2D),
            )
        }
    } else {
        val bgColor = Color(0xFF000000)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(buttonShape)
                .drawBehind { drawRect(bgColor) }
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 1.dp),
        ) {
            Text(
                "Follow",
                fontFamily = interFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFFFFF),
            )
        }
    }
}
