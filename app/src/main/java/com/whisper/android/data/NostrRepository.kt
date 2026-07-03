package com.whisper.android.data

import android.content.SharedPreferences
import android.util.Log
import com.whisper.android.nostr.EventSigner
import com.whisper.android.nostr.NostrClient
import com.whisper.android.nostr.NostrEvent
import com.whisper.android.nostr.NostrFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class NostrRepository(
    private val client: NostrClient,
    private val prefs: SharedPreferences,
) {

    companion object {
        val RELAYS = listOf(
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://relay.snort.social",
        )
        private const val KEY_FOLLOWS = "follows"
        private const val SUB_PUBLIC = "whisper-public"
        private const val SUB_PRIVATE = "whisper-private"
        private const val SUB_GLOW = "whisper-glow"
        private const val SUB_PROFILES = "whisper-profiles"
        private const val SUB_CONTACTS = "whisper-contacts"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connected = false

    private val _followedPubkeys = MutableStateFlow<Set<String>>(
        prefs.getString(KEY_FOLLOWS, "")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    )

    // All Kind 1 events received from the relay
    private val _allEvents = MutableStateFlow<Map<String, NostrEvent>>(emptyMap())

    // Map of event ID → reply count
    private val _replyCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    // Map of pubkey → user profile (from Kind 0 events)
    private val _profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())

    // Events that qualify for blue glow (targeting our pubkey)
    private val _glowEvents = MutableStateFlow<List<NostrEvent>>(emptyList())

    fun replyCountsFlow(): Flow<Map<String, Int>> = _replyCounts

    fun profilesFlow(): Flow<Map<String, UserProfile>> = _profiles

    fun publicFeedFlow(): Flow<List<NostrEvent>> =
        combine(_allEvents, _replyCounts, _followedPubkeys) { events, counts, followed ->
            events.values
                .filter { event ->
                    event.kind == 1
                    && event.pubkey !in followed
                    && event.tags.none { it.firstOrNull() == "e" }
                    && (counts.isEmpty() || event.id in counts)
                }
                .sortedByDescending { it.createdAt }
        }

    fun privateFeedFlow(): Flow<List<NostrEvent>> =
        combine(_allEvents, _followedPubkeys) { events, followed ->
            events.values
                .filter { event ->
                    event.kind == 1
                    && event.pubkey in followed
                    && event.tags.none { it.firstOrNull() == "e" }
                }
                .sortedByDescending { it.createdAt }
        }

    fun blueGlowFlow(myPubkey: String): Flow<Boolean> =
        _glowEvents.map { it.isNotEmpty() }

    fun recentInteractionsFlow(): Flow<List<NostrEvent>> =
        _glowEvents.map { it.takeLast(20).reversed() }

    fun clearGlow() {
        _glowEvents.value = emptyList()
    }

    fun followedPubkeysFlow(): Flow<Set<String>> = _followedPubkeys

    fun getFollowedPubkeys(): Set<String> = _followedPubkeys.value

    fun publishTextNote(content: String, hexPrivKey: String) {
        scope.launch {
            val event = EventSigner.createTextNote(content, hexPrivKey)
            client.publish(event)
            _allEvents.update { it + (event.id to event) }
        }
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
        _followedPubkeys.value = pubkeys
        prefs.edit().putString(KEY_FOLLOWS, pubkeys.joinToString(",")).apply()
    }

    private fun publishContactList(pubkeys: Set<String>, hexPrivKey: String) {
        scope.launch {
            val event = EventSigner.createContactList(pubkeys.toList(), hexPrivKey)
            client.publish(event)
        }
    }

    fun connect(myPubkey: String? = null) {
        if (connected) return
        connected = true
        Log.d("Whisper", "connect() called, pubkey=${myPubkey?.take(8)}")
        client.connect(RELAYS)

        // Subscribe to all Kind 1 events (limit recent)
        client.subscribe(
            subscriptionId = SUB_PUBLIC,
            filters = listOf(NostrFilter(kinds = listOf(1), limit = 1000)),
        )

        // Subscribe to Kind 1 events from followed authors (re-subscribes on follow/unfollow)
        scope.launch {
            _followedPubkeys
                .collect { followed ->
                    if (followed.isNotEmpty()) {
                        client.subscribe(
                            subscriptionId = SUB_PRIVATE,
                            filters = listOf(NostrFilter(kinds = listOf(1), authors = followed.toList(), limit = 500)),
                        )
                    }
                }
        }

        // Fetch our own contact list (Kind 3) from relays to sync follows
        if (myPubkey != null) {
            client.subscribe(
                subscriptionId = SUB_CONTACTS,
                filters = listOf(NostrFilter(kinds = listOf(3), authors = listOf(myPubkey), limit = 1)),
            )
        }

        // If we have a pubkey, subscribe to events that tag us
        if (myPubkey != null) {
            client.subscribe(
                subscriptionId = SUB_GLOW,
                filters = listOf(
                    NostrFilter(kinds = listOf(0, 1), eTags = listOf(myPubkey), limit = 50),
                ),
            )
        }

        // Debounced profile subscription: re-subscribe whenever new authors appear
        scope.launch {
            _allEvents
                .map { it.values.map { e -> e.pubkey }.toSet() }
                .distinctUntilChanged()
                .debounce(2000)
                .collect { pubkeys ->
                    if (pubkeys.isNotEmpty()) {
                        client.subscribe(
                            subscriptionId = SUB_PROFILES,
                            filters = listOf(NostrFilter(kinds = listOf(0), authors = pubkeys.toList())),
                        )
                    }
                }
        }

        scope.launch {
            client.eventFlow.collect { event ->
                Log.d("Whisper", "Event kind=${event.kind} id=${event.id.take(8)}")
                when (event.kind) {
                    1 -> {
                        // Store the event
                        _allEvents.update { it + (event.id to event) }

                        // Track reply counts via "e" tags
                        event.tags
                            .filter { tag -> tag.isNotEmpty() && tag[0] == "e" }
                            .mapNotNull { tag -> tag.getOrNull(1) }
                            .forEach { parentId ->
                                _replyCounts.update { counts ->
                                    counts + (parentId to (counts.getOrDefault(parentId, 0) + 1))
                                }
                            }
                    }
                    3 -> {
                        // Contact list from our own pubkey — sync follows
                        if (event.pubkey == myPubkey) {
                            val relayFollows = event.tags
                                .filter { tag -> tag.isNotEmpty() && tag[0] == "p" }
                                .mapNotNull { tag -> tag.getOrNull(1) }
                                .toSet()
                            if (relayFollows != _followedPubkeys.value) {
                                saveFollows(relayFollows)
                            }
                        }
                    }
                    0 -> {
                        // Parse and store user profile
                        try {
                            val json = JSONObject(event.content)
                            val profile = UserProfile(
                                name = json.optString("name").takeIf { it.isNotBlank() },
                                pictureUrl = json.optString("picture").takeIf { it.isNotBlank() },
                            )
                            _profiles.update { it + (event.pubkey to profile) }
                        } catch (_: Exception) {}

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
