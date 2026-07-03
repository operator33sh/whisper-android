package com.whisper.android.util

import com.vitorpamplona.quartz.nip01Core.crypto.KeyPair
import com.vitorpamplona.quartz.nip01Core.crypto.Nip01Crypto
import com.vitorpamplona.quartz.nip19Bech32.decodePrivateKeyAsHexOrNull
import com.vitorpamplona.quartz.nip19Bech32.toNpub
import com.vitorpamplona.quartz.nip19Bech32.toNsec
import com.vitorpamplona.quartz.utils.Hex

data class KeypairResult(
    val nsec: String,
    val npub: String,
    val hexPrivKey: String,
    val hexPubKey: String,
)

object KeyUtils {

    fun generateKeypair(): KeypairResult {
        val keyPair = KeyPair()
        val privKeyBytes = keyPair.privKey!!
        val pubKeyBytes = keyPair.pubKey
        return KeypairResult(
            nsec = privKeyBytes.toNsec(),
            npub = pubKeyBytes.toNpub(),
            hexPrivKey = Hex.encode(privKeyBytes),
            hexPubKey = Hex.encode(pubKeyBytes),
        )
    }

    fun nsecToHex(nsec: String): String? =
        decodePrivateKeyAsHexOrNull(nsec)

    fun hexToNpub(hexPubKey: String): String =
        Hex.decode(hexPubKey).toNpub()

    fun deriveHexPubKey(hexPrivKey: String): String {
        val privKeyBytes = Hex.decode(hexPrivKey)
        val pubKeyBytes = Nip01Crypto.pubKeyCreate(privKeyBytes)
        return Hex.encode(pubKeyBytes)
    }
}
