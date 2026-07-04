package com.whisper.android.ui.publicfeed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whisper.android.WhisperApplication
import com.whisper.android.data.PostUiModel
import com.whisper.android.data.UserProfile
import com.whisper.android.nostr.NostrEvent
import com.whisper.android.util.KeyUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PublicFeedViewModel(app: Application) : AndroidViewModel(app) {

    private val whisperApp = app as WhisperApplication
    private val repository = whisperApp.nostrRepository
    private val prefs = whisperApp.securePrefs

    init {
        repository.connect(prefs.getString("hex_pubkey", null))
    }

    val posts: StateFlow<List<PostUiModel>> = MutableStateFlow<List<PostUiModel>>(emptyList())
        .also { flow ->
            viewModelScope.launch {
                combine(repository.publicFeedFlow(), repository.replyCountsFlow(), repository.profilesFlow()) { events, counts, profiles ->
                    events.map { it.toUiModel(counts, profiles) }
                }.collect { flow.value = it }
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

    val followedPubkeys: StateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet())
        .also { flow ->
            viewModelScope.launch {
                repository.followedPubkeysFlow().collect { flow.value = it }
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

    fun getRepliesFlow(eventId: String): Flow<List<PostUiModel>> =
        combine(repository.repliesFlow(eventId), repository.replyCountsFlow(), repository.profilesFlow()) { events, counts, profiles ->
            events.map { it.toUiModel(counts, profiles) }
        }

    fun clearGlow() = repository.clearGlow()

    private fun getHexPrivKey(): String? =
        KeyUtils.nsecToHex(prefs.getString("nsec", null) ?: return null)

    private fun NostrEvent.toUiModel(counts: Map<String, Int> = emptyMap(), profiles: Map<String, UserProfile> = emptyMap()) = PostUiModel(
        id = id,
        authorPubkey = pubkey,
        authorName = profiles[pubkey]?.name,
        authorPictureUrl = profiles[pubkey]?.pictureUrl,
        content = content,
        createdAt = createdAt,
        replyCount = counts.getOrDefault(id, 0),
    )
}
