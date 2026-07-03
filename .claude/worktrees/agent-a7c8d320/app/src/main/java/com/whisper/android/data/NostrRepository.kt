package com.whisper.android.data

import android.content.SharedPreferences
import com.whisper.android.nostr.EventSigner
import com.whisper.android.nostr.NostrClient
import com.whisper.android.nostr.NostrEvent
import com.whisper.android.nostr.NostrFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NostrRepository(
    private val client: NostrClient,
    private val prefs: SharedPreferences,
) {

    companion object {
        const val DEFAULT_RELAY = "wss://relay.damus.io"
        private const val KEY_FOLLOWS = "follows"
        private const val SUB_PUBLIC = "whisper-public"
        private const val SUB_PRIVATE = "whisper-private"
        private const val SUB_GLOW = "whisper-glow"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // All Kind 1 events received from the relay
    private val _allEvents = MutableStateFlow<Map<String, NostrEvent>>(emptyMap())

    // Set of event IDs that have been referenced as a reply (via "e" tag)
    private val _referencedIds = MutableStateFlow<Set<String>>(emptySet())

    // Events that qualify for blue glow (targeting our pubkey)
    private val _glowEvents = MutableStateFlow<List<NostrEvent>>(emptyList())

    fun publicFeedFlow(): Flow<List<NostrEvent>> =
        _allEvents.map { events ->
            val referenced = _referencedIds.value
            events.values
                .filter { it.kind == 1 && it.id in referenced }
                .sortedByDescending { it.createdAt }
        }

    fun privateFeedFlow(): Flow<List<NostrEvent>> =
        _allEvents.map { events ->
            val followed = getFollowedPubkeys()
            events.values
                .filter { it.kind == 1 && it.pubkey in followed }
                .sortedByDescending { it.createdAt }
        }

    fun blueGlowFlow(myPubkey: String): Flow<Boolean> =
        _glowEvents.map { it.isNotEmpty() }

    fun recentInteractionsFlow(): Flow<List<NostrEvent>> =
        _glowEvents.map { it.takeLast(20).reversed() }

    fun clearGlow() {
        _glowEvents.value = emptyList()
    }

    fun getFollowedPubkeys(): Set<String> {
        val raw = prefs.getString(KEY_FOLLOWS, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    fun followUser(pubkey: String, hexPrivKey: String) {
        val updated = getFollowedPubkeys() + pubkey
        saveFollows(updated)
        publishContactList(updated, hexPrivKey)
    }

    fun unfollowUser(pubkey: String, hexPrivKey: String) {
        val updated = getFollowedPubkeys() - pubkey
        saveFollows(updated)
        publishContactList(updated, hexPrivKey)
    }

    private fun saveFollows(pubkeys: Set<String>) {
        prefs.edit().putString(KEY_FOLLOWS, pubkeys.joinToString(",")).apply()
    }

    private fun publishContactList(pubkeys: Set<String>, hexPrivKey: String) {
        scope.launch {
            val event = EventSigner.createContactList(pubkeys.toList(), hexPrivKey)
            client.publish(event)
        }
    }

    fun connect(myPubkey: String? = null) {
        client.connect(DEFAULT_RELAY)

        // Subscribe to all Kind 1 events (limit recent)
        client.subscribe(
            subscriptionId = SUB_PUBLIC,
            filters = listOf(NostrFilter(kinds = listOf(1), limit = 200)),
        )

        // Subscribe to Kind 1 replies (events with #e tags) to track referenced IDs
        client.subscribe(
            subscriptionId = SUB_PRIVATE,
            filters = listOf(NostrFilter(kinds = listOf(1), limit = 100)),
        )

        // If we have a pubkey, subscribe to events that tag us
        if (myPubkey != null) {
            client.subscribe(
                subscriptionId = SUB_GLOW,
                filters = listOf(
                    NostrFilter(kinds = listOf(0, 1), eTags = listOf(myPubkey), limit = 50),
                ),
            )
        }

        scope.launch {
            client.eventFlow.collect { event ->
                when (event.kind) {
                    1 -> {
                        // Store the event
                        _allEvents.update { it + (event.id to event) }

                        // Track which events this one replies to via "e" tags
                        event.tags
                            .filter { tag -> tag.isNotEmpty() && tag[0] == "e" }
                            .mapNotNull { tag -> tag.getOrNull(1) }
                            .forEach { referencedId ->
                                _referencedIds.update { it + referencedId }
                            }
                    }
                    0 -> {
                        // Kind 0 targeting us goes to glow
                        val targetsUs = event.tags.any { tag ->
                            tag.isNotEmpty() && tag[0] == "p" && myPubkey != null && tag.getOrNull(1) == myPubkey
                        }
                        if (targetsUs) {
                            _glowEvents.update { it + event }
                        }
                    }
                }

                // Check if this event tags our pubkey (for glow)
                if (myPubkey != null) {
                    val targetsUs = event.tags.any { tag ->
                        tag.isNotEmpty() && tag[0] == "p" && tag.getOrNull(1) == myPubkey
                    }
                    if (targetsUs && event.kind == 1) {
                        _glowEvents.update { it + event }
                    }
                }
            }
        }
    }
}
