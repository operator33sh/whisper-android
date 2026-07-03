package com.whisper.android.data

data class PostUiModel(
    val id: String,
    val authorPubkey: String,
    val authorName: String? = null,
    val authorPictureUrl: String? = null,
    val content: String,
    val createdAt: Long,
    val replyCount: Int,
)
