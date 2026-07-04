package com.whisper.android.ui.publicfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whisper.android.ui.components.BlueGlowIndicator
import com.whisper.android.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicFeedScreen(vm: PublicFeedViewModel = viewModel()) {
    val posts by vm.posts.collectAsState()
    val followedPubkeys by vm.followedPubkeys.collectAsState()
    val blueGlowActive by vm.blueGlowActive.collectAsState()
    val recentInteractions by vm.recentInteractions.collectAsState()
    var showGlowSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F7)),
    ) {
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Connecting to relay...",
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9F9F7)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isFollowing = followedPubkeys.contains(post.authorPubkey),
                        onFollowClick = { vm.onFollowClicked(post.authorPubkey) },
                        onUnfollowClick = { vm.onUnfollowClicked(post.authorPubkey) },
                        followedPubkeys = followedPubkeys,
                        onFollowReply = { vm.onFollowClicked(it) },
                        onUnfollowReply = { vm.onUnfollowClicked(it) },
                        getReplies = vm::getRepliesFlow,
                        onReplySubmit = { content, parentId, parentPubkey, rootId ->
                            vm.onReplySubmit(content, parentId, parentPubkey, rootId)
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            BlueGlowIndicator(
                active = blueGlowActive,
                onClick = {
                    showGlowSheet = true
                    vm.clearGlow()
                },
            )
        }
    }

    if (showGlowSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGlowSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFFF9F9F7),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Recent interactions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                )
                if (recentInteractions.isEmpty()) {
                    Text("Nothing yet.", color = Color(0xFF888888))
                } else {
                    recentInteractions.forEach { event ->
                        Column {
                            Text(
                                text = event.pubkey.take(8) + "...",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color.Black,
                            )
                            Text(
                                text = event.content.take(120),
                                fontSize = 13.sp,
                                color = Color(0xFF444444),
                            )
                        }
                    }
                }
            }
        }
    }
}
