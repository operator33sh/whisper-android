package com.whisper.android.ui.publicfeed

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whisper.android.data.NostrRepository
import com.whisper.android.data.PostUiModel
import com.whisper.android.nostr.NostrClient
import com.whisper.android.nostr.NostrEvent
import com.whisper.android.util.KeyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PublicFeedViewModel(app: Application) : AndroidViewModel(app) {

    private val securePrefs: SharedPreferences by lazy { buildSecurePrefs() }

    val repository: NostrRepository by lazy {
        val client = NostrClient()
        NostrRepository(client, securePrefs).also { repo ->
            val myPubkey = securePrefs.getString("hex_pubkey", null)
            repo.connect(myPubkey)
        }
    }

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
            val myPubkey = securePrefs.getString("hex_pubkey", null) ?: ""
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

    private fun getHexPrivKey(): String? {
        val nsec = securePrefs.getString("nsec", null) ?: return null
        return KeyUtils.nsecToHex(nsec)
    }

    private fun buildSecurePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(getApplication<Application>())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            getApplication<Application>(),
            "whisper_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun NostrEvent.toUiModel() = PostUiModel(
        id = id,
        authorPubkey = pubkey,
        content = content,
        createdAt = createdAt,
        replyCount = 0,
    )
}
