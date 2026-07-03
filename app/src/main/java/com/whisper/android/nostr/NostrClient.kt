package com.whisper.android.nostr

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NostrEvent(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String,
)

data class NostrFilter(
    val kinds: List<Int>? = null,
    val authors: List<String>? = null,
    val since: Long? = null,
    val limit: Int? = null,
    val eTags: List<String>? = null,
)

class NostrClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _eventFlow = MutableSharedFlow<NostrEvent>(extraBufferCapacity = 256)
    val eventFlow: SharedFlow<NostrEvent> = _eventFlow.asSharedFlow()

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private data class RelayState(
        var webSocket: WebSocket? = null,
        var reconnectAttempt: Int = 0,
        var isConnected: Boolean = false,
    )

    private val relays = mutableMapOf<String, RelayState>()
    private val pendingSubscriptions = mutableListOf<Pair<String, List<NostrFilter>>>()

    fun connect(relayUrls: List<String>) {
        relayUrls.forEach { url ->
            relays.getOrPut(url) { RelayState() }
            openWebSocket(url)
        }
    }

    private fun openWebSocket(relayUrl: String) {
        val state = relays[relayUrl] ?: return
        val request = Request.Builder().url(relayUrl).build()
        state.webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("Whisper", "Connected to $relayUrl")
                state.isConnected = true
                state.reconnectAttempt = 0
                // Replay pending subscriptions on (re)connect
                pendingSubscriptions.forEach { (id, filters) -> sendReq(ws, id, filters) }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                parseMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("Whisper", "Connection failed to $relayUrl: ${t.message}")
                state.isConnected = false
                scheduleReconnect(relayUrl)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                state.isConnected = false
            }
        })
    }

    private fun scheduleReconnect(relayUrl: String) {
        val state = relays[relayUrl] ?: return
        scope.launch {
            val backoffMs = minOf(1000L * (1 shl state.reconnectAttempt), 30_000L)
            state.reconnectAttempt++
            delay(backoffMs)
            if (!state.isConnected) openWebSocket(relayUrl)
        }
    }

    fun subscribe(subscriptionId: String, filters: List<NostrFilter>) {
        pendingSubscriptions.removeAll { it.first == subscriptionId }
        pendingSubscriptions.add(subscriptionId to filters)
        relays.values.forEach { state -> state.webSocket?.let { sendReq(it, subscriptionId, filters) } }
    }

    private fun sendReq(ws: WebSocket, subscriptionId: String, filters: List<NostrFilter>) {
        val msg = JSONArray().apply {
            put("REQ")
            put(subscriptionId)
            filters.forEach { put(it.toJson()) }
        }
        ws.send(msg.toString())
    }

    fun publish(event: NostrEvent) {
        val msg = JSONArray().apply {
            put("EVENT")
            put(event.toJson())
        }.toString()
        relays.values.forEach { it.webSocket?.send(msg) }
    }

    fun disconnect() {
        relays.values.forEach { it.webSocket?.close(1000, "Client disconnect") }
        relays.clear()
        pendingSubscriptions.clear()
    }

    private fun parseMessage(text: String) {
        try {
            val array = JSONArray(text)
            when (array.getString(0)) {
                "EVENT" -> {
                    val eventJson = array.getJSONObject(2)
                    scope.launch { _eventFlow.emit(eventJson.toNostrEvent()) }
                }
                // EOSE and NOTICE are intentionally ignored for now
            }
        } catch (_: Exception) {
        }
    }

    private fun NostrFilter.toJson(): JSONObject = JSONObject().apply {
        kinds?.let { put("kinds", JSONArray(it)) }
        authors?.let { put("authors", JSONArray(it)) }
        since?.let { put("since", it) }
        limit?.let { put("limit", it) }
        eTags?.let { put("#e", JSONArray(it)) }
    }

    private fun JSONObject.toNostrEvent(): NostrEvent {
        val tagsArray = getJSONArray("tags")
        val tags = (0 until tagsArray.length()).map { i ->
            val tag = tagsArray.getJSONArray(i)
            (0 until tag.length()).map { j -> tag.getString(j) }
        }
        return NostrEvent(
            id = getString("id"),
            pubkey = getString("pubkey"),
            createdAt = getLong("created_at"),
            kind = getInt("kind"),
            tags = tags,
            content = getString("content"),
            sig = getString("sig"),
        )
    }

    private fun NostrEvent.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("pubkey", pubkey)
        put("created_at", createdAt)
        put("kind", kind)
        put("tags", JSONArray(tags.map { tag -> JSONArray(tag) }))
        put("content", content)
        put("sig", sig)
    }
}
