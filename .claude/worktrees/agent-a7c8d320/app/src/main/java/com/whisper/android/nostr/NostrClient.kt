package com.whisper.android.nostr

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

    private var webSocket: WebSocket? = null
    private var relayUrl: String = ""
    private var reconnectAttempt = 0
    private var isConnected = false

    fun connect(relayUrl: String) {
        this.relayUrl = relayUrl
        reconnectAttempt = 0
        openWebSocket()
    }

    private fun openWebSocket() {
        val request = Request.Builder().url(relayUrl).build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempt = 0
            }

            override fun onMessage(ws: WebSocket, text: String) {
                parseMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
            }
        })
    }

    private fun scheduleReconnect() {
        scope.launch {
            val backoffMs = minOf(1000L * (1 shl reconnectAttempt), 30_000L)
            reconnectAttempt++
            delay(backoffMs)
            if (!isConnected) openWebSocket()
        }
    }

    fun subscribe(subscriptionId: String, filters: List<NostrFilter>) {
        val msg = JSONArray().apply {
            put("REQ")
            put(subscriptionId)
            filters.forEach { put(it.toJson()) }
        }
        webSocket?.send(msg.toString())
    }

    fun publish(event: NostrEvent) {
        val msg = JSONArray().apply {
            put("EVENT")
            put(event.toJson())
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
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
