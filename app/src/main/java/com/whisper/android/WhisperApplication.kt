package com.whisper.android

import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whisper.android.data.NostrRepository
import com.whisper.android.nostr.NostrClient

class WhisperApplication : Application() {

    val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            this,
            "whisper_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val nostrRepository: NostrRepository by lazy {
        NostrRepository(NostrClient(), securePrefs)
    }
}
