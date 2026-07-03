package com.whisper.android.ui.publicfeed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whisper.android.WhisperApplication
import com.whisper.android.data.PostUiModel
import com.whisper.android.nostr.NostrEvent
import com.whisper.android.util.KeyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PublicFeedViewModel(app: Application) : AndroidViewModel(app) {

    private val whisperApp = app as WhisperApplication
    private val repository = whisperApp.nostrRepository
    private val prefs = whisperApp.securePrefs

    val posts: StateFlow<List<PostUiModel>> = MutableStateFlow<List<PostUiModel>>(emptyList())
        .also { flow ->
            viewModelScope.launch {
                repository.publicFeedFlow()
                    .map { events -> events.map { it.toUiModel() } }
                    .collect { flow.value = it }
            }
        }

    val blueGlowActive: StateFlow<Boolean> = MutableStateFlow(false)
        .also { flow ->
            val myPubkey = prefs.getString("hex_pubkey", null) ?: ""
            viewModelScope.launch {
                repository.blueGlowFlow(myPubkey).collect { flow.value = it }
            }
        }

    val recentInteractions: StateFlow<List<NostrEvent>> = MutableStateFlow<List<NostrEvent>>(emptyList())
        .also { flow ->
            viewModelScope.launch {
                repository.recentInteractionsFlow().collect { flow.value = it }
            }
        }

    fun onFollowClicked(pubkey: String) {
        val hexPrivKey = getHexPrivKey() ?: return
        viewModelScope.launch { repository.followUser(pubkey, hexPrivKey) }
    }

    fun onUnfollowClicked(pubkey: String) {
        val hexPrivKey = getHexPrivKey() ?: return
        viewModelScope.launch { repository.unfollowUser(pubkey, hexPrivKey) }
    }

    fun isFollowing(pubkey: String): Boolean =
        repository.getFollowedPubkeys().contains(pubkey)

    fun clearGlow() = repository.clearGlow()

    private fun getHexPrivKey(): String? =
        KeyUtils.nsecToHex(prefs.getString("nsec", null) ?: return null)

    private fun NostrEvent.toUiModel() = PostUiModel(
        id = id,
        authorPubkey = pubkey,
        content = content,
        createdAt = createdAt,
        replyCount = 0,
    )
}
