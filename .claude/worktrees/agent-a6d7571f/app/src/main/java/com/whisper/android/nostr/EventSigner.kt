package com.whisper.android.nostr

import com.vitorpamplona.quartz.nip01Core.crypto.Nip01Crypto
import com.vitorpamplona.quartz.utils.Hex
import org.json.JSONArray
import java.security.MessageDigest

object EventSigner {

    fun createTextNote(content: String, hexPrivKey: String): NostrEvent =
        buildAndSign(kind = 1, tags = emptyList(), content = content, hexPrivKey = hexPrivKey)

    fun createContactList(followedPubkeys: List<String>, hexPrivKey: String): NostrEvent {
        val tags = followedPubkeys.map { pubkey -> listOf("p", pubkey) }
        return buildAndSign(kind = 3, tags = tags, content = "", hexPrivKey = hexPrivKey)
    }

    private fun buildAndSign(
        kind: Int,
        tags: List<List<String>>,
        content: String,
        hexPrivKey: String,
    ): NostrEvent {
        val privKeyBytes = Hex.decode(hexPrivKey)
        val pubKeyBytes = Nip01Crypto.pubKeyCreate(privKeyBytes)
        val hexPubKey = Hex.encode(pubKeyBytes)
        val createdAt = System.currentTimeMillis() / 1000L

        val eventId = computeEventId(hexPubKey, createdAt, kind, tags, content)
        val sigBytes = Nip01Crypto.sign(Hex.decode(eventId), privKeyBytes)

        return NostrEvent(
            id = eventId,
            pubkey = hexPubKey,
            createdAt = createdAt,
            kind = kind,
            tags = tags,
            content = content,
            sig = Hex.encode(sigBytes),
        )
    }

    private fun computeEventId(
        pubkey: String,
        createdAt: Long,
        kind: Int,
        tags: List<List<String>>,
        content: String,
    ): String {
        val tagsJson = JSONArray(tags.map { tag -> JSONArray(tag) })
        val serialized = JSONArray().apply {
            put(0)
            put(pubkey)
            put(createdAt)
            put(kind)
            put(tagsJson)
            put(content)
        }.toString()

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(serialized.toByteArray(Charsets.UTF_8))
        return Hex.encode(hashBytes)
    }
}
